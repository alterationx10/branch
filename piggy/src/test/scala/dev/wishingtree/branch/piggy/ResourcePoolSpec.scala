package dev.wishingtree.branch.piggy

import munit.FunSuite
import Sql.*
import dev.wishingtree.branch.macaroni.poolers.ResourcePool

import java.sql.{Connection, DriverManager}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

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
    given pool: ResourcePool[Connection] = H2Pool()

    val asyncSum = (1 to 100).map { _ =>
      Sql.statement(s"SELECT 1", rs => { rs.next(); rs.getInt(1) }).executeAsync
    }

    val sum = Await
      .result(
        Future.sequence(asyncSum)(implicitly, SqlRuntime.executionContext),
        Duration.Inf
      )
      .sum

    assertEquals(sum, 100)
  }

  test("ResultSet Tupled") {
    given pool: ResourcePool[Connection] = H2Pool()

    val tple =
      Sql.statement(s"SELECT 1, 'two'", _.tupled[(Int, String)]).execute

    assertEquals(tple.get, (1, "two"))
  }

  test("ResultSet TupledList") {
    given pool: ResourcePool[Connection] = H2Pool()

    case class Person(id: Int, name: String, age: Int)

    val ins = (p: Person) =>
      ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

    val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

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
    }.execute.get

    assert(readBack.size == 10)
    assert(readBack.forall(_._2.startsWith("Mark")))
  }

}
