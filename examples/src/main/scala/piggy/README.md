# Piggy Examples

Piggy is a type-safe SQL library for Scala that provides:
- String interpolators for safe SQL queries (`psUpdate`, `psNamed`)
- Automatic type class-based parsing of result sets
- Transaction management with automatic rollback on failure
- Monadic composition using for-comprehensions
- Batch operations for efficient data processing

## Prerequisites

All examples assume you have PostgreSQL running locally with the following configuration:
- **Host:** localhost
- **Port:** 5432
- **Database:** postgres
- **User:** postgres
- **Password:** postgres

You can adjust the connection settings in each example's `SimpleConnectionPool` definition.

If you have docker or podman, you can start up an instance with

```bash
podman run -p 5432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust --rm postgres
```

## Available Examples

### 1. Basic CRUD Operations
**Location:** `basic/BasicCrudExample.scala`

Demonstrates fundamental database operations:
- Creating tables and inserting records
- Querying data with `psNamed...bindQuery` and `Sql.statement`
- Updating and deleting records
- Filtering results
- Querying single columns

**Key Concepts:**
- `psUpdate` for INSERT, UPDATE, DELETE
- `psNamed...bindQuery` for SELECT queries
- `Sql.statement` for raw SQL with result parsing
- `ResultSetParser` auto-derivation for case classes

**Run:**
```bash
sbt "examples/runMain piggy.basic.BasicCrudExample"
```

### 2. Named Parameters
**Location:** `named/NamedParametersExample.scala`

Showcases the `psNamed` feature for readable, maintainable queries:
- Order-independent parameter binding
- Self-documenting queries with `:paramName` syntax
- Complex queries with multiple parameters
- Reusing parameters in subqueries and conditions
- Comparing different query styles

**Key Concepts:**
- `psNamed` with `:paramName` placeholders
- `.bindUpdate()` for INSERT/UPDATE/DELETE
- `.bindQuery[T]()` for SELECT queries
- Parameter order independence

**Run:**
```bash
sbt "examples/runMain piggy.named.NamedParametersExample"
```

### 3. Transaction Management
**Location:** `transaction/TransactionExample.scala`

Illustrates automatic transaction handling and error recovery:
- Successful atomic transactions
- Automatic rollback on failure
- Using `Sql.fail` for explicit transaction abortion
- Error recovery with `.recover`
- Multi-step complex transactions
- Database constraint violations

**Key Concepts:**
- Automatic transaction boundaries with `.executePool`
- Automatic commit on success, rollback on failure
- `Sql.fail` for business logic validation
- `.recover` for fallback strategies
- Transaction isolation

**Run:**
```bash
sbt "examples/runMain piggy.transaction.TransactionExample"
```

### 4. Batch Operations
**Location:** `batch/BatchOperationsExample.scala`

Shows efficient batch processing for large datasets:
- Batch inserts with `Sql.prepareUpdate`
- Batch queries with `Sql.prepareQuery`
- Processing 1000+ records efficiently
- Batch updates (e.g., salary adjustments)
- Aggregating results from batch queries
- Working with `java.time` types (LocalDate)

**Key Concepts:**
- `Sql.prepareUpdate` for batch INSERT/UPDATE
- `Sql.prepareQuery` for batch SELECT
- Function-based prepared statement generation
- Efficient processing of large datasets
- Type-safe handling of temporal types

**Run:**
```bash
sbt "examples/runMain piggy.batch.BatchOperationsExample"
```

## Common Patterns

### Connection Pool Setup

All examples use a simple connection pool implementation:

```scala
case class SimpleConnectionPool(url: String, username: String, password: String)
    extends ResourcePool[Connection] {
  override def acquire: Connection =
    DriverManager.getConnection(url, username, password)

  override def release(resource: Connection): Unit =
    if (resource != null && !resource.isClosed) resource.close()
}

given pool: ResourcePool[Connection] = SimpleConnectionPool(
  url = "jdbc:postgresql://localhost:5432/postgres",
  username = "postgres",
  password = "postgres"
)
```

### Case Class Mapping

Piggy automatically derives `ResultSetParser` for case classes:

```scala
case class User(id: Int, name: String, email: String, age: Int)
    derives ResultSetParser
```

### Query Execution

Queries are executed using `.executePool` which returns a `Try[A]`:

```scala
val result = {
  psNamed"SELECT * FROM users WHERE age > :minAge"
    .bindQuery[User]("minAge" -> 25)
}.executePool

result match {
  case Success(users) => println(s"Found ${users.size} users")
  case Failure(e)     => println(s"Query failed: ${e.getMessage}")
}
```

### For-Comprehension Composition

Multiple operations can be composed in a single transaction:

```scala
val result = {
  for {
    _     <- Sql.statement(createTableDDL)
    count <- psUpdate"INSERT INTO users (name, age) VALUES ($name, $age)"
    users <- psNamed"SELECT * FROM users WHERE age > :minAge"
               .bindQuery[User]("minAge" -> 18)
  } yield (count, users)
}.executePool
```

## Type Support

Piggy provides built-in support for common SQL types:

- **Primitives:** Int, Long, Float, Double, Boolean, String
- **Numeric:** BigDecimal, BigInteger, Short, Byte
- **Temporal:** `java.time.LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Instant`
- **Other:** UUID, Array[Byte]
- **Nullable:** Option[T] for any supported type T

## Error Handling

Piggy provides detailed exceptions for common failure cases:

- **`SqlExecutionException`** - SQL execution errors
- **`BatchExecutionException`** - Batch operation failures
- **`PreparedStatementException`** - Prepared statement errors
- **`MissingNamedParametersException`** - Missing named parameters
- **`EmptyArgumentException`** - Empty argument lists for batch operations

## Additional Resources

- **Source Code:** `branch/src/main/scala/dev/alteration/branch/piggy/`
- **Tests:** `branch/src/test/scala/dev/alteration/branch/piggy/PiggyPostgresqlSpec.scala`
- **Documentation:** See inline scaladoc in source files

## Tips

1. **Use named parameters** for complex queries with many parameters
2. **Batch operations** are significantly faster for bulk inserts/updates
3. **Transactions are automatic** - all operations in a for-comprehension are atomic
4. **Derive ResultSetParser** automatically for case classes
5. **Option[T] support** handles NULL values cleanly
6. **Type safety** - compiler catches type mismatches at compile time
