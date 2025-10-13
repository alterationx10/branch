package lzy.basic

import dev.alteration.branch.lzy.Lazy

import scala.concurrent.ExecutionContext.Implicits.global

/** A basic example showing core Lazy operations.
  *
  * The Lazy type represents a deferred computation that:
  *   - Is not executed until explicitly run
  *   - Can be composed using map, flatMap, zip, etc.
  *   - Supports both synchronous and asynchronous execution
  *
  * To run this example: sbt "examples/runMain lzy.basic.BasicOperationsExample"
  */
object BasicOperationsExample {

  def main(args: Array[String]): Unit = {
    println("=== Lazy Basic Operations Example ===\n")

    // Example 1: Simple computation with Lazy.fn
    println("--- Example 1: Simple Computation ---")
    val computation = Lazy.fn {
      println("  Computing...")
      42
    }
    println("Lazy computation created (not executed yet)")
    println(s"Result: ${computation.runSync().get}")
    println()

    // Example 2: Mapping over Lazy values
    println("--- Example 2: Map ---")
    val doubled = Lazy.fn(21).map { x =>
      println(s"  Doubling $x")
      x * 2
    }
    println(s"Result: ${doubled.runSync().get}")
    println()

    // Example 3: FlatMap for sequential operations
    println("--- Example 3: FlatMap ---")
    val sequential = for {
      a <- Lazy.fn {
             println("  Step 1: Getting value")
             10
           }
      b <- Lazy.fn {
             println(s"  Step 2: Adding 5 to $a")
             a + 5
           }
      c <- Lazy.fn {
             println(s"  Step 3: Multiplying $b by 2")
             b * 2
           }
    } yield c

    println(s"Result: ${sequential.runSync().get}")
    println()

    // Example 4: Zipping Lazy values
    println("--- Example 4: Zip ---")
    val left  = Lazy.fn {
      println("  Computing left value")
      "Hello"
    }
    val right = Lazy.fn {
      println("  Computing right value")
      "World"
    }
    val zipped = left.zip(right)
    println(s"Result: ${zipped.runSync().get}")
    println()

    // Example 5: ZipWith to apply a function
    println("--- Example 5: ZipWith ---")
    val sum = Lazy.fn(10).zipWith(Lazy.fn(32)) { (a, b) =>
      println(s"  Adding $a + $b")
      a + b
    }
    println(s"Result: ${sum.runSync().get}")
    println()

    // Example 6: Chaining operations with *>
    println("--- Example 6: Chaining with *> ---")
    val chain = Lazy.println("First operation") *>
      Lazy.println("Second operation") *>
      Lazy.fn("Final result")
    println(s"Result: ${chain.runSync().get}")
    println()

    // Example 7: Using 'as' to replace a value
    println("--- Example 7: Using 'as' ---")
    val replaced = Lazy.fn {
      println("  Doing some work...")
      "ignored"
    }.as("replacement")
    println(s"Result: ${replaced.runSync().get}")
    println()

    // Example 8: Using 'tap' to perform side effects
    println("--- Example 8: Tap ---")
    val tapped = Lazy.fn(100)
      .tap(x => println(s"  Value before: $x"))
      .map(_ * 2)
      .tap(x => println(s"  Value after doubling: $x"))
      .map(_ + 10)
    println(s"Result: ${tapped.runSync().get}")
    println()

    println("=== Example Complete ===")
  }
}
