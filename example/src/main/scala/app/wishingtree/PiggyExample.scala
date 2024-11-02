package app.wishingtree

import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import dev.wishingtree.branch.piggy.Sql.*
import dev.wishingtree.branch.piggy.Sql
import org.postgresql.ds.PGSimpleDataSource

import java.sql.Connection
import javax.sql.DataSource
import scala.util.*
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

    case class Person(id: Int, name: String, age: Int)

    val ins = (p: Person) =>
      ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

    
    val find: PsArg[Tuple1[String]] = a =>
      ps"SELECT id, name, age from person where name like $a"

    val tenPeople = (1 to 10).map(i => Person(0, s"Mark-$i", i))

    val lazyPiggy = for {
      _             <- Sql.statement(ddl)
      _             <- Sql.statement("truncate table person")
      nIns          <- Sql.prepareUpdate(ins, tenPeople*)
      fetchedPeople <- Sql.prepareQuery[Tuple1[String], (Int, String, Int)](
                         find,
                         Tuple1("Mark%")
                       ).map(_.map(Person.apply))
    } yield (nIns, fetchedPeople)

    lazyPiggy.execute match {
      case Success(value) => {
        println(s"Inserted ${value._1} piggies")
        value._2.foreach(println)
      }
      case Failure(e)     => println(s"Piggy went boom: ${e.getMessage}")
    }

//    println(Sql.statement(ddl).execute)
//    println(Sql.statement("insert into person (name, age) values ('Mark', 123)").execute)
//    println(Sql.statement("select * from person where name = 'Mark'", rs => rs.tupled[(Int, String, Int)]).execute)
//    println(Sql.statement("truncate table person").execute)
//
//    println(Sql.prepare[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234)).execute)
//    println(Sql.prepareUpdate[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234)).execute)
//    println(Sql.prepareUpdate[(String, Int)]((a: String, b: Int) => ps"insert into person (name, age) values ($a, $b)", ("Mark", 1234), ("Mark", 1234), ("Mark", 1234)).execute)
//
//
//    println {
//      Sql.prepareQuery[Tuple1[String], (String, Int)](a => ps"select name, age from person where name like $a", Tuple1("Mark%")).execute
//    }

  }
}
