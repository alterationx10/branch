package app.wishingtree

import dev.wishingtree.branch.lzy.LazyRuntime
import dev.wishingtree.branch.piggy.ResourcePool
import dev.wishingtree.branch.piggy.Sql.ps
import org.postgresql.ds.PGSimpleDataSource

import java.sql.{Connection, PreparedStatement}
import javax.sql.DataSource
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import dev.wishingtree.branch.piggy.Sql.*
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

  // docker run --rm  -p 5432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust postgres
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

    connPool.use { conn =>
      given Connection           = conn
      val name                   = "Alterationx10"
      val age                    = 1234
      val ins: PreparedStatement =
        ps"INSERT INTO person (name, age) VALUES ($name, $age)"
        // Turns into INSERT INTO person (name, age) VALUES (?, ?)
        // and then provides the values to the PreparedStatement
      ins.execute()
      val q                      =
        ps"SELECT name, age FROM person where name = $name"
      val resultSet              = q.executeQuery()
      val (dbName, dbAge)        =
        resultSet.tupledList[(String, Int)].head
        // Iterates through the result sets and
        // pulls out values based in the type and
        // length of the Tuple type argument.
        // use tupledList[T <: Tuple] for multiple rows
      println(s"Name: $dbName, Age: $dbAge")
    }

  }
}
