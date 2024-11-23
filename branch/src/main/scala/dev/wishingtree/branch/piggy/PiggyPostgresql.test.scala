package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import dev.wishingtree.branch.piggy.Sql.*
import org.postgresql.ds.PGSimpleDataSource

import java.sql.Connection
import javax.sql.DataSource

// This test hangs if the connection pool fails to start up!
// Need to investigate the cause of the hang.
class PiggyPostgresqlSpec extends munit.FunSuite {

  // docker run --rm  -p 5432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust postgres
  val pg = {
    val ds = new PGSimpleDataSource()
    ds.setURL("jdbc:postgresql://localhost:5432/postgres")
    ds.setUser("postgres")
    ds
  }

  given PgConnectionPool(pg)

  override def munitValueTransforms = super.munitValueTransforms ++ List(
    new ValueTransform(
      "Sql",
      { case s: Sql[?] =>
        SqlRuntime.executeAsync(s)
      }
    )
  )

  val ddl =
    """
      CREATE TABLE IF NOT EXISTS person (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL
      )
    """

  val cleanup =
    """
      |truncate table person;
      |""".stripMargin

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

  case class Person(id: Int, name: String, age: Int)

  val ins = (p: Person) =>
    ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

  val find: PsArg[Tuple1[String]] = a =>
    ps"SELECT id, name, age from person where name like $a"

  val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

  test("PiggyPostgresql") {
    for {
      _             <- Sql.statement(ddl)
      _             <- Sql.statement(cleanup)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      fetchedPeople <- Sql
                         .prepareQuery[Tuple1[String], (Int, String, Int)](
                           find,
                           Tuple1("Mark-%")
                         )
                         .map(_.map(Person.apply))
    } yield {
      assertEquals(nIns, 10)
      assertEquals(fetchedPeople.distinct.size, 10)
    }
//    sql.execute(using pool)
  }

}
