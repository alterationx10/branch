package piggy.batch

import dev.alteration.branch.piggy.*
import dev.alteration.branch.piggy.Sql.*
import dev.alteration.branch.piggy.PreparedStatementSetter.given
import dev.alteration.branch.macaroni.poolers.ResourcePool
import java.sql.{Connection, DriverManager}
import java.time.LocalDate
import scala.language.implicitConversions

/** An example showing batch operations with Piggy.
  *
  * Batch operations allow you to efficiently execute the same SQL statement
  * multiple times with different parameters. This is much more efficient than
  * executing statements one by one.
  *
  * Key features demonstrated:
  *   - Sql.prepareUpdate for batch inserts/updates
  *   - Sql.prepareQuery for batch queries
  *   - Functions that generate prepared statements
  *   - Processing large datasets efficiently
  *   - Working with java.time types
  *
  * This example assumes you have PostgreSQL running locally:
  *   - Host: localhost
  *   - Port: 5432
  *   - Database: postgres
  *   - User: postgres
  *   - Password: postgres
  *
  * To run this example: sbt "examples/runMain piggy.batch.BatchOperationsExample"
  */
object BatchOperationsExample {

  // Simple connection pool for examples
  case class SimpleConnectionPool(url: String, username: String, password: String)
      extends ResourcePool[Connection] {
    override def acquire: Connection =
      DriverManager.getConnection(url, username, password)

    override def release(resource: Connection): Unit =
      if (resource != null && !resource.isClosed) resource.close()
  }

  case class Employee(
      id: Int,
      name: String,
      department: String,
      salary: BigDecimal,
      hireDate: LocalDate
  ) derives ResultSetParser

  case class SalaryUpdate(employeeId: Int, newSalary: BigDecimal)

  def main(args: Array[String]): Unit = {
    println("=== Piggy Batch Operations Example ===\n")

    // Create a connection pool
    given pool: ResourcePool[Connection] = SimpleConnectionPool(
      url = "jdbc:postgresql://localhost:5432/postgres",
      username = "postgres",
      password = "postgres"
    )

    // Create table DDL
    val createTable = """
      CREATE TABLE IF NOT EXISTS employees (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        department TEXT NOT NULL,
        salary NUMERIC(10, 2) NOT NULL,
        hire_date DATE NOT NULL
      )
    """

    // Setup
    println("--- Setup: Creating table ---")
    val setupResult = {
      for {
        _ <- Sql.statement(createTable)
        _ <- Sql.statement("TRUNCATE TABLE employees")
      } yield ()
    }.executePool

    setupResult match {
      case scala.util.Success(_) => println("Table ready")
      case scala.util.Failure(e) =>
        println(s"Setup failed: ${e.getMessage}")
        return
    }
    println()

    // Example 1: Batch insert with prepareUpdate
    println("--- Example 1: Batch Insert ---")

    // Define a function that creates a prepared statement for an employee
    val insertEmployee: Employee => PsArgHolder = (emp: Employee) =>
      ps"INSERT INTO employees (name, department, salary, hire_date) VALUES (${emp.name}, ${emp.department}, ${emp.salary}, ${emp.hireDate})"

    // Create test data
    val newEmployees = Seq(
      Employee(0, "Alice Johnson", "Engineering", BigDecimal(95000), LocalDate.of(2023, 1, 15)),
      Employee(0, "Bob Smith", "Engineering", BigDecimal(87000), LocalDate.of(2023, 2, 1)),
      Employee(0, "Carol White", "Marketing", BigDecimal(72000), LocalDate.of(2023, 3, 10)),
      Employee(0, "David Brown", "Sales", BigDecimal(68000), LocalDate.of(2023, 3, 15)),
      Employee(0, "Eve Davis", "Engineering", BigDecimal(102000), LocalDate.of(2023, 4, 1)),
      Employee(0, "Frank Miller", "Marketing", BigDecimal(75000), LocalDate.of(2023, 5, 1)),
      Employee(0, "Grace Lee", "Sales", BigDecimal(71000), LocalDate.of(2023, 6, 1)),
      Employee(0, "Henry Wilson", "Engineering", BigDecimal(98000), LocalDate.of(2023, 7, 1))
    )

    // Execute batch insert
    val batchInsertResult = {
      Sql.prepareUpdate(insertEmployee, newEmployees*)
    }.executePool

    batchInsertResult match {
      case scala.util.Success(count) =>
        println(s"Batch inserted $count employee(s)")
      case scala.util.Failure(e)     =>
        println(s"Batch insert failed: ${e.getMessage}")
        return
    }
    println()

    // Example 2: Query all employees
    println("--- Example 2: Query All Employees ---")
    val allEmployees = {
      Sql.statement("SELECT * FROM employees ORDER BY hire_date", _.parsedList[Employee])
    }.executePool

    allEmployees match {
      case scala.util.Success(employees) =>
        println(s"Found ${employees.size} employee(s):")
        employees.foreach { emp =>
          println(f"  ${emp.name}%-20s ${emp.department}%-15s $$${emp.salary}%8.2f  ${emp.hireDate}")
        }
      case scala.util.Failure(e)         =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Example 3: Batch queries by department
    println("--- Example 3: Batch Queries by Department ---")

    // Define a function that creates a query for a department
    val findByDepartment: String => PsArgHolder = (dept: String) =>
      ps"SELECT id, name, department, salary, hire_date FROM employees WHERE department = $dept"

    val departments = Seq("Engineering", "Marketing", "Sales")

    val batchQueryResult = {
      Sql.prepareQuery[String, Employee](findByDepartment, departments*)
    }.executePool

    batchQueryResult match {
      case scala.util.Success(employees) =>
        println(s"Batch query returned ${employees.size} total employee(s):")
        departments.foreach { dept =>
          val deptEmps = employees.filter(_.department == dept)
          println(s"\n  $dept (${deptEmps.size}):")
          deptEmps.foreach(emp => println(f"    ${emp.name}%-20s $$${emp.salary}%8.2f"))
        }
      case scala.util.Failure(e)         =>
        println(s"Batch query failed: ${e.getMessage}")
    }
    println()

    // Example 4: Batch update salaries
    println("--- Example 4: Batch Update Salaries ---")

    // Give everyone in Engineering a 5% raise
    val engineeringEmployees = allEmployees.get.filter(_.department == "Engineering")
    val salaryUpdates = engineeringEmployees.map { emp =>
      (emp.id, emp.salary * 1.05)
    }

    val updateSalary: ((Int, BigDecimal)) => PsArgHolder = { case (id, newSalary) =>
      ps"UPDATE employees SET salary = $newSalary WHERE id = $id"
    }

    val batchUpdateResult = {
      Sql.prepareUpdate(updateSalary, salaryUpdates*)
    }.executePool

    batchUpdateResult match {
      case scala.util.Success(count) =>
        println(s"Batch updated $count employee salaries (5% raise for Engineering)")
      case scala.util.Failure(e)     =>
        println(s"Batch update failed: ${e.getMessage}")
    }

    // Show updated salaries
    val updatedEngineering = {
      psNamed"SELECT * FROM employees WHERE department = :dept ORDER BY name"
        .bindQuery[Employee]("dept" -> "Engineering")
    }.executePool

    updatedEngineering match {
      case scala.util.Success(employees) =>
        println("\nUpdated Engineering salaries:")
        employees.foreach(emp => println(f"  ${emp.name}%-20s $$${emp.salary}%8.2f"))
      case scala.util.Failure(e)         =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Example 5: Large batch insert
    println("--- Example 5: Large Batch Insert ---")

    val manyEmployees = (1 to 1000).map { i =>
      Employee(
        0,
        s"Employee-$i",
        if (i % 3 == 0) "Engineering" else if (i % 3 == 1) "Marketing" else "Sales",
        BigDecimal(50000 + (i % 50) * 1000),
        LocalDate.of(2024, 1 + (i % 12), 1 + (i % 28))
      )
    }

    val largeBatchResult = {
      Sql.prepareUpdate(insertEmployee, manyEmployees*)
    }.executePool

    largeBatchResult match {
      case scala.util.Success(count) =>
        println(s"Large batch inserted $count employee(s)")
      case scala.util.Failure(e)     =>
        println(s"Large batch insert failed: ${e.getMessage}")
    }

    // Count total employees
    val countResult = {
      Sql.statement("SELECT COUNT(*) FROM employees", rs => {
        if (rs.next()) rs.getInt(1) else 0
      })
    }.executePool

    countResult.foreach(count => println(s"Total employees in database: $count"))
    println()

    // Example 6: Batch operations in a transaction
    println("--- Example 6: Batch Operations in Transaction ---")

    val transactionalBatch = {
      for {
        // Add new employees
        n1       <- Sql.prepareUpdate(
                      insertEmployee,
                      Employee(0, "Transaction User 1", "Engineering", BigDecimal(90000), LocalDate.now()),
                      Employee(0, "Transaction User 2", "Marketing", BigDecimal(85000), LocalDate.now())
                    )
        // Query them back
        newUsers <- Sql.prepareQuery[String, Employee](
                      findByDepartment,
                      "Engineering",
                      "Marketing"
                    )
      } yield (n1, newUsers.size)
    }.executePool

    transactionalBatch match {
      case scala.util.Success((inserted, queried)) =>
        println(s"Transaction completed: inserted $inserted, queried $queried employees")
      case scala.util.Failure(e)                   =>
        println(s"Transaction failed: ${e.getMessage}")
    }
    println()

    // Example 7: Filtering batch results
    println("--- Example 7: Aggregating Batch Query Results ---")

    val aggregationResult = {
      Sql.prepareQuery[String, Employee](findByDepartment, departments*)
    }.executePool

    aggregationResult match {
      case scala.util.Success(employees) =>
        val byDept = employees.groupBy(_.department)
        println("Department statistics:")
        byDept.toSeq.sortBy(_._1).foreach { case (dept, emps) =>
          val avgSalary = emps.map(_.salary).sum / emps.size
          val total = emps.size
          println(f"  $dept%-15s: $total%4d employees, avg salary: $$${avgSalary}%8.2f")
        }
      case scala.util.Failure(e)         =>
        println(s"Query failed: ${e.getMessage}")
    }
    println()

    // Cleanup
    println("--- Cleanup ---")
    Sql.statement("DROP TABLE employees").executePool match {
      case scala.util.Success(_) => println("Table dropped successfully")
      case scala.util.Failure(e) => println(s"Failed to drop table: ${e.getMessage}")
    }

    println("\n=== Example Complete ===")
  }
}
