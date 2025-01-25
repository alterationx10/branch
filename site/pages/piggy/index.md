---
title: Piggy
description: Lzy SQL
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - sql
---

# Piggy

Piggy is a library for working with SQL via  `java.sql`. It tries to help to marshal data into and out of your SQL
statements. It does not offer a lof of opinions about the structure of the data in a database (as in, there is not a lot
of functionality around modeling tables around case classes).

## Writing SQL

The heart of the library involves this string interpolator extension to StringContext:

```scala
extension(sc: StringContext) {

  /** A string interpolator for creating prepared statements by capturing
   * arguments to aPsArgHolder.
   */
  def ps(args: Any*): PsArgHolder = PsArgHolder(
    sc.s(args.map(_ => "?") *),
    args *
  )
}

/** A holder for a prepared statement string and its arguments.
 */
final case class PsArgHolder(
                              psStr: String,
                              psArgs: Any*
                            ) {

  private def set(preparedStatement: PreparedStatement): Unit = {
    psArgs.zipWithIndex.map({ case (a, i) => a -> (i + 1) }).foreach {
      case (a: Int, i) => preparedStatement.setInt(i, a)
      case (a: Long, i) => preparedStatement.setLong(i, a)
      case (a: Float, i) => preparedStatement.setFloat(i, a)
      case (a: Double, i) => preparedStatement.setDouble(i, a)
      case (a: String, i) => preparedStatement.setString(i, a)
      case (a: Tuple1[String], i) => preparedStatement.setString(i, a._1)
      case (a: Boolean, i) => preparedStatement.setBoolean(i, a)
      case (u, i) =>
        throw new IllegalArgumentException(s"Unsupported type $u")
    }
  }

}
```

This allows you to *safely** **capture** arguments, and automatically build a prepared statement string. Note: *safely*
here does not mean that your SQL is correct 😅. With this, you can write things like:

```scala
case class Person(id: Int, name: String, age: Int)

val ins: Person => PsArgHolder = (p: Person) =>
  ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

val find: String => PsArgHolder = (a: String) =>
  ps"SELECT id, name, age from person where name like $a"
```

The string is interpolated to replace all variable arguments with `?`, and the arguments are additionally captured in a
`PsArgHolder` instance. This will be used to set the arguments to a `PreparedStatement` in the order they are passed in.

## Composing Sql

Piggy is structure like the [Lzy](/lzy) library, where you define a series of steps you want to take, and then execute
series.

Let's look at a test case; this set up that has our table creation, some data to insert, and a query to run:

```scala 3
val ddl =
  """
      CREATE TABLE IF NOT EXISTS person (
        id SERIAL PRIMARY KEY,
        name TEXT NOT NULL,
        age INT NOT NULL
      )
    """

case class Person(id: Int, name: String, age: Int)

val ins: Person => PsArgHolder = (p: Person) =>
  ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

val find: String => PsArgHolder = (a: String) =>
  ps"SELECT id, name, age from person where name like $a"

val tenPeople =
  (1 to 10).map(i => Person(0, s"Mark-$i", i))
```

Using these, we can compose our series of steps as:

```scala 3
val sql = for {
  _ <- Sql.statement(ddl)
  nIns <- Sql.prepareUpdate(ins, tenPeople *)
  fetchedPeople <- Sql
    .prepareQuery[String, (Int, String, Int)](
      find,
      "Mark-%"
    )
    .map(_.map(Person.apply))
} yield (nIns, fetchedPeople)
```

Our `sql` doesn't run - it's only a description of the `Sql` actions to take. To run it, we use the `SqlRuntime` (or an
extension method the `Sql` to do this for us)

```scala 3
val result: Try[(Int, Seq[Person])] = sql.execute()
```

Some of the magic is hidden here with implicits, so let's look at the method type signature on `SqlRuntime`:

```scala 3
def execute[A](
                sql: Sql[A],
                d: Duration
              )(using connection: Connection, executionContext: ExecutionContext): Try[A]
```

Our extension method has a default argument for the `Duration` (how long to wait for the result) to `Duration.Inf`.

We need an implicit `ExecutionContext` in scope to run the underlying `Future`s on (
`given ExecutionContext = BranchExecutors.executionContext` to use the virtual-thread-per-task one, or you could use the
default Scala one).

We also need a `Connection`. Let's look at the default implementation:

```scala 3
/** Execute a Sql[A] using a Connection, returning the result as a Try. The
 * entire chain of Sql operations is done over the given Connection, and the
 * transaction is rolled back on failure.
 */
override def execute[A](sql: Sql[A], d: Duration = Duration.Inf)(using
                                                                 connection: Connection,
                                                                 executionContext: ExecutionContext
): Try[A] =
  Try {
    try {
      connection.setAutoCommit(false)
      val result = Await.result(evalF(sql), d)
      connection.commit()
      result
    } catch {
      case e: Throwable =>
        connection.rollback()
        throw e
    } finally {
      connection.setAutoCommit(true)
    }

  }
```

The same connection is used for all steps in the series, and also if the evaluation of the series fails, the transaction
will be rolled back.

## Parsing Results

## Other Libraries

If you like *Piggy*, you should check out [Magnum](https://github.com/AugustNagro/magnum)
