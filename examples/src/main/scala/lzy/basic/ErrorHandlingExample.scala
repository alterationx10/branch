package lzy.basic

import dev.alteration.branch.lzy.Lazy

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

/** Examples demonstrating error handling in Lazy computations.
  *
  * The Lazy type provides several ways to handle errors:
  *   - recover: Handle errors and provide alternative computations
  *   - orElse: Fallback to another computation on failure
  *   - retryN: Retry failed computations
  *   - timeout: Fail if computation takes too long
  *   - either: Convert errors to Either values
  *
  * To run this example: sbt "examples/runMain lzy.basic.ErrorHandlingExample"
  */
object ErrorHandlingExample {

  def main(args: Array[String]): Unit = {
    println("=== Lazy Error Handling Example ===\n")

    // Example 1: Basic error and failure
    println("--- Example 1: Failing Computation ---")
    val failing = Lazy.fn {
      throw new RuntimeException("Something went wrong!")
    }
    println(s"Result: ${failing.runSync()}")
    println()

    // Example 2: Recovering from errors
    println("--- Example 2: Recover ---")
    val recovered = Lazy
      .fn[Int] {
        println("  Attempting operation...")
        throw new RuntimeException("Failed!")
      }
      .recover { error =>
        println(s"  Caught error: ${error.getMessage}")
        println("  Recovering with default value")
        Lazy.fn(0)
      }
    println(s"Result: ${recovered.runSync().get}")
    println()

    // Example 3: OrElse for fallback
    println("--- Example 3: OrElse Fallback ---")
    val withFallback = Lazy
      .fn[String] {
        println("  Trying primary source...")
        throw new Exception("Primary failed")
      }
      .orElse {
        println("  Trying fallback...")
        Lazy.fn("Fallback value")
      }
    println(s"Result: ${withFallback.runSync().get}")
    println()

    // Example 4: OrElseDefault for simple defaults
    println("--- Example 4: OrElseDefault ---")
    val withDefault = Lazy
      .fail[Int](new Exception("Error"))
      .orElseDefault(42)
    println(s"Result: ${withDefault.runSync().get}")
    println()

    // Example 5: Retry with retryN
    println("--- Example 5: Retry ---")
    var attempts = 0
    val retried  = Lazy
      .fn {
        attempts += 1
        println(s"  Attempt $attempts")
        if (attempts < 3) throw new Exception("Not yet!")
        "Success!"
      }
      .retryN(5)
    println(s"Result: ${retried.runSync().get}")
    println()

    // Example 6: Timeout
    println("--- Example 6: Timeout ---")
    val slowComputation = Lazy
      .fn {
        println("  Starting slow computation...")
        Thread.sleep(3000)
        "Done"
      }
      .timeout(1.second)
    println(s"Result: ${slowComputation.runSync()}")
    println()

    // Example 7: Successful timeout
    println("--- Example 7: Fast Computation with Timeout ---")
    val fastComputation = Lazy
      .fn {
        println("  Quick computation")
        "Done quickly"
      }
      .timeout(5.seconds)
    println(s"Result: ${fastComputation.runSync().get}")
    println()

    // Example 8: Converting errors to Either
    println("--- Example 8: Either ---")
    val asEither = Lazy
      .fn[Int] {
        throw new IllegalArgumentException("Invalid input")
      }
      .either
    println(s"Result: ${asEither.runSync().get}")
    println()

    // Example 9: Successful Either
    println("--- Example 9: Successful Either ---")
    val successEither = Lazy.fn(42).either
    println(s"Result: ${successEither.runSync().get}")
    println()

    // Example 10: MapError to transform errors
    println("--- Example 10: MapError ---")
    val mappedError = Lazy
      .fail[String](new Exception("Original error"))
      .mapError(e => new IllegalStateException(s"Wrapped: ${e.getMessage}"))
      .recover { e =>
        println(s"  Caught: ${e.getClass.getSimpleName}: ${e.getMessage}")
        Lazy.fn("recovered")
      }
    println(s"Result: ${mappedError.runSync().get}")
    println()

    // Example 11: TapError for side effects on errors
    println("--- Example 11: TapError ---")
    val tappedError = Lazy
      .fail[Int](new Exception("Error"))
      .tapError(e => println(s"  Logging error: ${e.getMessage}"))
      .orElseDefault(0)
    println(s"Result: ${tappedError.runSync().get}")
    println()

    // Example 12: Optional - convert failures to None
    println("--- Example 12: Optional ---")
    val optional = Lazy
      .fail[String](new Exception("Failed"))
      .optional
    println(s"Result: ${optional.runSync().get}")
    println()

    println("=== Example Complete ===")
  }
}
