package dev.wishingtree.branch.piggy

import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors
import dev.wishingtree.branch.piggy.Sql.*
import dev.wishingtree.branch.testkit.testcontainers.PGContainerSuite

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class PiggyPostgresqlSpec extends PGContainerSuite {

  given ExecutionContext = BranchExecutors.executionContext

  override val munitTimeout = Duration(10, "s")

  val ddl =
    """
      CREATE TABLE IF NOT EXISTS person (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL
      )
    """

  case class Person(id: Int, name: String, age: Int)

  val ins = (p: Person) =>
    ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

  val find = (a: String) =>
    ps"SELECT id, name, age from person where name like $a"

  val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

  test("PiggyPostgresql") {
    val sql    = for {
      _             <- Sql.statement(ddl)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      fetchedPeople <- Sql
                         .prepareQuery[String, (Int, String, Int)](
                           find,
                           "Mark-%"
                         )
                         .map(_.map(Person.apply))
    } yield (nIns, fetchedPeople)
    val result = sql.executePool()(using pgPool)
    assert(result.isSuccess)
    assertEquals(result.get._1, 10)
    assertEquals(result.get._2.distinct.size, 10)
  }

  test("PiggyPostgresql Rollback") {
    given PgConnectionPool = pgPool
    assert(Sql.statement(ddl).executePool().isSuccess)

    val blowup = for {
      nIns <- Sql.prepareUpdate(ins, tenPeople*)
      _    <- Sql.statement("this is not valid sql")
    } yield nIns
    assert(blowup.executePool().isFailure)

    val sql    = for {
      fetchedPeople <- Sql
                         .prepareQuery[String, (Int, String, Int)](
                           find,
                           "Mark-%"
                         )
                         .map(_.map(Person.apply))
    } yield {
      fetchedPeople
    }
    val result = sql.executePool()
    assert(result.isSuccess)
    assertEquals(result.get.size, 0)

  }

  test("ResultSet Tupled") {
    given pool: PgConnectionPool = pgPool

    val tple =
      Sql.statement(s"SELECT 1, 'two'", _.tupled[(Int, String)]).executePool()

    assertEquals(tple.get.get, (1, "two"))
  }

  test("ResultSet TupledList") {
    given pool: PgConnectionPool = pgPool

    val readBack = {
      for {
        _             <- Sql.statement(ddl)
        _             <- Sql.statement("truncate table person")
        _             <- Sql.prepareUpdate(ins, tenPeople*)
        fetchedPeople <- Sql.statement(
                           "select * from person where name like 'Mark%'",
                           _.tupledList[(Int, String, Int)]
                         )
      } yield fetchedPeople
    }.executePool().get

    assert(readBack.size == 10)
    assert(readBack.forall(_._2.startsWith("Mark")))
  }

  test("Sql.fail") {
    val sql    = for {
      _             <- Sql.statement(ddl)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      _             <- Sql.fail(new Exception("boom"))
      fetchedPeople <- Sql
                         .prepareQuery[String, (Int, String, Int)](
                           find,
                           "Mark-%"
                         )
                         .map(_.map(Person.apply))
    } yield (nIns, fetchedPeople)
    val result = sql.executePool()(using pgPool)
    assert(result.isFailure)
    assert(result.toEither.left.exists(_.getMessage == "boom"))
  }
}
