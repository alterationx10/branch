---
title: Piggy
description: Lazy SQL
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-10-11T00:00:00Z
tags:
  - sql
---

# Piggy

Piggy is a library for working with SQL via `java.sql`. It helps marshal data into and out of SQL statements using type
classes and provides a composable API for database operations. The library focuses on safe SQL operations rather than
imposing opinions about data modeling.

## High Level Overview

The library consists of several key components:

- **Composable SQL Operations**: Database interactions are modeled lazily using the `Sql[A]` data structure, similar to
  the [Lzy](/lzy) module. Operations are composed as a "description" and evaluated when desired.
- **Type-Safe SQL Writing**: String interpolators help write SQL strings and safely capture arguments for prepared
  statements, with support for both positional and named parameters.
- **Type Class Based Parsing**: Type classes help safely marshal data between Scala types and SQL, both for parameters
  and results, with automatic support for nullable columns via `Option[T]`.
- **Transaction Management**: Automatic transaction handling with rollback on failures.
- **Rich Error Context**: Custom exception types provide detailed SQL context for easier debugging.

## Composing and Executing Sql[A]

Like the [Lzy](/lzy) library, Piggy lets you compose a series of SQL operations that are executed later. Here's a simple
example:

```scala
case class Person(id: Int, name: String, age: Int) derives ResultSetParser

val sql: Sql[(Int, Seq[Person])] = for {
  _ <- Sql.statement(ddl) // Create table
  nInserted <- Sql.prepareUpdate(insert, people *) // Insert records
  found <- Sql.prepareQuery[String, Person]( // Query with type-safe parsing
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

The interpolator replaces variables with `?` placeholders and captures the arguments to be set safely on the
PreparedStatement. This helps prevent SQL injection while keeping queries readable.

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

val ddl =
  """
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
  nInserted <- Sql.prepareUpdate(insert, people *)
  found <- Sql.prepareQuery[String, Person](find, "Mark-%")
} yield (nInserted, found)

// Execute with a connection pool (sync)
val result: Try[(Int, Seq[Person])] = sql.executePool()
```

## Ad-hoc Updates with psUpdate

For single-use INSERT/UPDATE/DELETE operations, Piggy provides a convenient interpolator that returns `Sql[Int]` directly:

```scala
// Single INSERT operation
val inserted = psUpdate"INSERT INTO users (name, email) VALUES ($name, $email)"
  .executePool
  .get // Returns Int (affected rows)

// Use in for-comprehensions
val result = for {
  _ <- psUpdate"INSERT INTO users (name) VALUES ($name)"
  users <- Sql.statement("SELECT * FROM users", _.parsedList[User])
} yield users
```

**When to use:**

- **psUpdate**: Ad-hoc, single-use INSERT/UPDATE/DELETE operations
- **ps with prepareUpdate**: Batch UPDATE operations with multiple rows
- **ps with prepareQuery**: Parameterized SELECT queries

## Named Parameters

Piggy supports named parameters for improved code readability and refactoring safety:

```scala
// INSERT with named parameters
psNamed"INSERT INTO users (name, email, age) VALUES (:name, :email, :age)"
  .bindUpdate(
    "name" -> "John Doe",
    "email" -> "john@example.com",
    "age" -> 30
  )
  .executePool

// SELECT with named parameters
val users = psNamed"SELECT * FROM users WHERE age > :minAge AND status = :status"
  .bindQuery[User](
    "minAge" -> 25,
    "status" -> "active"
  )
  .executePool
  .get
```

**Benefits of named parameters:**

- Parameter order doesn't matter
- Self-documenting queries
- Easier refactoring
- Can reuse the same parameter multiple times

## Option Support for Nullable Columns

Piggy provides automatic support for nullable database columns using `Option[T]`:

```scala
// Case class with optional fields
case class User(
                 id: Int,
                 name: String,
                 email: Option[String], // Nullable column
                 lastLogin: Option[Instant] // Nullable timestamp
               ) derives ResultSetParser

// Insert with None
psUpdate"INSERT INTO users (name, email) VALUES ($name, ${None: Option[String]})"
  .executePool

// Insert with Some
psUpdate"INSERT INTO users (name, email) VALUES ($name, ${Some("user@example.com")})"
  .executePool

// Query returns Option for nullable columns
val users = Sql.statement(
  "SELECT * FROM users",
  _.parsedList[User]
).executePool.get

// users will have None for NULL emails
```

`Option[T]` works automatically with all supported types, including `java.time.*` types.

## Supported Types

Piggy supports a comprehensive set of types out of the box:

**Primitive Types:**

- `Int`, `Long`, `Float`, `Double`, `Short`, `Byte`, `Boolean`

**Common Types:**

- `String`, `BigDecimal`, `Array[Byte]`, `UUID`

**Date/Time Types:**

- `java.sql.Date`, `java.sql.Time`, `java.sql.Timestamp`
- `java.time.Instant`, `java.time.LocalDate`, `java.time.LocalDateTime`, `java.time.ZonedDateTime`

**Other Types:**

- `java.math.BigInteger`

**Nullable Columns:**

- `Option[T]` for any of the above types

All types work seamlessly with both parameter setting and result parsing. Custom types can be added by implementing
`PreparedStatementSetter[T]` and `ResultSetGetter[T]`.

## Error Handling

Piggy provides custom exception types with rich SQL context for easier debugging:

```scala
try {
  sql.executePool
} catch {
  case e: PiggyException.SqlExecutionException =>
    // Simple statement failed
    // e.sql contains the failing SQL
    log.error(s"SQL failed: ${e.sql}", e)

  case e: PiggyException.BatchExecutionException =>
    // Batch operation failed
    // e.sql contains the SQL, e.batchSize contains the batch size
    log.error(s"Batch failed: ${e.sql}, size: ${e.batchSize}", e)

  case e: PiggyException.EmptyArgumentException =>
    // Called prepare/prepareUpdate/prepareQuery with empty args
    log.error(s"Empty arguments: ${e.operation}", e)

  case e: PiggyException.MissingNamedParametersException =>
    // Named parameter binding missing required parameters
    // e.missingParams contains the parameter names
    log.error(s"Missing params: ${e.missingParams.mkString(", ")}", e)

  case e: PiggyException =>
    // Catch-all for any Piggy exception
    log.error("SQL operation failed", e)
}
```

All Piggy exceptions extend `PiggyException` and include contextual information to help diagnose issues quickly.

## Performance

Piggy uses JDBC's batch API for efficient bulk operations:

```scala
val people = (1 to 10000).map(i => Person(0, s"Person-$i", i))

val sql = for {
  _ <- Sql.statement(ddl)
  n <- Sql.prepareUpdate(insert, people *) // Uses executeBatch()
} yield n

// 10,000 inserts in ~1 second (single round trip)
val result = sql.executePool
```

The library automatically uses `addBatch()` and `executeBatch()` for prepared statements with multiple arguments,
dramatically reducing round trips to the database.

## Other Libraries

If you like *Piggy*, you should check out [Magnum](https://github.com/AugustNagro/magnum)
