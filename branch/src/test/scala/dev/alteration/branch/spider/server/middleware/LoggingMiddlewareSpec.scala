package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import dev.alteration.branch.spider.server.RequestHandler.given
import munit.FunSuite

import java.net.URI
import java.util.logging.{Level, LogRecord}
import scala.collection.mutable.ListBuffer

class LoggingMiddlewareSpec extends FunSuite {

  val testRequest: Request[String] = Request(
    URI.create("http://localhost:9000/test"),
    Map("Content-Type" -> List("text/plain")),
    "test body"
  )

  val baseHandler = new RequestHandler[String, String] {
    def handle(request: Request[String]): Response[String] =
      Response(200, s"Handled: ${request.body}")
  }

  // Helper to capture log records
  class TestHandler extends java.util.logging.Handler {
    val records = ListBuffer[LogRecord]()

    override def publish(record: LogRecord): Unit = {
      records += record
    }

    override def flush(): Unit = ()
    override def close(): Unit = ()
  }

  test("LoggingMiddleware should log request entry") {
    val testHandler = new TestHandler()
    val middleware  = new LoggingMiddleware[String, String] {
      override val handler = testHandler
    }

    val wrappedHandler = middleware(baseHandler)
    wrappedHandler.handle(testRequest)

    val logMessages = testHandler.records.map(_.getMessage).toList
    assert(logMessages.exists(_.contains("/test")))
    assert(logMessages.exists(_.contains("→")))
  }

  test("LoggingMiddleware should log response exit") {
    val testHandler = new TestHandler()
    val middleware  = new LoggingMiddleware[String, String] {
      override val handler = testHandler
    }

    val wrappedHandler = middleware(baseHandler)
    wrappedHandler.handle(testRequest)

    val logMessages = testHandler.records.map(_.getMessage).toList
    assert(logMessages.exists(_.contains("/test")))
    assert(logMessages.exists(_.contains("←")))
    assert(logMessages.exists(_.contains("200")))
  }

  test("LoggingMiddleware should not modify request or response") {
    val middleware     = LoggingMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response = wrappedHandler.handle(testRequest)

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Handled: test body")
  }

  test("TimingMiddleware should measure request duration") {
    val testHandler = new TestHandler()
    val middleware  = new TimingMiddleware[String, String] {
      override val handler = testHandler
    }

    val handlerWithDelay = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        Thread.sleep(10) // Add some delay
        Response(200, "ok")
      }
    }

    val wrappedHandler = middleware(handlerWithDelay)
    wrappedHandler.handle(testRequest)

    val logMessages = testHandler.records.map(_.getMessage).toList
    // JSON logs contain timing field - find the one with actual timing value (not null)
    val timingLog   = logMessages.find(msg =>
      msg.contains("timing") && msg.contains("statusCode") && !msg.contains(
        "null"
      )
    )

    assert(
      timingLog.isDefined,
      s"Should have a timing log entry. Got: $logMessages"
    )
    assert(timingLog.get.contains("/test") || timingLog.get.contains("test"))
    assert(timingLog.get.contains("200"))
  }

  test("TimingMiddleware should log entry and exit") {
    val testHandler = new TestHandler()
    val middleware  = new TimingMiddleware[String, String] {
      override val handler = testHandler
    }

    val wrappedHandler = middleware(baseHandler)
    wrappedHandler.handle(testRequest)

    val logMessages = testHandler.records.map(_.getMessage).toList

    // Should have entry log (JSON format with direction field)
    assert(logMessages.exists(msg => msg.contains("→") && msg.contains("test")))

    // Should have exit log with timing (JSON format)
    assert(
      logMessages.exists(msg =>
        msg.contains("←") && msg.contains("200") && msg.contains("timing")
      )
    )
  }

  test("TimingMiddleware should not modify request or response") {
    val middleware     = TimingMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response = wrappedHandler.handle(testRequest)

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Handled: test body")
  }

  test("TimingMiddleware with custom log level") {
    val testHandler = new TestHandler()
    testHandler.setLevel(Level.WARNING)

    val middleware = new TimingMiddleware[String, String] {
      override val handler  = testHandler
      override val logLevel = Level.WARNING
    }

    val wrappedHandler = middleware(baseHandler)
    wrappedHandler.handle(testRequest)

    // At WARNING level, INFO logs should not appear
    // But the test handler captures all, so we check the level
    testHandler.records.foreach { record =>
      assert(record.getLevel.intValue >= Level.WARNING.intValue)
    }
  }

  test("LoggingMiddleware should log headers") {
    val testHandler = new TestHandler()
    val middleware  = new LoggingMiddleware[String, String] {
      override val handler = testHandler
    }

    val requestWithHeaders = testRequest.copy(
      headers = Map(
        "Content-Type"  -> List("application/json"),
        "Authorization" -> List("Bearer token")
      )
    )

    val wrappedHandler = middleware(baseHandler)
    wrappedHandler.handle(requestWithHeaders)

    val logMessages = testHandler.records.map(_.getMessage).toList
    val entryLog    = logMessages.find(_.contains("→"))

    assert(entryLog.isDefined)
    assert(
      entryLog.get.contains("Content-Type") || entryLog.get.contains(
        "Authorization"
      )
    )
  }

  test("Multiple logging middleware should both log") {
    val testHandler1 = new TestHandler()
    val testHandler2 = new TestHandler()

    val middleware1 = new LoggingMiddleware[String, String] {
      override val handler = testHandler1
    }

    val middleware2 = new LoggingMiddleware[String, String] {
      override val handler = testHandler2
    }

    val wrappedHandler = middleware1.andThen(middleware2)(baseHandler)
    wrappedHandler.handle(testRequest)

    // Both handlers should have captured logs
    assert(testHandler1.records.nonEmpty)
    assert(testHandler2.records.nonEmpty)
  }
}
