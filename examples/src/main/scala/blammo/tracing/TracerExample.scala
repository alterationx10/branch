package blammo.tracing

import dev.alteration.branch.blammo.{JsonConsoleLogger, Tracer}
import dev.alteration.branch.friday.Json
import scala.util.{Success, Failure}

/** An example showing distributed tracing via structured logging.
  *
  * The Tracer trait provides distributed tracing capabilities by logging
  * structured span events that can be collected and visualized by log
  * aggregators like:
  * - Grafana Loki
  * - AWS CloudWatch
  * - OpenSearch/Elasticsearch
  * - Splunk
  *
  * Key concepts:
  * - Trace: A complete request flow (has a unique traceId)
  * - Span: A single operation within a trace (has a unique spanId)
  * - Parent Span: Spans can be nested to show call hierarchies
  *
  * The tracer automatically logs:
  * - span.start: When an operation begins
  * - span.end: When an operation completes (with duration and success status)
  * - span.event: Additional events that occur within a span
  *
  * To run this example:
  * sbt "examples/runMain blammo.tracing.TracerExample"
  *
  * Try piping the output to jq for pretty printing:
  * sbt "examples/runMain blammo.tracing.TracerExample" 2>&1 | grep jsonMessage | jq
  * '.jsonMessage'
  */
object TracerExample extends App with JsonConsoleLogger with Tracer {

  println("=== Distributed Tracing Example ===")
  println("(Trace spans will be logged as JSON below)\n")

  // Example 1: Simple traced operation
  simpleExample()

  println("\n--- Waiting between examples ---\n")
  Thread.sleep(1000)

  // Example 2: Nested traced operations
  nestedExample()

  println("\n--- Waiting between examples ---\n")
  Thread.sleep(1000)

  // Example 3: Traced operation with Try
  errorHandlingExample()

  println("\n=== Example Complete ===")

  /** Simple example: trace a single operation */
  def simpleExample(): Unit = {
    traced("simple.operation") {
      println("[App] Performing a simple operation...")
      Thread.sleep(100)
      println("[App] Simple operation complete")
    }
  }

  /** Nested example: trace operations that call other operations */
  def nestedExample(): Unit = {
    traced("http.request", Map("endpoint" -> Json.JsonString("/api/users"))) {
      println("[App] Handling HTTP request...")

      traced("db.query", Map("table" -> Json.JsonString("users"))) {
        println("[App] Querying database...")
        Thread.sleep(50)
        spanEvent("query.executed", Map("rows" -> Json.JsonNumber(42)))
        println("[App] Database query complete")
      }

      traced("cache.write", Map("key" -> Json.JsonString("users:123"))) {
        println("[App] Writing to cache...")
        Thread.sleep(30)
        println("[App] Cache write complete")
      }

      traced("response.serialize") {
        println("[App] Serializing response...")
        Thread.sleep(20)
        println("[App] Serialization complete")
      }

      println("[App] HTTP request complete")
    }
  }

  /** Example showing error handling with tracing */
  def errorHandlingExample(): Unit = {
    println("[App] Demonstrating error handling with tracing...\n")

    // Example with successful operation
    val successResult = tracedTry("payment.process") {
      println("[App] Processing payment...")
      Thread.sleep(75)
      println("[App] Payment processed successfully")
      "payment-123"
    }

    successResult match {
      case Success(id) => println(s"[App] Success! Payment ID: $id")
      case Failure(ex) => println(s"[App] Failed: ${ex.getMessage}")
    }

    Thread.sleep(500)

    // Example with failing operation
    val failureResult = tracedTry(
      "payment.process",
      Map("amount" -> Json.JsonNumber(99.99), "currency" -> Json.JsonString("USD"))
    ) {
      println("[App] Processing payment...")
      Thread.sleep(50)
      throw new RuntimeException("Insufficient funds")
    }

    failureResult match {
      case Success(id) => println(s"[App] Success! Payment ID: $id")
      case Failure(ex) => println(s"[App] Failed: ${ex.getMessage}")
    }
  }
}
