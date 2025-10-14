package blammo.logging

import dev.alteration.branch.blammo.JsonConsoleLogger
import dev.alteration.branch.friday.Json

/** A basic example showing how to use JSON structured logging.
  *
  * JsonConsoleLogger provides:
  *   - Structured JSON output for easy parsing by log aggregators
  *   - Environment-aware log levels (DEV=ALL, TEST/STAGING=INFO, PROD=WARNING)
  *   - Standard java.util.logging Logger interface
  *
  * All logs are formatted as JSON with fields:
  *   - name: logger name
  *   - level: log level (INFO, WARNING, SEVERE, etc.)
  *   - time: timestamp
  *   - message: log message
  *   - jsonMessage: parsed JSON if message is valid JSON
  *   - error: exception details if present
  *
  * To run this example: sbt "examples/runMain blammo.logging.LoggingExample"
  *
  * Try different environments: export SCALA_ENV=PROD sbt "examples/runMain
  * blammo.logging.LoggingExample"
  */
object LoggingExample extends JsonConsoleLogger {

  def main(args: Array[Int]): Unit = {

    println("=== JSON Logging Example ===")
    println(s"Current log level: $logLevel")
    println("(Log output will be in JSON format below)\n")

    // Basic logging at different levels
    logger.info("Application started")
    logger.fine("This is a debug message (only visible in DEV)")
    logger.warning("This is a warning message")

    // Structured logging with JSON
    logger.info(
      Json
        .obj(
          "event"  -> Json.JsonString("user.login"),
          "userId" -> Json.JsonNumber(12345),
          "ip"     -> Json.JsonString("192.168.1.1")
        )
        .toJsonString
    )

    // Logging with context
    logger.info(
      Json
        .obj(
          "event"    -> Json.JsonString("db.query"),
          "query"    -> Json.JsonString("SELECT * FROM users"),
          "duration" -> Json.JsonNumber(42.5),
          "rows"     -> Json.JsonNumber(100)
        )
        .toJsonString
    )

    // Simulating an operation that could fail
    try {
      performOperation()
    } catch {
      case ex: Exception =>
        logger.severe(s"Operation failed: ${ex.getMessage}")
      // The JsonFormatter will automatically include the exception details
    }

    // Logging with nested data
    logger.info(
      Json
        .obj(
          "event"    -> Json.JsonString("api.response"),
          "endpoint" -> Json.JsonString("/api/users"),
          "status"   -> Json.JsonNumber(200),
          "metadata" -> Json.obj(
            "responseTime" -> Json.JsonNumber(125.3),
            "cacheHit"     -> Json.JsonBool(true),
            "region"       -> Json.JsonString("us-east-1")
          )
        )
        .toJsonString
    )

    logger.info("Application finished")

    println("\n=== Example Complete ===")

    def performOperation(): Unit = {
      logger.info("Starting risky operation...")
      // Simulating a failure
      throw new RuntimeException("Something went wrong!")
    }
  }

}
