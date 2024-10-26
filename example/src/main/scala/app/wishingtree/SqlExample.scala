package app.wishingtree

import dev.wishingtree.branch.piggy.Sql.ps
import org.postgresql.ds.PGSimpleDataSource

import java.sql.{Connection, PreparedStatement}

object SqlExample extends App {

  val ddl = """
    CREATE TABLE IF NOT EXISTS person (
      id SERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      age INT NOT NULL
    )
  """

  given con: Connection = {
    val pg = new PGSimpleDataSource()
    pg.setURL("jdbc:postgresql://localhost:5432/postgres")
    pg.setUser("postgres")
    pg.getConnection
  }

  con.createStatement().execute(ddl)
  val name = "Mark"
  val age  = 66
  val ins  = ps"INSERT INTO person (name, age) VALUES ($name, $age)"
  ins.execute()

  case class Person(id: Int, name: String, age: Int)

  val insertPerson: Person => PreparedStatement = { p =>
    ps"INSERT INTO person (name, age) VALUES (${p.name}, ${p.age})" // connection still needs t be here
  }

  insertPerson(Person(-1, "Mark2", 123)).execute()

}
