package piggy.transaction

import dev.alteration.branch.piggy.*
import dev.alteration.branch.piggy.Sql.*
import dev.alteration.branch.piggy.PreparedStatementSetter.given
import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, DriverManager}
import scala.language.implicitConversions

/** An example showing transaction management and error handling with Piggy.
  *
  * Key features demonstrated:
  *   - Automatic transaction management
  *   - Automatic rollback on failure
  *   - Error recovery with .recover
  *   - Sql.fail for explicit failures
  *   - Composing multiple operations atomically
  *
  * When you call .executePool or .execute, Piggy automatically:
  *   1. Starts a transaction 2. Executes all operations in the
  *      for-comprehension 3. Commits if successful, or rolls back if any
  *      operation fails
  *
  * This example assumes you have PostgreSQL running locally:
  *   - Host: localhost
  *   - Port: 5432
  *   - Database: postgres
  *   - User: postgres
  *   - Password: postgres
  *
  * To run this example: sbt "examples/runMain
  * piggy.transaction.TransactionExample"
  */
object TransactionExample {

  // Simple connection pool for examples
  case class SimpleConnectionPool(
      url: String,
      username: String,
      password: String
  ) extends ResourcePool[Connection] {
    override def acquire: Connection =
      DriverManager.getConnection(url, username, password)

    override def release(resource: Connection): Unit =
      if (resource != null && !resource.isClosed) resource.close()
  }

  case class Account(id: Int, name: String, balance: BigDecimal)
      derives ResultSetParser

  def main(args: Array[String]): Unit = {
    println("=== Piggy Transaction Example ===\n")

    // Create a connection pool
    given pool: ResourcePool[Connection] = SimpleConnectionPool(
      url = "jdbc:postgresql://localhost:5432/postgres",
      username = "postgres",
      password = "postgres"
    )

    // Create table DDL
    val createTable = """
      CREATE TABLE IF NOT EXISTS accounts (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        balance NUMERIC(10, 2) NOT NULL CHECK (balance >= 0)
      )
    """

    // Setup: Create table and insert test accounts
    println("--- Setup: Creating accounts ---")
    val setupResult = {
      for {
        _  <- Sql.statement(createTable)
        _  <- Sql.statement("TRUNCATE TABLE accounts")
        n1 <-
          psUpdate"INSERT INTO accounts (name, balance) VALUES (${"Alice"}, ${BigDecimal(1000.00)})"
        n2 <-
          psUpdate"INSERT INTO accounts (name, balance) VALUES (${"Bob"}, ${BigDecimal(500.00)})"
      } yield n1 + n2
    }.executePool

    setupResult match {
      case scala.util.Success(count) =>
        println(s"Created $count account(s)")
      case scala.util.Failure(e)     =>
        println(s"Setup failed: ${e.getMessage}")
        return
    }

    // Show initial balances
    showBalances("Initial balances")

    // Example 1: Successful transaction - money transfer
    println("--- Example 1: Successful Transaction ---")
    println("Transferring $200 from Alice to Bob")

    val successfulTransfer = {
      for {
        _ <-
          psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(200.00)} WHERE name = ${"Alice"}"
        _ <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(200.00)} WHERE name = ${"Bob"}"
      } yield ()
    }.executePool

    successfulTransfer match {
      case scala.util.Success(_) =>
        println("Transaction completed successfully")
        showBalances("Balances after successful transfer")
      case scala.util.Failure(e) =>
        println(s"Transaction failed: ${e.getMessage}")
    }

    // Example 2: Failed transaction with automatic rollback
    println("--- Example 2: Failed Transaction (Automatic Rollback) ---")
    println(
      "Attempting to transfer $1000 from Bob to Alice (will fail due to insufficient funds)"
    )

    val failedTransfer = {
      for {
        _ <-
          psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(1000.00)} WHERE name = ${"Bob"}"
        _ <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(1000.00)} WHERE name = ${"Alice"}"
      } yield ()
    }.executePool

    failedTransfer match {
      case scala.util.Success(_) =>
        println("Transaction completed (unexpected)")
      case scala.util.Failure(e) =>
        println(s"Transaction failed as expected: ${e.getMessage}")
        println("Database automatically rolled back the transaction")
        showBalances("Balances after failed transaction (unchanged)")
    }

    // Example 3: Using Sql.fail to abort a transaction
    println("--- Example 3: Explicit Failure with Sql.fail ---")
    println("Attempting transfer with business logic validation")

    val transferAmount  = BigDecimal(300.00)
    val explicitFailure = {
      for {
        accounts <- psNamed"SELECT * FROM accounts WHERE name = :name"
                      .bindQuery[Account]("name" -> "Bob")
        _        <- if (accounts.headOption.exists(_.balance >= transferAmount)) {
                      Sql.statement("").map(_ => ()) // Continue
                    } else {
                      Sql.fail(
                        new Exception("Insufficient funds (business logic check)")
                      )
                    }
        _        <-
          psUpdate"UPDATE accounts SET balance = balance - $transferAmount WHERE name = ${"Bob"}"
        _        <-
          psUpdate"UPDATE accounts SET balance = balance + $transferAmount WHERE name = ${"Alice"}"
      } yield ()
    }.executePool

    explicitFailure match {
      case scala.util.Success(_) =>
        println("Transaction completed")
        showBalances("Balances after validated transfer")
      case scala.util.Failure(e) =>
        println(s"Transaction aborted: ${e.getMessage}")
        showBalances("Balances unchanged")
    }

    // Example 4: Error recovery with .recover
    println("--- Example 4: Error Recovery ---")
    println("Attempting transfer with fallback to smaller amount")

    val transferWithRecovery = {
      val largeTransfer = for {
        _ <-
          psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(600.00)} WHERE name = ${"Alice"}"
        _ <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(600.00)} WHERE name = ${"Bob"}"
      } yield "Transferred $600"

      largeTransfer.recover { _ =>
        // If large transfer fails, try smaller amount
        println("  Large transfer failed, trying smaller amount...")
        for {
          _ <-
            psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(100.00)} WHERE name = ${"Alice"}"
          _ <-
            psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(100.00)} WHERE name = ${"Bob"}"
        } yield "Transferred $100 (fallback)"
      }
    }.executePool

    transferWithRecovery match {
      case scala.util.Success(msg) =>
        println(s"Result: $msg")
        showBalances("Balances after recovery")
      case scala.util.Failure(e)   =>
        println(s"All attempts failed: ${e.getMessage}")
    }

    // Example 5: Complex transaction with multiple operations
    println("--- Example 5: Complex Multi-Step Transaction ---")
    println("Creating new account and transferring funds atomically")

    val complexTransaction = {
      for {
        n       <-
          psUpdate"INSERT INTO accounts (name, balance) VALUES (${"Charlie"}, ${BigDecimal(0.00)})"
        _       <-
          psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(50.00)} WHERE name = ${"Alice"}"
        _       <-
          psUpdate"UPDATE accounts SET balance = balance - ${BigDecimal(50.00)} WHERE name = ${"Bob"}"
        _       <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(100.00)} WHERE name = ${"Charlie"}"
        charlie <- psNamed"SELECT * FROM accounts WHERE name = :name"
                     .bindQuery[Account]("name" -> "Charlie")
      } yield (n, charlie)
    }.executePool

    complexTransaction match {
      case scala.util.Success((created, accounts)) =>
        println(s"Created $created account(s)")
        accounts.headOption.foreach(acc =>
          println(s"Charlie's balance: ${acc.balance}")
        )
        showBalances("All balances after complex transaction")
      case scala.util.Failure(e)                   =>
        println(s"Transaction failed: ${e.getMessage}")
        showBalances("Balances unchanged (rollback)")
    }

    // Example 6: Demonstrating isolation - what happens with invalid SQL
    println("--- Example 6: Invalid SQL Causes Rollback ---")
    val invalidSqlTransaction = {
      for {
        _ <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(1000.00)} WHERE name = ${"Alice"}"
        _ <- Sql.statement("THIS IS INVALID SQL")
        _ <-
          psUpdate"UPDATE accounts SET balance = balance + ${BigDecimal(1000.00)} WHERE name = ${"Bob"}"
      } yield ()
    }.executePool

    invalidSqlTransaction match {
      case scala.util.Success(_) =>
        println("Transaction completed (unexpected)")
      case scala.util.Failure(e) =>
        println(s"Transaction failed: ${e.getClass.getSimpleName}")
        println("All changes in the transaction were rolled back")
        showBalances("Balances unchanged (complete rollback)")
    }

    // Cleanup
    println("--- Cleanup ---")
    Sql.statement("DROP TABLE accounts").executePool match {
      case scala.util.Success(_) => println("Table dropped successfully")
      case scala.util.Failure(e) =>
        println(s"Failed to drop table: ${e.getMessage}")
    }

    println("\n=== Example Complete ===")
  }

  // Helper method to display current account balances
  private def showBalances(
      title: String
  )(using pool: ResourcePool[Connection]): Unit = {
    println(s"\n$title:")
    val result = {
      Sql.statement(
        "SELECT * FROM accounts ORDER BY name",
        _.parsedList[Account]
      )
    }.executePool

    result match {
      case scala.util.Success(accounts) =>
        accounts.foreach(acc =>
          println(f"  ${acc.name}%10s: $$${acc.balance}%7.2f")
        )
      case scala.util.Failure(e)        =>
        println(s"  Failed to query balances: ${e.getMessage}")
    }
    println()
  }
}
