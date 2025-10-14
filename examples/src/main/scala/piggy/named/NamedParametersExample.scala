package piggy.named

import dev.alteration.branch.piggy.*
import dev.alteration.branch.piggy.Sql.*
import dev.alteration.branch.piggy.PreparedStatementSetter.given
import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, DriverManager}
import scala.language.implicitConversions

/** An example showing named parameters with Piggy.
  *
  * Named parameters provide better readability for complex queries with many
  * parameters. Instead of positional ${} placeholders, you use :paramName syntax
  * and bind values by name.
  *
  * Benefits:
  *   - Self-documenting queries
  *   - Order-independent parameter binding
  *   - Easier to maintain complex queries
  *   - Same parameter can be used multiple times
  *
  * This example assumes you have PostgreSQL running locally:
  *   - Host: localhost
  *   - Port: 5432
  *   - Database: postgres
  *   - User: postgres
  *   - Password: postgres
  *
  * To run this example: sbt "examples/runMain piggy.named.NamedParametersExample"
  */
object NamedParametersExample {

  // Simple connection pool for examples
  case class SimpleConnectionPool(url: String, username: String, password: String)
      extends ResourcePool[Connection] {
    override def acquire: Connection =
      DriverManager.getConnection(url, username, password)

    override def release(resource: Connection): Unit =
      if (resource != null && !resource.isClosed) resource.close()
  }

  case class Product(id: Int, name: String, price: BigDecimal, category: String)
      derives ResultSetParser

  def main(args: Array[String]): Unit = {
    println("=== Piggy Named Parameters Example ===\n")

    // Create a connection pool
    given pool: ResourcePool[Connection] = SimpleConnectionPool(
      url = "jdbc:postgresql://localhost:5432/postgres",
      username = "postgres",
      password = "postgres"
    )

    // Create table DDL
    val createTable = """
      CREATE TABLE IF NOT EXISTS products (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        price NUMERIC(10, 2) NOT NULL,
        category TEXT NOT NULL
      )
    """

    // Setup: Create table and insert test data
    println("--- Setup: Creating table and inserting test data ---")
    val setupResult = {
      for {
        _ <- Sql.statement(createTable)
        _ <- Sql.statement("TRUNCATE TABLE products")
        n <- psNamed"INSERT INTO products (name, price, category) VALUES (:name, :price, :category)"
               .bindUpdate(
                 "name"     -> "Laptop",
                 "price"    -> BigDecimal(999.99),
                 "category" -> "Electronics"
               )
      } yield n
    }.executePool

    setupResult match {
      case scala.util.Success(count) => println(s"Inserted $count product(s)")
      case scala.util.Failure(e)     =>
        println(s"Setup failed: ${e.getMessage}")
        return
    }
    println()

    // Example 1: Named parameters for INSERT
    println("--- Example 1: Named Parameters INSERT ---")
    val insertResult = {
      psNamed"INSERT INTO products (name, price, category) VALUES (:productName, :productPrice, :productCategory)"
        .bindUpdate(
          "productName"     -> "Wireless Mouse",
          "productPrice"    -> BigDecimal(29.99),
          "productCategory" -> "Electronics"
        )
    }.executePool

    insertResult.foreach(count => println(s"Inserted $count product(s)"))
    println()

    // Example 2: Order independence - parameters can be in any order
    println("--- Example 2: Order-Independent Parameters ---")
    val orderIndependentResult = {
      psNamed"INSERT INTO products (name, price, category) VALUES (:name, :price, :category)"
        .bindUpdate(
          "category" -> "Books",        // Different order than SQL
          "name"     -> "Scala Book",
          "price"    -> BigDecimal(49.99)
        )
    }.executePool

    orderIndependentResult.foreach(count =>
      println(s"Inserted $count product(s) with order-independent parameters")
    )
    println()

    // Example 3: Named parameters for SELECT
    println("--- Example 3: Named Parameters SELECT ---")
    val selectResult = {
      psNamed"SELECT * FROM products WHERE category = :categoryName AND price < :maxPrice ORDER BY price"
        .bindQuery[Product](
          "categoryName" -> "Electronics",
          "maxPrice"     -> BigDecimal(500.00)
        )
    }.executePool

    selectResult match {
      case scala.util.Success(products) =>
        println(s"Found ${products.size} product(s) in Electronics under $$500:")
        products.foreach(p => println(s"  - ${p.name}: ${p.price}"))
      case scala.util.Failure(e)        =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Example 4: Complex query with multiple conditions
    println("--- Example 4: Complex Query with Multiple Parameters ---")
    val complexQuery = {
      for {
        _ <- psNamed"INSERT INTO products (name, price, category) VALUES (:name, :price, :category)"
               .bindUpdate(
                 "name"     -> "Desk Chair",
                 "price"    -> BigDecimal(199.99),
                 "category" -> "Furniture"
               )
        _ <- psNamed"INSERT INTO products (name, price, category) VALUES (:name, :price, :category)"
               .bindUpdate(
                 "name"     -> "Standing Desk",
                 "price"    -> BigDecimal(599.99),
                 "category" -> "Furniture"
               )
        products <- psNamed"""
          SELECT * FROM products
          WHERE category = :cat
            AND price BETWEEN :minPrice AND :maxPrice
          ORDER BY price DESC
        """
                      .bindQuery[Product](
                        "cat"      -> "Furniture",
                        "minPrice" -> BigDecimal(100.00),
                        "maxPrice" -> BigDecimal(600.00)
                      )
      } yield products
    }.executePool

    complexQuery match {
      case scala.util.Success(products) =>
        println(s"Furniture between $$100-$$600 (${products.size} found):")
        products.foreach(p => println(s"  - ${p.name}: ${p.price}"))
      case scala.util.Failure(e)        =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Example 5: Reusing same parameter multiple times
    println("--- Example 5: Reusing Parameters ---")
    val reuseResult = {
      for {
        _ <- psUpdate"INSERT INTO products (name, price, category) VALUES (${"Power Bank"}, ${BigDecimal(39.99)}, ${"Electronics"})"
        products <- psNamed"""
          SELECT * FROM products
          WHERE category = :category
            AND name ILIKE '%' || :searchTerm || '%'
            AND price > (SELECT AVG(price) FROM products WHERE category = :category)
          ORDER BY price
        """
                      .bindQuery[Product](
                        "category"   -> "Electronics",
                        "searchTerm" -> "o" // Matches "Mouse", "Power Bank", etc.
                      )
      } yield products
    }.executePool

    reuseResult match {
      case scala.util.Success(products) =>
        println(s"Electronics with 'o' in name, above avg price (${products.size} found):")
        products.foreach(p => println(s"  - ${p.name}: ${p.price}"))
      case scala.util.Failure(e)        =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Example 6: Update with named parameters
    println("--- Example 6: UPDATE with Named Parameters ---")
    val updateResult = {
      psNamed"UPDATE products SET price = :newPrice WHERE category = :cat AND price < :threshold"
        .bindUpdate(
          "newPrice"  -> BigDecimal(999.99),
          "cat"       -> "Electronics",
          "threshold" -> BigDecimal(50.00)
        )
    }.executePool

    updateResult match {
      case scala.util.Success(count) =>
        println(s"Updated $count product(s) to new price")
      case scala.util.Failure(e)     =>
        println(s"Update failed: ${e.getMessage}")
    }
    println()

    // Example 7: Delete with named parameters
    println("--- Example 7: DELETE with Named Parameters ---")
    val deleteResult = {
      psNamed"DELETE FROM products WHERE category = :categoryName AND price > :maxPrice"
        .bindUpdate(
          "categoryName" -> "Furniture",
          "maxPrice"     -> BigDecimal(500.00)
        )
    }.executePool

    deleteResult match {
      case scala.util.Success(count) =>
        println(s"Deleted $count product(s)")
      case scala.util.Failure(e)     =>
        println(s"Delete failed: ${e.getMessage}")
    }
    println()

    // Example 8: Comparing psUpdate with named parameters
    println("--- Example 8: psUpdate vs Named Parameters ---")

    // Using psUpdate with direct interpolation
    val updateStyleResult = {
      for {
        _ <- psUpdate"INSERT INTO products (name, price, category) VALUES (${"Test Product"}, ${BigDecimal(75.00)}, ${"Electronics"})"
        products <- psNamed"SELECT * FROM products WHERE category = :category AND price < :maxPrice"
                      .bindQuery[Product](
                        "category" -> "Electronics",
                        "maxPrice" -> BigDecimal(100.00)
                      )
      } yield products
    }.executePool

    // Using named parameters throughout
    val namedStyleResult = {
      psNamed"SELECT * FROM products WHERE category = :category AND price < :maxPrice ORDER BY price"
        .bindQuery[Product](
          "category" -> "Electronics",
          "maxPrice" -> BigDecimal(100.00)
        )
    }.executePool

    println("Both psUpdate and named parameters work well")
    println(s"Update style found: ${updateStyleResult.map(_.size).getOrElse(0)} products")
    println(s"Named style found: ${namedStyleResult.map(_.size).getOrElse(0)} products")
    println()

    // Cleanup
    println("--- Cleanup ---")
    Sql.statement("DROP TABLE products").executePool match {
      case scala.util.Success(_) => println("Table dropped successfully")
      case scala.util.Failure(e) => println(s"Failed to drop table: ${e.getMessage}")
    }

    println("\n=== Example Complete ===")
  }
}
