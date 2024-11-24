package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import dev.wishingtree.branch.piggy.Sql.*
import dev.wishingtree.branch.testkit.testcontainers.PGContainerSuite

import java.sql.Connection
import javax.sql.DataSource

class PiggyPostgresqlSpec extends PGContainerSuite {

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

  case class Person(id: Int, name: String, age: Int)

  val ins = (p: Person) =>
    ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

  val find: PsArg[Tuple1[String]] = a =>
    ps"SELECT id, name, age from person where name like $a"

  val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

  test("PiggyPostgresql") {
    val sql    = for {
      _             <- Sql.statement(ddl)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      fetchedPeople <- Sql
                         .prepareQuery[Tuple1[String], (Int, String, Int)](
                           find,
                           Tuple1("Mark-%")
                         )
                         .map(_.map(Person.apply))
    } yield (nIns, fetchedPeople)
    val result = sql.executePool(using PgConnectionPool(ds))
    assert(result.isSuccess)
    assertEquals(result.get._1, 10)
    assertEquals(result.get._2.distinct.size, 10)
  }

  test("PiggyPostgresql Rollback") {
    given PgConnectionPool = PgConnectionPool(ds)
    assert(Sql.statement(ddl).executePool.isSuccess)

    val blowup = for {
      nIns <- Sql.prepareUpdate(ins, tenPeople*)
      _    <- Sql.statement("this is not valid sql")
    } yield nIns
    assert(blowup.executePool.isFailure)

    val sql    = for {
      fetchedPeople <- Sql
                         .prepareQuery[Tuple1[String], (Int, String, Int)](
                           find,
                           Tuple1("Mark-%")
                         )
                         .map(_.map(Person.apply))
    } yield {
      fetchedPeople
    }
    val result = sql.executePool
    assert(result.isSuccess)
    assertEquals(result.get.size, 0)

  }

}
