package app.wishingtree

import dev.wishingtree.branch.lzy.LazyRuntime
import dev.wishingtree.branch.piggy.ResourcePool
import dev.wishingtree.branch.piggy.Sql.ps
import org.postgresql.ds.PGSimpleDataSource

import java.sql.Connection
import javax.sql.DataSource
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object PiggyExample {

  val ddl =
    """
    CREATE TABLE IF NOT EXISTS person (
      id SERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      age INT NOT NULL
    )
  """

  case class PgConnectionPool(ds: DataSource) extends ResourcePool[Connection] {

    override def acquire: Connection = {
      ds.getConnection()
    }

    override def release(resource: Connection): Unit = {
      resource.close()
    }

    override def test(resource: Connection): Boolean =
      resource.isValid(5)
  }

  val pg = {
    val ds = new PGSimpleDataSource()
    ds.setURL("jdbc:postgresql://localhost:5432/postgres")
    ds.setUser("postgres")
    ds
  }

  val connPool = PgConnectionPool(pg)

  // Run everything on virtual threads
  given ExecutionContext = LazyRuntime.executionContext

  // sql modeling lite :tm:
  case class Person(id: Int, name: String, age: Int) {
    def prepareInsert(using connection: Connection) =
      ps"INSERT INTO person (name, age) VALUES (${this.name}, ${this.age})"
  }

  def main(args: Array[String]): Unit = {

    // Clear out previous rus
    connPool.use(conn => {
      val stmt = conn.createStatement()
      stmt.execute(ddl)
      stmt.execute(s"TRUNCATE TABLE person")
      stmt.close()
    })

    // Insert 100 records sequentially
    (1 to 100).foreach { i =>
      println(s"Hello from ${i}")
      connPool.use[Unit] { conn =>
        println(s"Creating person ${i}")
        val name = s"Mark-$i"
        val age  = i
        val ins  =
          ps"INSERT INTO person (name, age) VALUES ($name, $age)" (using conn)
        ins.execute()
        println(s"Created person ${i}")
      }
    }

    // Insert 100 records in parallel
    val futures = (1 to 100).map { i =>
      Future {
        println(s"Hello from ${i}")
        connPool.use[Unit] { conn =>
          println(s"Creating person ${i}")
          Person(-1, s"Mark-$i", i).prepareInsert(using conn).execute()
          println(s"Created person ${i}")
        }
      }
    }

    // Wait for all futures to complete, so we don't terminate early
    Await.result(Future.sequence(futures), Duration.Inf)

  }
}
