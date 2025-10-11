package dev.alteration.branch.piggy

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.testkit.testcontainers.PGContainerSuite
import Sql.*
import PreparedStatementSetter.given

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime, ZoneId}
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

  test("Named parameters - INSERT") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _ <- Sql.statement(ddl)
        _ <- Sql.statement("truncate table person")
        n <- psNamed"INSERT INTO person (name, age) VALUES (:name, :age)"
               .bindUpdate(
                 "name" -> "Named User",
                 "age"  -> 42
               )
      } yield n
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get, 1)
  }

  test("Named parameters - SELECT") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _      <- Sql.statement(ddl)
        _      <- Sql.statement("truncate table person")
        _      <-
          psUpdate"INSERT INTO person (name, age) VALUES (${"Named Test"}, ${35})"
        people <- psNamed"SELECT * FROM person WHERE name = :name"
                    .bindQuery[Person]("name" -> "Named Test")
      } yield people
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get.size, 1)
    assertEquals(result.get.head.name, "Named Test")
    assertEquals(result.get.head.age, 35)
  }

  test("Named parameters - parameter order independence") {
    given pool: PgConnectionPool = pgPool

    val result1 = {
      for {
        _ <- Sql.statement(ddl)
        _ <- Sql.statement("truncate table person")
        n <- psNamed"INSERT INTO person (name, age) VALUES (:name, :age)"
               .bindUpdate("name" -> "Order1", "age" -> 25)
      } yield n
    }.executePool

    val result2 = {
      for {
        n <- psNamed"INSERT INTO person (name, age) VALUES (:name, :age)"
               .bindUpdate("age" -> 26, "name" -> "Order2") // Different order
      } yield n
    }.executePool

    assert(result1.isSuccess)
    assert(result2.isSuccess)
    assertEquals(result1.get, 1)
    assertEquals(result2.get, 1)

    val allPeople =
      Sql
        .statement("SELECT * FROM person ORDER BY age", _.parsedList[Person])
        .executePool
    assertEquals(allPeople.get.size, 2)
    assertEquals(allPeople.get(0).age, 25)
    assertEquals(allPeople.get(1).age, 26)
  }

  test("Named parameters - missing parameter throws exception") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _ <- Sql.statement(ddl)
        n <- psNamed"INSERT INTO person (name, age) VALUES (:name, :age)"
               .bindUpdate("name" -> "Incomplete") // Missing :age
      } yield n
    }.executePool

    assert(result.isFailure)
    assert(
      result.toEither.left.exists(
        _.isInstanceOf[PiggyException.MissingNamedParametersException]
      )
    )
  }

  test("Named parameters - multiple uses of same parameter") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _      <- Sql.statement(ddl)
        _      <- Sql.statement("truncate table person")
        _      <-
          psUpdate"INSERT INTO person (name, age) VALUES (${"Duplicate"}, ${30})"
        people <-
          psNamed"SELECT * FROM person WHERE name = :name OR age = (SELECT age FROM person WHERE name = :name)"
            .bindQuery[Person]("name" -> "Duplicate")
      } yield people
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get.size, 1)
  }

  test("Named parameters - ignore parameters in string literals") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _      <- Sql.statement(ddl)
        _      <- Sql.statement("truncate table person")
        _      <-
          psUpdate"INSERT INTO person (name, age) VALUES (${"User:Test"}, ${40})"
        people <-
          psNamed"SELECT * FROM person WHERE name = :name AND name ILIKE '%:test%'"
            .bindQuery[Person]("name" -> "User:Test")
      } yield people
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get.size, 1)
    assertEquals(result.get.head.name, "User:Test")
  }

  test("Named parameters - complex query with multiple parameters") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _      <- Sql.statement(ddl)
        _      <- Sql.statement("truncate table person")
        _      <- Sql.prepareUpdate(ins, tenPeople*)
        people <-
          psNamed"SELECT * FROM person WHERE age >= :minAge AND age <= :maxAge AND name LIKE :pattern"
            .bindQuery[Person](
              "minAge"  -> 15,
              "maxAge"  -> 25,
              "pattern" -> "Mark-%"
            )
      } yield people
    }.executePool

    assert(result.isSuccess)
    assert(result.get.nonEmpty)
    assert(result.get.forall(p => p.age >= 15 && p.age <= 25))
  }

  test("Option support - nullable columns with None") {
    given pool: PgConnectionPool = pgPool

    val ddlWithNullable = """
      CREATE TABLE IF NOT EXISTS person_with_email (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL,
        email TEXT
      )
    """

    case class PersonWithEmail(
        id: Int,
        name: String,
        age: Int,
        email: Option[String]
    ) derives ResultSetParser

    val result = {
      for {
        _      <- Sql.statement(ddlWithNullable)
        _      <- Sql.statement("truncate table person_with_email")
        n      <-
          psUpdate"INSERT INTO person_with_email (name, age, email) VALUES (${"John"}, ${30}, ${None: Option[String]})"
        people <- Sql.statement(
                    "SELECT * FROM person_with_email WHERE name = 'John'",
                    _.parsedList[PersonWithEmail]
                  )
      } yield (n, people)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 1)
    assertEquals(result.get._2.size, 1)
    assertEquals(result.get._2.head.email, None)
  }

  test("Option support - nullable columns with Some") {
    given pool: PgConnectionPool = pgPool

    val ddlWithNullable = """
      CREATE TABLE IF NOT EXISTS person_with_email (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL,
        email TEXT
      )
    """

    case class PersonWithEmail(
        id: Int,
        name: String,
        age: Int,
        email: Option[String]
    ) derives ResultSetParser

    val result = {
      for {
        _      <- Sql.statement(ddlWithNullable)
        _      <- Sql.statement("truncate table person_with_email")
        n      <-
          psUpdate"INSERT INTO person_with_email (name, age, email) VALUES (${"Jane"}, ${25}, ${Some("jane@example.com")})"
        people <-
          Sql.statement(
            "SELECT * FROM person_with_email WHERE name = 'Jane'",
            _.parsedList[PersonWithEmail]
          )
      } yield (n, people)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 1)
    assertEquals(result.get._2.size, 1)
    assertEquals(result.get._2.head.email, Some("jane@example.com"))
  }

  test("Option support - batch insert with mixed None and Some") {
    given pool: PgConnectionPool = pgPool

    val ddlWithNullable = """
      CREATE TABLE IF NOT EXISTS person_with_email (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL,
        email TEXT
      )
    """

    case class PersonWithEmail(
        id: Int,
        name: String,
        age: Int,
        email: Option[String]
    ) derives ResultSetParser

    val insertPerson = (p: PersonWithEmail) =>
      ps"INSERT INTO person_with_email (name, age, email) VALUES (${p.name}, ${p.age}, ${p.email})"

    val testPeople = Seq(
      PersonWithEmail(0, "Alice", 30, Some("alice@example.com")),
      PersonWithEmail(0, "Bob", 25, None),
      PersonWithEmail(0, "Charlie", 35, Some("charlie@example.com"))
    )

    val result = {
      for {
        _      <- Sql.statement(ddlWithNullable)
        _      <- Sql.statement("truncate table person_with_email")
        n      <- Sql.prepareUpdate(insertPerson, testPeople*)
        people <-
          Sql.statement(
            "SELECT * FROM person_with_email ORDER BY name",
            _.parsedList[PersonWithEmail]
          )
      } yield (n, people)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(result.get._2(0).email, Some("alice@example.com"))
    assertEquals(result.get._2(1).email, None)
    assertEquals(result.get._2(2).email, Some("charlie@example.com"))
  }

  test("Empty arguments - prepareUpdate throws EmptyArgumentException") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _ <- Sql.statement(ddl)
        n <- Sql.prepareUpdate(ins, Seq.empty*)
      } yield n
    }.executePool

    assert(result.isFailure)
    assert(
      result.toEither.left
        .exists(_.isInstanceOf[PiggyException.EmptyArgumentException])
    )
    assert(
      result.toEither.left
        .exists(
          _.getMessage.contains("PreparedUpdate requires at least one argument")
        )
    )
  }

  test("Empty arguments - prepareQuery throws EmptyArgumentException") {
    given pool: PgConnectionPool = pgPool

    val result = {
      for {
        _      <- Sql.statement(ddl)
        people <- Sql.prepareQuery[String, Person](find, Seq.empty*)
      } yield people
    }.executePool

    assert(result.isFailure)
    assert(
      result.toEither.left
        .exists(_.isInstanceOf[PiggyException.EmptyArgumentException])
    )
    assert(
      result.toEither.left
        .exists(
          _.getMessage.contains("PreparedQuery requires at least one argument")
        )
    )
  }

  test("Empty arguments - prepare throws EmptyArgumentException") {
    given pool: PgConnectionPool = pgPool

    val exec = (p: Person) =>
      ps"INSERT INTO person (name, age) VALUES (${p.name}, ${p.age})"

    val result = {
      for {
        _ <- Sql.statement(ddl)
        _ <- Sql.prepare(exec, Seq.empty*)
      } yield ()
    }.executePool

    assert(result.isFailure)
    assert(
      result.toEither.left
        .exists(_.isInstanceOf[PiggyException.EmptyArgumentException])
    )
    assert(
      result.toEither.left
        .exists(
          _.getMessage.contains("PreparedExec requires at least one argument")
        )
    )
  }

  test("java.time.LocalDate - insert and query") {
    given pool: PgConnectionPool = pgPool

    val ddlWithDate = """
      CREATE TABLE IF NOT EXISTS events (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        event_date DATE NOT NULL
      )
    """

    case class Event(id: Int, name: String, eventDate: LocalDate)
        derives ResultSetParser

    val testDate = LocalDate.of(2025, 10, 11)

    val result = {
      for {
        _      <- Sql.statement(ddlWithDate)
        _      <- Sql.statement("truncate table events")
        n      <-
          psUpdate"INSERT INTO events (name, event_date) VALUES (${"Conference"}, ${testDate})"
        events <- Sql.statement(
                    "SELECT * FROM events WHERE name = 'Conference'",
                    _.parsedList[Event]
                  )
      } yield (n, events)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 1)
    assertEquals(result.get._2.size, 1)
    assertEquals(result.get._2.head.eventDate, testDate)
  }

  test("java.time.LocalDateTime - insert and query") {
    given pool: PgConnectionPool = pgPool

    val ddlWithTimestamp = """
      CREATE TABLE IF NOT EXISTS log_entries (
        id SERIAL PRIMARY KEY,
        message TEXT NOT NULL,
        logged_at TIMESTAMP NOT NULL
      )
    """

    case class LogEntry(id: Int, message: String, loggedAt: LocalDateTime)
        derives ResultSetParser

    val testDateTime = LocalDateTime.of(2025, 10, 11, 14, 30, 45)

    val result = {
      for {
        _       <- Sql.statement(ddlWithTimestamp)
        _       <- Sql.statement("truncate table log_entries")
        n       <-
          psUpdate"INSERT INTO log_entries (message, logged_at) VALUES (${"Test log"}, ${testDateTime})"
        entries <-
          Sql.statement(
            "SELECT * FROM log_entries WHERE message = 'Test log'",
            _.parsedList[LogEntry]
          )
      } yield (n, entries)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 1)
    assertEquals(result.get._2.size, 1)
    assertEquals(result.get._2.head.loggedAt, testDateTime)
  }

  test("java.time.ZonedDateTime - insert and query") {
    given pool: PgConnectionPool = pgPool

    val ddlWithTimestamp = """
      CREATE TABLE IF NOT EXISTS scheduled_tasks (
        id SERIAL PRIMARY KEY,
        task_name TEXT NOT NULL,
        scheduled_at TIMESTAMP NOT NULL
      )
    """

    case class ScheduledTask(
        id: Int,
        taskName: String,
        scheduledAt: ZonedDateTime
    ) derives ResultSetParser

    val testZonedDateTime =
      ZonedDateTime.of(2025, 10, 11, 14, 30, 45, 0, ZoneId.systemDefault())

    val result = {
      for {
        _     <- Sql.statement(ddlWithTimestamp)
        _     <- Sql.statement("truncate table scheduled_tasks")
        n     <-
          psUpdate"INSERT INTO scheduled_tasks (task_name, scheduled_at) VALUES (${"Daily backup"}, ${testZonedDateTime})"
        tasks <-
          Sql.statement(
            "SELECT * FROM scheduled_tasks WHERE task_name = 'Daily backup'",
            _.parsedList[ScheduledTask]
          )
      } yield (n, tasks)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 1)
    assertEquals(result.get._2.size, 1)
    // Compare instants since ZonedDateTime might differ in zone representation
    assertEquals(
      result.get._2.head.scheduledAt.toInstant,
      testZonedDateTime.toInstant
    )
  }

  test("java.time types - batch insert with mixed dates") {
    given pool: PgConnectionPool = pgPool

    val ddlWithDates = """
      CREATE TABLE IF NOT EXISTS appointments (
        id SERIAL PRIMARY KEY,
        title TEXT NOT NULL,
        appointment_date DATE NOT NULL,
        created_at TIMESTAMP NOT NULL
      )
    """

    case class Appointment(
        id: Int,
        title: String,
        appointmentDate: LocalDate,
        createdAt: LocalDateTime
    ) derives ResultSetParser

    val insertAppointment = (a: Appointment) =>
      ps"INSERT INTO appointments (title, appointment_date, created_at) VALUES (${a.title}, ${a.appointmentDate}, ${a.createdAt})"

    val testAppointments = Seq(
      Appointment(
        0,
        "Meeting 1",
        LocalDate.of(2025, 10, 12),
        LocalDateTime.of(2025, 10, 11, 10, 0)
      ),
      Appointment(
        0,
        "Meeting 2",
        LocalDate.of(2025, 10, 13),
        LocalDateTime.of(2025, 10, 11, 11, 0)
      ),
      Appointment(
        0,
        "Meeting 3",
        LocalDate.of(2025, 10, 14),
        LocalDateTime.of(2025, 10, 11, 12, 0)
      )
    )

    val result = {
      for {
        _            <- Sql.statement(ddlWithDates)
        _            <- Sql.statement("truncate table appointments")
        n            <- Sql.prepareUpdate(insertAppointment, testAppointments*)
        appointments <-
          Sql.statement(
            "SELECT * FROM appointments ORDER BY appointment_date",
            _.parsedList[Appointment]
          )
      } yield (n, appointments)
    }.executePool

    assert(result.isSuccess)
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(
      result.get._2.head.appointmentDate,
      LocalDate.of(2025, 10, 12)
    )
    assertEquals(
      result.get._2(1).appointmentDate,
      LocalDate.of(2025, 10, 13)
    )
    assertEquals(
      result.get._2(2).appointmentDate,
      LocalDate.of(2025, 10, 14)
    )
  }

  test("java.time types with Option - nullable timestamps") {
    given pool: PgConnectionPool = pgPool

    val ddlWithNullableTimestamp = """
      CREATE TABLE IF NOT EXISTS tasks (
        id SERIAL PRIMARY KEY,
        title TEXT NOT NULL,
        due_date DATE,
        completed_at TIMESTAMP
      )
    """

    case class Task(
        id: Int,
        title: String,
        dueDate: Option[LocalDate],
        completedAt: Option[LocalDateTime]
    ) derives ResultSetParser

    val insertTask = (t: Task) =>
      ps"INSERT INTO tasks (title, due_date, completed_at) VALUES (${t.title}, ${t.dueDate}, ${t.completedAt})"

    val testTasks = Seq(
      Task(
        0,
        "Task 1",
        Some(LocalDate.of(2025, 10, 15)),
        Some(LocalDateTime.of(2025, 10, 11, 15, 0))
      ),
      Task(0, "Task 2", Some(LocalDate.of(2025, 10, 16)), None),
      Task(0, "Task 3", None, None)
    )

    val setup = {
      for {
        _ <- Sql.statement(ddlWithNullableTimestamp)
        _ <- Sql.statement("DROP TABLE IF EXISTS tasks CASCADE")
        _ <- Sql.statement(ddlWithNullableTimestamp)
      } yield ()
    }.executePool

    assert(setup.isSuccess)

    val result = {
      for {
        n     <- Sql.prepareUpdate(insertTask, testTasks*)
        tasks <- Sql.statement(
                   "SELECT * FROM tasks ORDER BY title",
                   _.parsedList[Task]
                 )
      } yield (n, tasks)
    }.executePool

    assert(result.isSuccess, s"Result failed: ${result}")
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(result.get._2(0).dueDate, Some(LocalDate.of(2025, 10, 15)))
    assertEquals(
      result.get._2(0).completedAt,
      Some(LocalDateTime.of(2025, 10, 11, 15, 0))
    )
    assertEquals(result.get._2(1).dueDate, Some(LocalDate.of(2025, 10, 16)))
    assertEquals(result.get._2(1).completedAt, None)
    assertEquals(result.get._2(2).dueDate, None)
    assertEquals(result.get._2(2).completedAt, None)
  }

  test("Option[UUID] - nullable UUID column") {
    given pool: PgConnectionPool = pgPool

    val ddlWithUuid = """
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username TEXT NOT NULL,
        external_id UUID
      )
    """

    case class User(id: Int, username: String, externalId: Option[UUID])
        derives ResultSetParser

    val insertUser = (u: User) =>
      ps"INSERT INTO users (username, external_id) VALUES (${u.username}, ${u.externalId})"

    val testUuid  = UUID.randomUUID()
    val testUsers = Seq(
      User(0, "user1", Some(testUuid)),
      User(0, "user2", None),
      User(0, "user3", Some(UUID.randomUUID()))
    )

    val result = {
      for {
        _     <- Sql.statement(ddlWithUuid)
        _     <- Sql.statement("DROP TABLE IF EXISTS users CASCADE")
        _     <- Sql.statement(ddlWithUuid)
        n     <- Sql.prepareUpdate(insertUser, testUsers*)
        users <- Sql.statement(
                   "SELECT * FROM users ORDER BY username",
                   _.parsedList[User]
                 )
      } yield (n, users)
    }.executePool

    assert(result.isSuccess, s"Result failed: ${result}")
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(result.get._2(0).externalId, Some(testUuid))
    assertEquals(result.get._2(1).externalId, None)
    assert(result.get._2(2).externalId.isDefined)
  }

  test("Option[Instant] - nullable timestamp column") {
    given pool: PgConnectionPool = pgPool

    val ddlWithInstant = """
      CREATE TABLE IF NOT EXISTS audit_log (
        id SERIAL PRIMARY KEY,
        action TEXT NOT NULL,
        performed_at TIMESTAMP
      )
    """

    case class AuditEntry(
        id: Int,
        action: String,
        performedAt: Option[Instant]
    ) derives ResultSetParser

    val insertEntry = (e: AuditEntry) =>
      ps"INSERT INTO audit_log (action, performed_at) VALUES (${e.action}, ${e.performedAt})"

    val testInstant = Instant.now().minusSeconds(3600)
    val testEntries = Seq(
      AuditEntry(0, "login", Some(testInstant)),
      AuditEntry(0, "logout", None),
      AuditEntry(0, "update", Some(Instant.now()))
    )

    val result = {
      for {
        _       <- Sql.statement(ddlWithInstant)
        _       <- Sql.statement("DROP TABLE IF EXISTS audit_log CASCADE")
        _       <- Sql.statement(ddlWithInstant)
        n       <- Sql.prepareUpdate(insertEntry, testEntries*)
        entries <- Sql.statement(
                     "SELECT * FROM audit_log ORDER BY action",
                     _.parsedList[AuditEntry]
                   )
      } yield (n, entries)
    }.executePool

    assert(result.isSuccess, s"Result failed: ${result}")
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(result.get._2(0).action, "login")
    assertEquals(
      result.get._2(0).performedAt.map(_.toEpochMilli),
      Some(testInstant.toEpochMilli)
    )
    assertEquals(result.get._2(1).action, "logout")
    assertEquals(result.get._2(1).performedAt, None)
    assertEquals(result.get._2(2).action, "update")
    assert(result.get._2(2).performedAt.isDefined)
  }

  test("Option[BigInteger] - nullable numeric column") {
    given pool: PgConnectionPool = pgPool

    val ddlWithBigInt = """
      CREATE TABLE IF NOT EXISTS large_numbers (
        id SERIAL PRIMARY KEY,
        label TEXT NOT NULL,
        huge_value NUMERIC(50, 0)
      )
    """

    case class LargeNumber(
        id: Int,
        label: String,
        hugeValue: Option[java.math.BigInteger]
    ) derives ResultSetParser

    val insertNumber = (n: LargeNumber) =>
      ps"INSERT INTO large_numbers (label, huge_value) VALUES (${n.label}, ${n.hugeValue})"

    val testBigInt  = new java.math.BigInteger("123456789012345678901234567890")
    val testNumbers = Seq(
      LargeNumber(0, "num1", Some(testBigInt)),
      LargeNumber(0, "num2", None),
      LargeNumber(
        0,
        "num3",
        Some(new java.math.BigInteger("999999999999999999999999999999"))
      )
    )

    val result = {
      for {
        _       <- Sql.statement(ddlWithBigInt)
        _       <- Sql.statement("DROP TABLE IF EXISTS large_numbers CASCADE")
        _       <- Sql.statement(ddlWithBigInt)
        n       <- Sql.prepareUpdate(insertNumber, testNumbers*)
        numbers <- Sql.statement(
                     "SELECT * FROM large_numbers ORDER BY label",
                     _.parsedList[LargeNumber]
                   )
      } yield (n, numbers)
    }.executePool

    assert(result.isSuccess, s"Result failed: ${result}")
    assertEquals(result.get._1, 3)
    assertEquals(result.get._2.size, 3)
    assertEquals(result.get._2(0).hugeValue, Some(testBigInt))
    assertEquals(result.get._2(1).hugeValue, None)
    assert(result.get._2(2).hugeValue.isDefined)
    assertEquals(
      result.get._2(2).hugeValue.get,
      new java.math.BigInteger("999999999999999999999999999999")
    )
  }

}
