package lzy.async

import dev.alteration.branch.lzy.Lazy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Examples demonstrating asynchronous and parallel execution with Lazy.
  *
  * This example shows:
  *   - race: Run computations concurrently, take the first to complete
  *   - parSequence: Execute multiple Lazy values in parallel
  *   - parTraverse: Map and execute in parallel
  *   - fromFuture: Integrate with Scala Futures
  *   - runAsync: Execute Lazy computations asynchronously
  *
  * To run this example: sbt "examples/runMain lzy.async.ParallelExample"
  */
object ParallelExample {

  def main(args: Array[String]): Unit = {
    println("=== Lazy Parallel Operations Example ===\n")

    // Example 1: Racing two computations
    println("--- Example 1: Race ---")
    val fast = Lazy.fn {
      Thread.sleep(100)
      println("  Fast computation completed")
      "Fast"
    }
    val slow = Lazy.fn {
      Thread.sleep(500)
      println("  Slow computation completed")
      "Slow"
    }

    val raceResult = fast.race(slow)
    println(s"Race winner: ${raceResult.runSync().get}")
    Thread.sleep(600) // Let slow one finish for demonstration
    println()

    // Example 2: Race with different types (union types)
    println("--- Example 2: Race with Different Types ---")
    val leftInt    = Lazy.fn {
      Thread.sleep(200)
      42
    }
    val rightString = Lazy.fn {
      Thread.sleep(100)
      "Hello"
    }

    val unionResult: Int | String = leftInt.race(rightString).runSync().get
    unionResult match {
      case i: Int    => println(s"Int won: $i")
      case s: String => println(s"String won: $s")
    }
    println()

    // Example 3: Sequential vs Parallel execution
    println("--- Example 3: Sequential vs Parallel ---")
    val tasks = List(
      Lazy.fn {
        Thread.sleep(200)
        println("  Task 1 done")
        1
      },
      Lazy.fn {
        Thread.sleep(200)
        println("  Task 2 done")
        2
      },
      Lazy.fn {
        Thread.sleep(200)
        println("  Task 3 done")
        3
      }
    )

    println("Sequential execution:")
    val seqStart  = System.currentTimeMillis()
    val seqResult = Lazy.sequence(tasks).runSync().get
    val seqTime   = System.currentTimeMillis() - seqStart
    println(s"  Results: $seqResult")
    println(s"  Time: ${seqTime}ms")
    println()

    println("Parallel execution:")
    val parStart  = System.currentTimeMillis()
    val parResult = Lazy.parSequence(tasks).runSync().get
    val parTime   = System.currentTimeMillis() - parStart
    println(s"  Results: $parResult")
    println(s"  Time: ${parTime}ms")
    println()

    // Example 4: ParTraverse
    println("--- Example 4: ParTraverse ---")
    val numbers = List(1, 2, 3, 4, 5)
    val start   = System.currentTimeMillis()
    val doubled = Lazy.parTraverse(numbers) { n =>
      Lazy.fn {
        Thread.sleep(100)
        n * 2
      }
    }
    val result = doubled.runSync().get
    val time   = System.currentTimeMillis() - start
    println(s"  Input: $numbers")
    println(s"  Output: $result")
    println(s"  Time: ${time}ms (would be ${numbers.size * 100}ms sequential)")
    println()

    // Example 5: FromFuture integration
    println("--- Example 5: FromFuture ---")
    val future = Future {
      Thread.sleep(100)
      println("  Future computation completed")
      "Result from Future"
    }

    val fromFuture = Lazy.fromFuture(future)
    println(s"Result: ${fromFuture.runSync().get}")
    println()

    // Example 6: RunAsync
    println("--- Example 6: RunAsync ---")
    val asyncComp = Lazy.fn {
      Thread.sleep(200)
      "Async result"
    }

    println("Starting async computation...")
    val futureResult = asyncComp.runAsync
    println("Doing other work while waiting...")
    Thread.sleep(100)
    println(s"Result: ${scala.concurrent.Await.result(futureResult, scala.concurrent.duration.Duration.Inf)}")
    println()

    // Example 7: Combining parallel and sequential
    println("--- Example 7: Mixed Parallel and Sequential ---")
    val workflow = for {
      // First phase: parallel data fetching
      data <- Lazy.parSequence(
                List(
                  Lazy.fn {
                    Thread.sleep(100)
                    "DataA"
                  },
                  Lazy.fn {
                    Thread.sleep(100)
                    "DataB"
                  }
                )
              )
      // Second phase: sequential processing
      processed <- Lazy.sequence(
                     data.map { d =>
                       Lazy.fn {
                         println(s"  Processing $d")
                         s"Processed-$d"
                       }
                     }
                   )
    } yield processed

    println(s"Result: ${workflow.runSync().get}")
    println()

    println("=== Example Complete ===")
  }
}
