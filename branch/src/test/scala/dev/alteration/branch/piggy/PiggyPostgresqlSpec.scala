package dev.alteration.branch.piggy

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.testkit.testcontainers.PGContainerSuite
import Sql.*
import PreparedStatementSetter.given

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.util.Try

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

  case class Person(id: Int, name: String, age: Int) derives ResultSetParser

  val ins: Person => PsArgHolder = (p: Person) =>
    ps"INSERT INTO person (name, age) values (${p.name}, ${p.age + 10})"

  val find: String => PsArgHolder = (a: String) =>
    ps"SELECT id, name, age from person where name like $a"

  val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

  test("PiggyPostgresql") {
    val sql    = for {
      _             <- Sql.statement(ddl)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      fetchedPeople <- Sql
                         .prepareQuery[String, Person](
                           find,
                           "Mark-%"
                         )
    } yield (nIns, fetchedPeople)
    val result = sql.executePool(using pgPool)
    assert(result.isSuccess)
    assertEquals(result.get._1, 10)
    assertEquals(result.get._2.distinct.size, 10)
    assert(result.get._2.forall(p => p.id + 10 == p.age))
  }

  test("PiggyPostgresql Rollback") {
    given PgConnectionPool = pgPool
    assert(Sql.statement(ddl).executePool.isSuccess)

    val blowup = for {
      nIns <- Sql.prepareUpdate(ins, tenPeople*)
      _    <- Sql.statement("this is not valid sql")
    } yield nIns
    assert(blowup.executePool.isFailure)

    val sql                      = for {
      fetchedPeople <- Sql
                         .prepareQuery[String, Person](
                           find,
                           "Mark-%"
                         )
    } yield {
      fetchedPeople
    }
    val result: Try[Seq[Person]] = sql.executePool
    assert(result.isSuccess)
    assertEquals(result.get.size, 0)

  }

  test("ResultSet Tupled") {
    given pool: PgConnectionPool = pgPool

    given rsParse: ResultSetParser[(Int, String)] =
      ResultSetParser.ofTuple[(Int, String)]

    val tple =
      Sql.statement(s"SELECT 1, 'two'", _.parsed[(Int, String)]).executePool

    assertEquals(tple.get.get, (1, "two"))
  }

  test("ResultSet TupledList") {
    given pool: PgConnectionPool = pgPool

    given rsParse: ResultSetParser[(Int, String, Int)] =
      ResultSetParser.ofTuple[(Int, String, Int)]

    val readBack = {
      for {
        _             <- Sql.statement(ddl)
        _             <- Sql.statement("truncate table person")
        _             <- Sql.prepareUpdate(ins, tenPeople*)
        fetchedPeople <- Sql.statement(
                           "select * from person where name like 'Mark%'",
                           _.parsedList[(Int, String, Int)]
                         )
      } yield fetchedPeople
    }.executePool.get

    assert(readBack.size == 10)
    assert(readBack.forall(_._2.startsWith("Mark")))
    assert(readBack.forall(t => t._1 + 10 == t._3))
  }

  test("Sql.fail") {
    val sql    = for {
      _             <- Sql.statement(ddl)
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      _             <- Sql.fail(new Exception("boom"))
      fetchedPeople <- Sql
                         .prepareQuery[String, Person](
                           find,
                           "Mark-%"
                         )
    } yield (nIns, fetchedPeople)
    val result = sql.executePool(using pgPool)
    assert(result.isFailure)
    assert(result.toEither.left.exists(_.getMessage == "boom"))
  }

  test("ResultSetParser - UUID") {
    given pool: PgConnectionPool = pgPool
    val uuid                     =
      Sql.statement("SELECT gen_random_uuid()", _.parsed[UUID]).executePool
    assert(uuid.isSuccess)
    assert(uuid.get.nonEmpty)
    assert(
      Sql.statement("SELECT 'boom'", _.parsed[UUID]).executePool.isFailure
    )
  }

  test("ResultSetParser - Instant") {
    given pool: PgConnectionPool = pgPool

    val now = Instant
      .now()
      .minusSeconds(1) // Help to avoid a race condition on fast tests
    val uuid = Sql.statement("SELECT now()", _.parsed[Instant]).executePool
    assert(uuid.isSuccess)
    assert(uuid.get.nonEmpty)
    assert(uuid.get.get.isAfter(now))
    assert(
      uuid.get.exists(i => java.time.Duration.between(i, now).toMillis < 1000)
    )
  }

  test("Batch operations with different SQL types") {
    given pool: PgConnectionPool = pgPool

    val complexDDL = """
      CREATE TABLE IF NOT EXISTS complex_types (
        id SERIAL PRIMARY KEY,
        int_val INT,
        long_val BIGINT,
        float_val REAL,
        double_val DOUBLE PRECISION,
        bool_val BOOLEAN,
        string_val TEXT
      )
    """

    case class ComplexRow(
        id: Int,
        intVal: Int,
        longVal: Long,
        floatVal: Float,
        doubleVal: Double,
        boolVal: Boolean,
        stringVal: String
    ) derives ResultSetParser

    val insert = (r: ComplexRow) => ps"""
      INSERT INTO complex_types 
      (int_val, long_val, float_val, double_val, bool_val, string_val)
      VALUES 
      (${r.intVal}, ${r.longVal}, ${r.floatVal}, ${r.doubleVal}, 
       ${r.boolVal}, ${r.stringVal})
    """

    val testRows = Seq(
      ComplexRow(0, 42, 123L, 3.14f, 2.718, true, "hello"),
      ComplexRow(
        0,
        -1,
        Long.MaxValue,
        Float.MinValue,
        Double.MaxValue,
        false,
        "world"
      )
    )

    val result = {
      for {
        _    <- Sql.statement(complexDDL)
        n    <- Sql.prepareUpdate(insert, testRows*)
        rows <- Sql.statement(
                  "SELECT * FROM complex_types ORDER BY id",
                  _.parsedList[ComplexRow]
                )
      } yield (n, rows)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 2)
    assertEquals(result.get._2.size, 2)
  }

  test("Large result set handling") {
    given pool: PgConnectionPool = pgPool

    // Create test table with many rows
    val setup = for {
      _ <- Sql.statement(ddl)
      _ <- Sql.statement("truncate table person")
      n <- Sql.prepareUpdate(
             ins,
             (1 to 10000).map(i => Person(0, s"Person-$i", i))*
           )
    } yield n

    assert(setup.executePool.isSuccess)

    // Test retrieving large result set
    val query = Sql.statement(
      "SELECT * FROM person ORDER BY id",
      _.parsedList[Person]
    )

    val result = query.executePool
    assert(result.isSuccess)
    assertEquals(result.get.size, 10000)
    assert(result.get.map(_.name).distinct.size == 10000)
  }

}
