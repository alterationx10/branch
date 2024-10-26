package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.piggy.Sql.ps
import munit.FunSuite

import java.sql.{Connection, DriverManager}

class ResourcePoolSpec extends FunSuite {

  case class H2Pool() extends ResourcePool[Connection] {

    override def acquire: Connection =
      DriverManager.getConnection("jdbc:h2:~/test", "sa", "")

    override def release(resource: Connection): Unit = resource.close()
  }

  val ddl =
    """
    CREATE TABLE IF NOT EXISTS person (
      id SERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      age INT NOT NULL
    )
  """

  test("ResourcePool") {
    val pool = H2Pool()

    pool.use(conn => {
      val stmt = conn.createStatement()
      stmt.execute(ddl)
      stmt.execute(s"TRUNCATE TABLE person")
      stmt.close()
    })

    (1 to 100).foreach { i =>
      pool.use[Unit] { conn =>
        val name = s"Mark-$i"
        val age  = i
        val ins  =
          ps"INSERT INTO person (name, age) VALUES ($name, $age)" (using conn)
        ins.execute()
      }
    }

    val nRecords = pool.use(conn => {
      val stmt  = conn.createStatement()
      val rs    = stmt.executeQuery("SELECT count(1) FROM person")
      rs.next()
      val count = rs.getInt(1)
      stmt.close()
      count
    })

    assertEquals(nRecords, 100)
  }

}
