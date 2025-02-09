---
title: Lzy
description: Somewhere between lazy Futures, and a tiny Effect System.
author: Mark Rudolph
published: 2025-01-25T04:37:00Z
lastUpdated: 2025-01-25T04:37:00Z
tags:
  - future
  - effect
  - lazy
---

# Lzy

_Lzy_ is somewhere between lazy Futures, and a tiny Effect System. It provides a way to describe computations that can be composed and executed later, with built-in support for error handling, resource management, and logging.

## Futures vs Lazy: A Comparison

A (Scala) `Future` is an eager calculation - once referenced, it's going to run! This also means once it's done, we
can't re-run it - only evaluate the finished state. Sometimes it's beneficial to describe some logic in a lazy fashion,
where it's more of a blueprint of what to do. Calling the blueprint runs the code, whose output can be assigned to a
value, but then the blueprint can be run again!

Let's see a simple comparison between Future and Lazy:

```scala
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Random

// Future Example
given ExecutionContext = LazyRuntime.executionContext

val f1: Future[Int] = Future(Random.nextInt(10))
// f1 is already running, kicked off by the ExecutionContext
val f2: Future[Int] = Future(Random.nextInt(10))
// f2 is also already running...

def fRandomSum: Future[Int] = for {
  a <- f1  // Will always get the same value
  b <- f2  // Will always get the same value
} yield (a + b)

// The sum will be the same every time
println(Await.result(fRandomSum, Duration.Inf))
println(Await.result(fRandomSum, Duration.Inf))
```

```scala
// Lazy Example
val l1: Lazy[Int] = Lazy.fn(Random.nextInt(10))
// l1 is just a description, nothing runs yet
val l2: Lazy[Int] = Lazy.fn(Random.nextInt(10))
// l2 is also just a description

def lzyRandomSum: Lazy[Int] = for {
  a <- l1  // New random number each time
  b <- l2  // New random number each time
} yield (a + b)

// The sum will be different each time
println(lzyRandomSum.runSync)
println(lzyRandomSum.runSync)
```

This lazy evaluation approach lets you:

1. Describe complex operations without executing them immediately
2. Re-run the same computation multiple times
3. Add error handling and other effects where needed
4. Control exactly when and how the computation runs

For example, we can add error handling without modifying the original computation:

```scala
def myLazyOp(arg: Int): Lazy[Int] =
  Lazy.fn(42 / arg)

// Without error handling
myLazyOp(0).runSync()
// -> Failure(java.lang.ArithmeticException: / by zero)

// With error handling added at the call site
myLazyOp(0)
  .recover(_ => Lazy.fn(0))
  .runSync()
// => Success(0)

// Or with different error handling elsewhere
myLazyOp(0)
  .recoverSome { case _: ArithmeticException => Lazy.fn(-1) }
  .runSync()
// => Success(-1)
```

## Core Features

Lzy provides a rich set of features to help you build and compose lazy computations. At its core, Lzy is built around the concept of describing computations rather than executing them immediately. This allows you to:

- Build complex workflows by composing smaller operations
- Handle errors gracefully with various recovery strategies
- Control execution timing and flow
- Work with resources safely
- Integrate logging seamlessly
- Process collections efficiently

All operations in Lzy are lazy by default - they're just descriptions until you explicitly run them using `runSync` or `runAsync`.

### Creating Lazy Values

There are several ways to create Lazy values:

```scala
// From a direct value
val simple: Lazy[Int] = Lazy.fn(42)

// From a computation
val computed: Lazy[Int] = Lazy.fn {
  println("Computing...")
  42
}

// From a Try
val fromTry: Lazy[Int] = Lazy.fromTry(Try(42 / 0))

// From an Option
val fromOption: Lazy[Int] = Lazy.fromOption(Some(42))

// Create a failed computation
val failed: Lazy[Int] = Lazy.fail(new Exception("Boom!"))

// Get current time
val now: Lazy[Instant] = Lazy.now()
```

### Error Handling

Lzy provides several ways to handle errors:

```scala
def myLazyOp(arg: Int): Lazy[Int] =
  Lazy.fn(42 / arg)

// Basic recovery
myLazyOp(0).recover(_ => Lazy.fn(0))

// Recover with pattern matching
myLazyOp(0).recoverSome {
  case _: ArithmeticException => Lazy.fn(0)
}

// Provide a default value
myLazyOp(0).orElseDefault(0)

// Map/tap errors
myLazyOp(0)
  .mapError(e => new Exception(s"Failed with: ${e.getMessage}"))
  .tapError(e => println(s"Error occurred: ${e.getMessage}"))

// Retry on failure
myLazyOp(0).retryN(3) // Retry up to 3 times
```

### Timing Control

You can add delays and pauses to your Lazy computations:

```scala
import scala.concurrent.duration._

Lazy.fn("hello")
  .delay(1.second)    // Delay before execution
  .pause(500.millis)  // Pause after execution
```

### Optional Values

Work with optional values elegantly:

```scala
// Convert success to Some, failure to None
val optional: Lazy[Option[Int]] = myLazyOp(0).optional

// Run computation only if condition is true
val conditional: Lazy[Option[Int]] = Lazy.when(someCondition)(myLazyOp(42))

// Pattern match with whenCase
val matched: Lazy[Option[Int]] = Lazy.whenCase(value) {
  case n if n > 0 => Lazy.fn(n * 2)
}

// Handle Options
Lazy.fromOption(Some(42))  // Convert Option to Lazy
  .getOrElse(0)           // Default if None
```

### Resource Management

Safely work with resources that need to be released:

```scala
import scala.util.Using

// Resource is automatically closed after use
Lazy.using(scala.io.Source.fromFile("data.txt")) { source =>
  source.getLines().toList
}
```

### Logging Integration

Built-in logging support with different log levels:

```scala
import java.util.logging.Logger
given logger: Logger = Logger.getLogger("MyApp")

for {
  _ <- Lazy.logInfo("Starting operation")
  result <- myLazyOp(42)
  _ <- Lazy.logFine(s"Operation completed with: $result")
  _ <- Lazy.logConfig("Configuration updated")
} yield result
```

### Collection Operations

Work with collections efficiently:

```scala
// Iterate over collections
Lazy.iterate(Iterator(1,2,3))(List.newBuilder[Int])(_ => Lazy.fn(1))

// Process each element
Lazy.forEach(List(1,2,3))(n => Lazy.fn(n * 2))
```

### Composing Operations

Lazy values can be composed using for-comprehensions or combinators:

```scala
// Using for-comprehension
for {
  a <- Lazy.fn(40)
  b <- Lazy.fn(2)
  _ <- Lazy.logInfo(s"Adding $a and $b")
  sum <- Lazy.fn(a + b)
} yield sum

// Using combinators
Lazy.fn(40)
  .flatMap(a => Lazy.fn(2).map(b => a + b))
  .tap(sum => Lazy.logInfo(s"Sum is $sum"))
```

## Running Lazy Values

There are several ways to execute a Lazy value:

```scala
val myLazy: Lazy[Int] = Lazy.fn(42)

// Synchronous execution
val result: Try[Int] = myLazy.runSync
val resultWithTimeout: Try[Int] = myLazy.runSync(5.seconds)

// Asynchronous execution
val future: Future[Int] = myLazy.runAsync
```

## Creating Lazy Applications

You can easily create full applications using the `LazyApp` trait:

```scala
object MyApp extends LazyApp {
  // Customize the execution context if needed
  override val executionContext: ExecutionContext =
    BranchExecutors.executionContext

  // Define your main application logic
  override def run: Lazy[Any] = for {
    _ <- Lazy.logInfo("Starting application")
    result <- myLazyOp(42)
    _ <- Lazy.logInfo(s"Got result: $result")
  } yield result
}
```

The `LazyApp` trait provides:

- A default virtual-thread-per-task execution context
- Automatic handling of the main method
- Built-in logging support

## Other Libraries

If you like _Lzy_, you should check out [ZIO](https://zio.dev/) for a more comprehensive effect system.
