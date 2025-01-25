---
title: Piggy
description: Lazy SQL
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

## High Level Overview

There are a few moving parts to go over, which will be broken up into three sections

- Composing and Executing `Sql[A]` : Database interactions are done lazily in a `Sql[A]` data structure, similar to
  the [Lzy](/lzy)
  module. You compose a series of steps as a "description", and then use a runtime to evaluate those steps when desired.
- Writing SQL: The library provides a string interpolator to help you write SQL strings, and capture arguments to be
  used in a prepared statement.
- Parsing `ResultSet`s: The library helps to parse a `ResultSet` into the expected `Tuple` of a result, that can then be
  marshaled to case classes if desired.
- Putting it all together: A simple example of how these pieces fir together.

## Composing and Executing Sql[A]

Piggy is structure like the [Lzy](/lzy) library, where you define a series of steps (`Sql[A]`s) you want to take, and
then execute series.

Without any deeper context yet (we'll come back to it), this will end up looking like:

```scala 3
val sql: Sql[(Int, Seq[Person])] = for {
  _ <- Sql.statement(ddl)
  nIns <- Sql.prepareUpdate(insert, tenPeople *)
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

Some of the magic is hidden here with implicits, so let's look at the method type signature of the extension method:

```scala 3

extension [A](a: Sql[A]) {

  /** Execute this Sql operation using the given Connection. See
   * [[SqlRuntime.execute]].
   */
  def execute(d: Duration = Duration.Inf)(using
                                          connection: Connection,
                                          executionContext: ExecutionContext
  ): Try[A] = {
    SqlRuntime.execute(a, d)
  }
}
```

Our extension method has a default argument for the `Duration` (how long to wait for the result) to `Duration.Inf`.

We need an implicit `ExecutionContext` in scope to run the underlying `Future`s on (
`given ExecutionContext = BranchExecutors.executionContext` to use the virtual-thread-per-task one in `BranchExecutors`,
or others if you want).

We also need a `Connection` to interact with the database. Let's look at the default implementation on `SqlRuntime`:

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

The same connection is used for all steps in the series, and if the evaluation of the series fails, the transaction will
be rolled back. Note, the `evalF` method wraps each step in a `Future.fromTry`, so they are executed sequentially.

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

val insert: Person => PsArgHolder = (p: Person) =>
  ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

val find: String => PsArgHolder = (a: String) =>
  ps"SELECT id, name, age from person where name like $a"
```

The string is interpolated to replace all variable arguments with `?`, and the arguments are additionally captured in a
`PsArgHolder` instance. This will be used to set the arguments to a `PreparedStatement` in the order they are passed in.

## Parsing Results

When describing a `Sql[A]` that is going to return a ResultSet, there is a type argument to indicate the expected shape
of the tupled results.

For example, the `prepareQuery` below shows that my input will require a `String` argument, and the result is expected
be a `Tuple3[Int, String, Int]`:

```scala 3
Sql
  .prepareQuery[String, (Int, String, Int)](
    find,
    "Mark-%"
  )
```

This correlates to our `find`.

```scala 3
val find: String => PsArgHolder = (a: String) =>
  ps"SELECT id, name, age from person where name like $a"
```

We're expecting to get back a `ResultSet` with three columns (id, name, and age), and we want to parse the rows to
`(Int, String, Int)`.

There is some machinery in place during execution to automatically iterate through a `ResultSet`, and grab typed values
for the length of the tuple provided as the type argument. Without much further discussion, this looks a bit like:

```scala 3
private inline def parseRs[T <: Tuple](rs: ResultSet)(index: Int): Tuple =
  inline erasedValue[T] match {
    case _: EmptyTuple =>
      EmptyTuple
    case _: (t *: ts) =>
      summonInline[ResultSetGetter[t]].get(rs)(index) *: parseRs[ts](rs)(
        index + 1
      )
  }
```

which is made available as an extension to a `ResultSet` via

```scala 3
extension (rs: ResultSet) {

  /** Parse the ResultSet into a List of Tuples.
   */
  inline def tupledList[A <: Tuple]: List[A] = {
    val b = List.newBuilder[A]
    while rs.next() do b += parseRs[A](rs)(1).asInstanceOf[A]
    b.result()
  }

  /** Parse the ResultSet into an Option of a Tuple.
   */
  inline def tupled[A <: Tuple]: Option[A] = {
    tupledList[A].headOption
  }

}
```

A`ResultSetGetter` type class instance helps parse the correct types, as provided by the tuple type parameter.

```scala 3
/** A ResultSetGetter for String */
given ResultSetGetter[String] = new ResultSetGetter[String] {
  def get(rs: ResultSet)(index: Int): String = rs.getString(index)
}
```

Note: The library does not (currently) help you ensure that your declared tuple is the correct shape of the query!

## Putting It All Together

Now that we have been able to delve a little deeper into the library, let's see how it all fits together. Let's look at
a test case; this set up that has our table creation, some data to insert, and a query to run:

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

val insert: Person => PsArgHolder = (p: Person) =>
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
  nIns <- Sql.prepareUpdate(insert, tenPeople *)
  fetchedPeople <- Sql
    .prepareQuery[String, (Int, String, Int)](
      find,
      "Mark-%"
    )
    .map(_.map(Person.apply))
} yield (nIns, fetchedPeople)
```

and evaluate to a result (using a `Connection` and `ExecutionContext` in scope) with

```scala 3
val result: Try[Seq[Person]] = sql.executePool()
```

## Other Libraries

If you like *Piggy*, you should check out [Magnum](https://github.com/AugustNagro/magnum)
