package piggy.basic

import dev.alteration.branch.piggy.*
import dev.alteration.branch.piggy.Sql.*
import dev.alteration.branch.piggy.PreparedStatementSetter.given
import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, DriverManager}
import scala.language.implicitConversions

/** A basic example showing CRUD operations with Piggy.
  *
  * Piggy is a type-safe SQL library that provides:
  *   - String interpolators for queries (psUpdate, psQuery)
  *   - Automatic type class-based parsing of result sets
  *   - Transaction management with automatic rollback on failure
  *   - Monadic composition using for-comprehensions
  *
  * This example assumes you have PostgreSQL running locally:
  *   - Host: localhost
  *   - Port: 5432
  *   - Database: postgres
  *   - User: postgres
  *   - Password: postgres
  *
  * To run this example: sbt "examples/runMain piggy.basic.BasicCrudExample"
  */
object BasicCrudExample {

  // Simple connection pool for examples
  case class SimpleConnectionPool(url: String, username: String, password: String)
      extends ResourcePool[Connection] {
    override def acquire: Connection =
      DriverManager.getConnection(url, username, password)

    override def release(resource: Connection): Unit =
      if (resource != null && !resource.isClosed) resource.close()
  }

  // Define a case class that maps to our database table
  // The ResultSetParser is automatically derived
  case class User(id: Int, name: String, email: String, age: Int)
      derives ResultSetParser

  def main(args: Array[String]): Unit = {
    println("=== Piggy Basic CRUD Example ===\n")

    // Create a connection pool
    given pool: ResourcePool[Connection] = SimpleConnectionPool(
      url = "jdbc:postgresql://localhost:5432/postgres",
      username = "postgres",
      password = "postgres"
    )

    // Create table DDL
    val createTable = """
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        age INT NOT NULL
      )
    """

    // Example 1: Create table and insert a user
    println("--- Example 1: Create and Insert ---")
    val insertResult = {
      for {
        _       <- Sql.statement(createTable)
        _       <- Sql.statement("TRUNCATE TABLE users")
        rowsAff <- psUpdate"INSERT INTO users (name, email, age) VALUES (${"Alice"}, ${"alice@example.com"}, ${30})"
      } yield rowsAff
    }.executePool

    insertResult match {
      case scala.util.Success(count) =>
        println(s"Inserted $count row(s)")
      case scala.util.Failure(e)     =>
        println(s"Failed to insert: ${e.getMessage}")
        return
    }
    println()

    // Example 2: Query users
    println("--- Example 2: Query Users ---")
    val queryResult = {
      psNamed"SELECT * FROM users WHERE name = :name"
        .bindQuery[User]("name" -> "Alice")
    }.executePool

    queryResult match {
      case scala.util.Success(users) =>
        println(s"Found ${users.size} user(s):")
        users.foreach(u => println(s"  - ${u.name} (${u.email}), age ${u.age}"))
      case scala.util.Failure(e)     =>
        println(s"Failed to query: ${e.getMessage}")
    }
    println()

    // Example 3: Insert multiple users
    println("--- Example 3: Insert Multiple Users ---")
    val multiInsertResult = {
      for {
        n1 <- psUpdate"INSERT INTO users (name, email, age) VALUES (${"Bob"}, ${"bob@example.com"}, ${25})"
        n2 <- psUpdate"INSERT INTO users (name, email, age) VALUES (${"Charlie"}, ${"charlie@example.com"}, ${35})"
        n3 <- psUpdate"INSERT INTO users (name, email, age) VALUES (${"Diana"}, ${"diana@example.com"}, ${28})"
      } yield n1 + n2 + n3
    }.executePool

    multiInsertResult match {
      case scala.util.Success(total) =>
        println(s"Inserted $total more user(s)")
      case scala.util.Failure(e)     =>
        println(s"Failed to insert: ${e.getMessage}")
    }
    println()

    // Example 4: Query all users
    println("--- Example 4: Query All Users ---")
    val allUsersResult = {
      Sql.statement("SELECT * FROM users ORDER BY age", _.parsedList[User])
    }.executePool

    allUsersResult match {
      case scala.util.Success(users) =>
        println(s"All users (${users.size} total):")
        users.foreach(u => println(s"  - ${u.name}, age ${u.age}"))
      case scala.util.Failure(e)     =>
        println(s"Failed to query: ${e.getMessage}")
    }
    println()

    // Example 5: Update a user
    println("--- Example 5: Update User ---")
    val updateResult = {
      psUpdate"UPDATE users SET age = ${31} WHERE name = ${"Alice"}"
    }.executePool

    updateResult match {
      case scala.util.Success(count) =>
        println(s"Updated $count row(s)")
        // Verify the update
        val verify = psNamed"SELECT * FROM users WHERE name = :name"
          .bindQuery[User]("name" -> "Alice")
          .executePool
        verify.foreach { users =>
          users.headOption.foreach(u => println(s"  Alice's new age: ${u.age}"))
        }
      case scala.util.Failure(e)     =>
        println(s"Failed to update: ${e.getMessage}")
    }
    println()

    // Example 6: Delete a user
    println("--- Example 6: Delete User ---")
    val deleteResult = {
      psUpdate"DELETE FROM users WHERE name = ${"Bob"}"
    }.executePool

    deleteResult match {
      case scala.util.Success(count) =>
        println(s"Deleted $count row(s)")
      case scala.util.Failure(e)     =>
        println(s"Failed to delete: ${e.getMessage}")
    }
    println()

    // Example 7: Query with filtering
    println("--- Example 7: Query with Age Filter ---")
    val filteredQuery = {
      psNamed"SELECT * FROM users WHERE age >= :minAge ORDER BY age"
        .bindQuery[User]("minAge" -> 30)
    }.executePool

    filteredQuery match {
      case scala.util.Success(users) =>
        println(s"Users 30 or older (${users.size} total):")
        users.foreach(u => println(s"  - ${u.name}, age ${u.age}"))
      case scala.util.Failure(e)     =>
        println(s"Failed to query: ${e.getMessage}")
    }
    println()

    // Example 8: Query single column as tuple
    println("--- Example 8: Query Names Only ---")
    val namesQuery = {
      Sql.statement(
        "SELECT name FROM users ORDER BY name",
        _.parsedList[String]
      )
    }.executePool

    namesQuery match {
      case scala.util.Success(names) =>
        println(s"User names: ${names.mkString(", ")}")
      case scala.util.Failure(e)     =>
        println(s"Failed to query: ${e.getMessage}")
    }
    println()

    // Cleanup
    println("--- Cleanup ---")
    Sql.statement("DROP TABLE users").executePool match {
      case scala.util.Success(_) => println("Table dropped successfully")
      case scala.util.Failure(e) => println(s"Failed to drop table: ${e.getMessage}")
    }

    println("\n=== Example Complete ===")
  }
}
