package lzy.async

import dev.alteration.branch.lzy.Lazy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import java.io.{File, PrintWriter}

/** Examples demonstrating safe resource management with Lazy.
  *
  * The Lazy type integrates with scala.util.Using for automatic resource
  * management:
  *   - using: Safely manage a single resource
  *   - usingManager: Manage multiple resources with automatic cleanup
  *   - managed: Acquire resources within a manager context
  *
  * Resources are automatically closed even if errors occur.
  *
  * To run this example: sbt "examples/runMain lzy.async.ResourceExample"
  */
object ResourceExample {

  def main(args: Array[String]): Unit = {
    println("=== Lazy Resource Management Example ===\n")

    // Create a temporary file for examples
    val tempFile = File.createTempFile("lazy-example-", ".txt")
    tempFile.deleteOnExit()

    // Example 1: Basic resource usage
    println("--- Example 1: Basic Resource Usage ---")
    val writeResult = Lazy.using(new PrintWriter(tempFile)) { writer =>
      println("  Writing to file...")
      writer.println("Line 1")
      writer.println("Line 2")
      writer.println("Line 3")
      "Written successfully"
    }
    println(s"Result: ${writeResult.runSync().get}")
    println()

    // Example 2: Reading from a file
    println("--- Example 2: Reading from File ---")
    val readResult = Lazy.using(Source.fromFile(tempFile)) { source =>
      println("  Reading from file...")
      val lines = source.getLines().toList
      println(s"  Read ${lines.size} lines")
      lines
    }
    println(s"Result: ${readResult.runSync().get}")
    println()

    // Example 3: Error handling with resources
    println("--- Example 3: Error Handling with Resources ---")
    val errorResult = Lazy
      .using(Source.fromFile(tempFile)) { source =>
        println("  Processing file...")
        throw new RuntimeException("Simulated error")
        source.getLines().toList
      }
      .recover { error =>
        println(s"  Caught error: ${error.getMessage}")
        println("  Resource was still properly closed!")
        Lazy.fn(List.empty[String])
      }
    println(s"Result: ${errorResult.runSync().get}")
    println()

    // Example 4: Managing multiple resources
    println("--- Example 4: Managing Multiple Resources ---")
    val tempFile2 = File.createTempFile("lazy-example-2-", ".txt")
    tempFile2.deleteOnExit()

    // Write to second file
    Lazy.using(new PrintWriter(tempFile2)) { writer =>
      writer.println("Source file content")
    }.runSync()

    val copyResult = Lazy.usingManager { implicit manager =>
      // Both resources are managed and will be closed together
      val sourceRes = Lazy.managed(Source.fromFile(tempFile2)).runSync().get
      val targetRes = Lazy.managed(new PrintWriter(tempFile)).runSync().get

      println("  Copying from source to target...")
      sourceRes.getLines().foreach { line =>
        targetRes.println(s"Copied: $line")
      }
      "Copy completed"
    }
    println(s"Result: ${copyResult.runSync().get}")

    // Verify the copy
    val verifyResult = Lazy.using(Source.fromFile(tempFile)) { source =>
      source.getLines().toList
    }
    println(s"  Verification: ${verifyResult.runSync().get}")
    println()

    // Example 5: Chaining resource operations
    println("--- Example 5: Chaining Resource Operations ---")
    val chainedResult = for {
      // Write some data
      _     <- Lazy.using(new PrintWriter(tempFile)) { writer =>
                 writer.println("Step 1")
                 writer.println("Step 2")
               }
      // Read it back
      lines <- Lazy.using(Source.fromFile(tempFile)) { source =>
                 source.getLines().toList
               }
      // Process the lines
      processed <- Lazy.fn {
                     lines.map(_.toUpperCase)
                   }
    } yield processed

    println(s"Result: ${chainedResult.runSync().get}")
    println()

    // Example 6: Resource with retry
    println("--- Example 6: Resource with Retry ---")
    var attempts = 0
    val retryResult = Lazy
      .using(Source.fromFile(tempFile)) { source =>
        attempts += 1
        println(s"  Attempt $attempts")
        if (attempts < 2) throw new Exception("Retry me")
        source.getLines().toList.size
      }
      .retryN(3)

    println(s"Result (line count): ${retryResult.runSync().get}")
    println()

    // Example 7: Conditional resource usage
    println("--- Example 7: Conditional Resource Usage ---")
    def readFileIfExists(file: File): Lazy[Option[List[String]]] = {
      Lazy.when(file.exists()) {
        Lazy.using(Source.fromFile(file)) { source =>
          source.getLines().toList
        }
      }
    }

    val existingFile = readFileIfExists(tempFile)
    println(s"Existing file: ${existingFile.runSync().get.map(_.size)} lines")

    val nonExistentFile = readFileIfExists(new File("nonexistent.txt"))
    println(s"Non-existent file: ${nonExistentFile.runSync().get}")
    println()

    // Cleanup
    tempFile.delete()
    tempFile2.delete()

    println("=== Example Complete ===")
  }
}
