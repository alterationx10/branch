package app.wishingtree

import dev.wishingtree.branch.piggy.{ResourcePool, Sql, SqlRuntime}
import dev.wishingtree.branch.piggy.Sql.*
import org.postgresql.ds.PGSimpleDataSource

import java.sql.Connection
import javax.sql.DataSource
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

  def main(args: Array[String]): Unit = {

    given PgConnectionPool(pg)

    println(Sql.statement(ddl).execute)
    println(Sql.statement("insert into person (name, age) values ('Mark', 123)").execute)
    println(Sql.statement("select * from person where name = 'Mark'", rs => rs.tupled[(Int, String, Int)]).execute)
    println(Sql.statement("truncate table person").execute)

    println(Sql.prepare[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234)).execute)
    println(Sql.prepareUpdate[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234)).execute)
    println(Sql.prepareUpdate[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234), ("Mark", 1234), ("Mark", 1234)).execute)
    

    println {
      Sql.prepareQuery[Tuple1[String], (String, Int)](a => ps"select name, age from person where name = $a", Tuple1("Mark")).execute
    }


  }
}
