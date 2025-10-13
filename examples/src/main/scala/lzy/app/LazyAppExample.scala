package lzy.app

import dev.alteration.branch.lzy.{Lazy, LazyApp}

import scala.io.Source
import scala.util.Random
import java.io.{File, PrintWriter}

/** A complete application example using LazyApp trait.
  *
  * LazyApp provides a convenient way to build applications with Lazy:
  *   - Automatically handles ExecutionContext
  *   - Provides a clean main method
  *   - Uses virtual threads by default for efficient concurrency
  *
  * This example simulates a data processing pipeline:
  *   1. Generate some data
  *   2. Process it in parallel
  *   3. Write results to a file
  *   4. Verify and report
  *
  * To run this example: sbt "examples/runMain lzy.app.LazyAppExample"
  */
object LazyAppExample extends LazyApp {

  // The main Lazy workflow
  override def run: Lazy[Any] = {
    for {
      _       <- Lazy.println("=== Lazy Application Example ===\n")
      // Step 1: Setup
      tempFile <- setupTempFile()
      _       <- Lazy.println("Step 1: Setup complete\n")

      // Step 2: Generate data
      _    <- Lazy.println("Step 2: Generating data...")
      data <- generateData(10)
      _    <- Lazy.println(s"  Generated ${data.size} items\n")

      // Step 3: Process data in parallel
      _         <- Lazy.println("Step 3: Processing data in parallel...")
      processed <- processData(data)
      _         <- Lazy.println(s"  Processed ${processed.size} items\n")

      // Step 4: Save results
      _    <- Lazy.println("Step 4: Saving results...")
      _    <- saveResults(tempFile, processed)
      _    <- Lazy.println(s"  Saved to ${tempFile.getAbsolutePath}\n")

      // Step 5: Verify results
      _     <- Lazy.println("Step 5: Verifying results...")
      stats <- verifyResults(tempFile)
      _     <- Lazy.println(s"  Verification complete: $stats\n")

      // Step 6: Cleanup
      _ <- Lazy.println("Step 6: Cleanup...")
      _ <- cleanup(tempFile)
      _ <- Lazy.println("  Cleanup complete\n")

      _ <- Lazy.println("=== Application Complete ===")
    } yield ()
  }

  // Setup a temporary file
  private def setupTempFile(): Lazy[File] = Lazy.fn {
    val file = File.createTempFile("lazy-app-", ".txt")
    file.deleteOnExit()
    file
  }

  // Generate some random data
  private def generateData(count: Int): Lazy[List[Int]] = Lazy.fn {
    val random = new Random()
    (1 to count).map(_ => random.nextInt(100)).toList
  }

  // Process data in parallel
  private def processData(data: List[Int]): Lazy[List[String]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    Lazy
      .parTraverse(data) { item =>
        Lazy.fn {
          // Simulate processing time
          Thread.sleep(50)
          val result = item * 2
          s"Item $item -> Processed $result"
        }
      }
      .recover { error =>
        Lazy.println(s"  Error during processing: ${error.getMessage}")
          .flatMap(_ => Lazy.fn(List.empty[String]))
      }
  }

  // Save results to file
  private def saveResults(file: File, results: List[String]): Lazy[Unit] = {
    Lazy
      .using(new PrintWriter(file)) { writer =>
        results.foreach(writer.println)
      }
      .tapError { error =>
        println(s"  Failed to save results: ${error.getMessage}")
      }
      .unit
  }

  // Verify results from file
  private def verifyResults(file: File): Lazy[Map[String, Int]] = {
    Lazy
      .using(Source.fromFile(file)) { source =>
        val lines = source.getLines().toList
        Map(
          "totalLines"  -> lines.size,
          "avgLength"   -> (if (lines.isEmpty) 0
                            else lines.map(_.length).sum / lines.size),
          "longestLine" -> lines.map(_.length).maxOption.getOrElse(0)
        )
      }
      .recover { error =>
        Lazy.println(s"  Verification failed: ${error.getMessage}")
          .as(Map.empty[String, Int])
      }
  }

  // Cleanup temporary file
  private def cleanup(file: File): Lazy[Unit] = Lazy.fn {
    if (file.exists()) {
      file.delete()
    }
  }
}

/** An alternative example showing error handling and retries.
  */
object LazyAppWithRetryExample extends LazyApp {

  override def run: Lazy[Any] = {
    for {
      _      <- Lazy.println("=== Lazy App with Retry Example ===\n")
      // Simulate an unreliable operation
      result <- unreliableOperation()
                  .retryN(3)
                  .recover { error =>
                    Lazy.println(s"All retries failed: ${error.getMessage}")
                      .as("fallback-result")
                  }
      _      <- Lazy.println(s"\nFinal result: $result")
      _      <- Lazy.println("\n=== Example Complete ===")
    } yield ()
  }

  private var attempt = 0

  private def unreliableOperation(): Lazy[String] = Lazy.fn {
    attempt += 1
    println(s"Attempt $attempt...")

    if (attempt < 3) {
      throw new Exception("Operation failed!")
    }

    "success"
  }
}

/** An example showing conditional execution and branching.
  */
object LazyAppConditionalExample extends LazyApp {

  override def run: Lazy[Any] = {
    for {
      _      <- Lazy.println("=== Lazy App Conditional Example ===\n")
      // Get a random number
      number <- Lazy.fn(Random.nextInt(100))
      _      <- Lazy.println(s"Generated number: $number\n")

      // Conditional execution
      result <- Lazy.when(number > 50) {
                  Lazy.println("  Number is greater than 50")
                    .flatMap(_ => processLargeNumber(number))
                }.map {
                  case Some(result) => s"Large number result: $result"
                  case None         => "Number was not large enough"
                }

      _ <- Lazy.println(s"\n$result")
      _ <- Lazy.println("\n=== Example Complete ===")
    } yield ()
  }

  private def processLargeNumber(n: Int): Lazy[String] = Lazy.fn {
    s"Processed: ${n * 10}"
  }
}
