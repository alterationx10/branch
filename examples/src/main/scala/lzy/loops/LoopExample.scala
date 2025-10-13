package lzy.loops

import dev.alteration.branch.lzy.Lazy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/** Examples demonstrating loops and repetition with Lazy.
  *
  * The Lazy type provides several ways to repeat computations:
  *   - until: Repeat until the result satisfies a condition
  *   - repeatUntil: Repeat until an external condition becomes true
  *   - repeatWhile: Repeat while an external condition remains true
  *   - retryN: Retry a computation N times on failure
  *
  * To run this example: sbt "examples/runMain lzy.loops.LoopExample"
  */
object LoopExample {

  def main(args: Array[String]): Unit = {
    println("=== Lazy Loop Operations Example ===\n")

    // Example 1: Until - repeat until result meets condition
    println("--- Example 1: Until (Result-based) ---")
    val random       = new Random()
    var untilAttempts = 0
    val findFive     = Lazy.fn {
      untilAttempts += 1
      val value = random.nextInt(10)
      println(s"  Attempt $untilAttempts: Generated $value")
      value
    }.until(_ == 5)

    println(s"Result: ${findFive.runSync().get}")
    println(s"Total attempts: $untilAttempts")
    println()

    // Example 2: RepeatUntil - repeat until external condition
    println("--- Example 2: RepeatUntil (External condition) ---")
    var counter = 0
    val repeatUntilExample = Lazy.fn {
      counter += 1
      println(s"  Iteration $counter")
      s"Value at iteration $counter"
    }.repeatUntil(counter >= 3)

    println(s"Result: ${repeatUntilExample.runSync().get}")
    println()

    // Example 3: RepeatWhile - repeat while condition is true
    println("--- Example 3: RepeatWhile ---")
    var remaining = 5
    val repeatWhileExample = Lazy.fn {
      remaining -= 1
      println(s"  Remaining: $remaining")
      remaining
    }.repeatWhile(remaining > 0)

    println(s"Result: ${repeatWhileExample.runSync().get}")
    println()

    // Example 4: Combining loops with other operations
    println("--- Example 4: Loop with Processing ---")
    var sum = 0
    val accumulator = Lazy.fn {
      val value = random.nextInt(20)
      sum += value
      println(s"  Added $value, sum now: $sum")
      sum
    }.until(_ >= 50)

    println(s"Final sum: ${accumulator.runSync().get}")
    println()

    // Example 5: Retry with retryN
    println("--- Example 5: RetryN on Failure ---")
    var attempt = 0
    val unreliableOp = Lazy.fn {
      attempt += 1
      println(s"  Attempt $attempt")
      if (attempt < 3) {
        throw new Exception(s"Failed on attempt $attempt")
      }
      "Success!"
    }.retryN(5)

    println(s"Result: ${unreliableOp.runSync().get}")
    println()

    // Example 6: Polling example
    println("--- Example 6: Polling Simulation ---")
    var jobStatus = "pending"
    var pollCount = 0
    val pollJob   = Lazy.fn {
      pollCount += 1
      println(s"  Poll $pollCount: Status is $jobStatus")

      // Simulate job completion after 3 polls
      if (pollCount >= 3) {
        jobStatus = "complete"
      }

      jobStatus
    }.until(_ == "complete")

    println(s"Final status: ${pollJob.runSync().get}")
    println()

    // Example 7: Countdown timer
    println("--- Example 7: Countdown ---")
    var countdown = 5
    val timer     = Lazy
      .fn {
        println(s"  T-$countdown")
        countdown -= 1
        Thread.sleep(200)
        countdown
      }
      .repeatWhile(countdown >= 0)

    timer.runSync()
    println("  Liftoff!")
    println()

    // Example 8: Retry with increasing delays
    println("--- Example 8: Retry with Backoff ---")
    var backoffAttempt = 0
    val withBackoff = Lazy
      .fn {
        backoffAttempt += 1
        println(s"  Attempt $backoffAttempt")
        if (backoffAttempt < 3) {
          throw new Exception("Not ready yet")
        }
        "Success"
      }
      .recover { _ =>
        // Add delay between retries
        import scala.concurrent.duration.*
        Lazy.sleep(100.milliseconds).flatMap(_ =>
          Lazy.fn {
            println(s"  Waiting before retry...")
            throw new Exception("Try again")
          }
        )
      }
      .retryN(5)

    println(s"Result: ${withBackoff.runSync().get}")
    println()

    // Example 9: Conditional loops with forEach
    println("--- Example 9: ForEach with Conditions ---")
    val items = List(1, 2, 3, 4, 5)
    val processed = Lazy.forEach(items) { item =>
      Lazy.fn {
        val doubled = item * 2
        println(s"  Processing $item -> $doubled")
        doubled
      }
    }

    println(s"Results: ${processed.runSync().get}")
    println()

    // Example 10: Using until for validation
    println("--- Example 10: Retry Until Valid ---")
    var validationAttempts = 0
    val validInput = Lazy.fn {
      validationAttempts += 1
      val input = random.nextInt(100)
      println(s"  Generated: $input")
      input
    }.until(n => n >= 10 && n <= 90)

    println(s"Valid input found: ${validInput.runSync().get} (after $validationAttempts attempts)")
    println()

    println("=== Example Complete ===")
  }
}
