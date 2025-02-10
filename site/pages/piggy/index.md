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

Piggy is a library for working with SQL via `java.sql`. It helps marshal data into and out of SQL statements using type classes and provides a composable API for database operations. The library focuses on safe SQL operations rather than imposing opinions about data modeling.

## High Level Overview

The library consists of several key components:

- **Composable SQL Operations**: Database interactions are modeled lazily using the `Sql[A]` data structure, similar to the [Lzy](/lzy) module. Operations are composed as a "description" and evaluated when desired.
- **Type-Safe SQL Writing**: A string interpolator helps write SQL strings and safely capture arguments for prepared statements.
- **Type Class Based Parsing**: Type classes help safely marshal data between Scala types and SQL, both for parameters and results.
- **Transaction Management**: Automatic transaction handling with rollback on failures.

## Composing and Executing Sql[A]

Like the [Lzy](/lzy) library, Piggy lets you compose a series of SQL operations that are executed later. Here's a simple example:

```scala
case class Person(id: Int, name: String, age: Int) derives ResultSetParser

val sql: Sql[(Int, Seq[Person])] = for {
  _ <- Sql.statement(ddl)  // Create table
  nInserted <- Sql.prepareUpdate(insert, people*)  // Insert records
  found <- Sql.prepareQuery[String, Person](  // Query with type-safe parsing
    find,
    "Mark-%"
  )
} yield (nInserted, found)
```

The `sql` value is just a description - it hasn't executed anything yet. To run it, use one of the execution methods:

```scala
// Synchronous execution (no ExecutionContext needed)
val result: Try[(Int, Seq[Person])] = sql.execute()
val poolResult: Try[(Int, Seq[Person])] = sql.executePool()

// Async execution (requires ExecutionContext)
given ExecutionContext = BranchExecutors.executionContext
val futureResult: Future[(Int, Seq[Person])] = sql.executeAsync()
val futurePoolResult: Future[(Int, Seq[Person])] = sql.executePoolAsync()
```

Each execution method handles transactions automatically - rolling back on failure and committing on success.

## Writing SQL Safely

The library provides a `ps` string interpolator to help write SQL strings and capture arguments safely:

```scala
case class Person(id: Int, name: String, age: Int)

val insert: Person => PsArgHolder = (p: Person) =>
  ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

val find: String => PsArgHolder = (pattern: String) =>
  ps"SELECT id, name, age from person where name like $pattern"
```

The interpolator replaces variables with `?` placeholders and captures the arguments to be set safely on the PreparedStatement. This helps prevent SQL injection while keeping queries readable.

## Type-Safe Parsing with Type Classes

Piggy uses type classes to safely marshal data between Scala and SQL:

- `PreparedStatementSetter[A]`: For setting parameters on prepared statements
- `ResultSetGetter[A]`: For getting values from result sets
- `ResultSetParser[A]`: For parsing entire rows into Scala types

The library provides instances for common types and can derive instances for case classes:

```scala
// Built-in support for common types
val uuid = Sql.statement("SELECT gen_random_uuid()", _.parsed[UUID])
val timestamp = Sql.statement("SELECT now()", _.parsed[Instant])

// Derived parser for case classes
case class Person(id: Int, name: String, age: Int) derives ResultSetParser

// Use the derived parser
val people = Sql.prepareQuery[String, Person](find, "Mark-%")
```

For custom types, you can provide your own instances:

```scala
given ResultSetParser[MyType] with {
  def parse(rs: ResultSet): MyType = 
    MyType(rs.getString(1), rs.getInt(2))
}
```

## Complete Example

Here's a complete example showing the pieces working together:

```scala
case class Person(id: Int, name: String, age: Int) derives ResultSetParser

val ddl = """
  CREATE TABLE IF NOT EXISTS person (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    age INT NOT NULL
  )
"""

val insert: Person => PsArgHolder = (p: Person) =>
  ps"INSERT INTO person (name, age) values (${p.name}, ${p.age})"

val find: String => PsArgHolder = (pattern: String) =>
  ps"SELECT id, name, age from person where name like $pattern"

// Compose operations
val sql = for {
  _ <- Sql.statement(ddl)
  nInserted <- Sql.prepareUpdate(insert, people*)
  found <- Sql.prepareQuery[String, Person](find, "Mark-%")
} yield (nInserted, found)

// Execute with a connection pool (sync)
val result: Try[(Int, Seq[Person])] = sql.executePool()
```

## Other Libraries

If you like *Piggy*, you should check out [Magnum](https://github.com/AugustNagro/magnum)
