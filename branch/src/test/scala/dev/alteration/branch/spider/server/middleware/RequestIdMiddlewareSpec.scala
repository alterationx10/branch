package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import dev.alteration.branch.spider.server.RequestHandler.given
import munit.FunSuite

import java.net.URI
import java.util.UUID

class RequestIdMiddlewareSpec extends FunSuite {

  val testRequest: Request[String] = Request(
    URI.create("http://localhost:9000/test"),
    Map("Content-Type" -> List("text/plain")),
    "test body"
  )

  val baseHandler = new RequestHandler[String, String] {
    def handle(request: Request[String]): Response[String] =
      Response(200, s"Handled: ${request.body}")
  }

  test("RequestIdMiddleware should add request ID to request headers") {
    val middleware                               = RequestIdMiddleware[String, String]()
    var capturedRequest: Option[Request[String]] = None

    val handler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        capturedRequest = Some(request)
        Response(200, "ok")
      }
    }

    val wrappedHandler = middleware(handler)
    wrappedHandler.handle(testRequest)

    assert(capturedRequest.isDefined)
    assert(capturedRequest.get.headers.contains("X-Request-ID"))

    val requestId = capturedRequest.get.headers("X-Request-ID").head
    // Should be a valid UUID
    assert(UUID.fromString(requestId) != null)
  }

  test("RequestIdMiddleware should add request ID to response headers") {
    val middleware     = RequestIdMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response = wrappedHandler.handle(testRequest)

    assert(response.headers.contains("X-Request-ID"))
    val requestId = response.headers("X-Request-ID").head
    // Should be a valid UUID
    assert(UUID.fromString(requestId) != null)
  }

  test("RequestIdMiddleware should use same ID in request and response") {
    val middleware                        = RequestIdMiddleware[String, String]()
    var capturedRequestId: Option[String] = None

    val handler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        capturedRequestId =
          request.headers.get("X-Request-ID").flatMap(_.headOption)
        Response(200, "ok")
      }
    }

    val wrappedHandler = middleware(handler)
    val response       = wrappedHandler.handle(testRequest)

    assert(capturedRequestId.isDefined)
    val responseId = response.headers("X-Request-ID").head
    assertEquals(capturedRequestId.get, responseId)
  }

  test("RequestIdMiddleware should reuse existing request ID") {
    val existingId    = UUID.randomUUID().toString
    val requestWithId = testRequest.copy(
      headers = testRequest.headers + ("X-Request-ID" -> List(existingId))
    )

    val middleware                        = RequestIdMiddleware[String, String]()
    var capturedRequestId: Option[String] = None

    val handler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        capturedRequestId =
          request.headers.get("X-Request-ID").flatMap(_.headOption)
        Response(200, "ok")
      }
    }

    val wrappedHandler = middleware(handler)
    val response       = wrappedHandler.handle(requestWithId)

    // Should use the existing ID, not generate a new one
    assertEquals(capturedRequestId.get, existingId)
    assertEquals(response.headers("X-Request-ID").head, existingId)
  }

  test("RequestIdMiddleware should not modify response body") {
    val middleware     = RequestIdMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response = wrappedHandler.handle(testRequest)

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Handled: test body")
  }

  test("RequestIdMiddleware with custom header names") {
    val middleware = RequestIdMiddleware[String, String](
      requestHeaderName = "X-Custom-Request-ID",
      responseHeaderName = "X-Custom-Response-ID"
    )

    var capturedRequest: Option[Request[String]] = None

    val handler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        capturedRequest = Some(request)
        Response(200, "ok")
      }
    }

    val wrappedHandler = middleware(handler)
    val response       = wrappedHandler.handle(testRequest)

    // Request should have custom header
    assert(capturedRequest.isDefined)
    assert(capturedRequest.get.headers.contains("X-Custom-Request-ID"))

    // Response should have custom header
    assert(response.headers.contains("X-Custom-Response-ID"))

    // IDs should match
    val requestId  = capturedRequest.get.headers("X-Custom-Request-ID").head
    val responseId = response.headers("X-Custom-Response-ID").head
    assertEquals(requestId, responseId)
  }

  test("requestId extension method should extract request ID") {
    val requestId     = UUID.randomUUID().toString
    val requestWithId = testRequest.copy(
      headers = testRequest.headers + ("X-Request-ID" -> List(requestId))
    )

    assertEquals(requestWithId.requestId, Some(requestId))
  }

  test("requestId extension method should return None if no ID") {
    assertEquals(testRequest.requestId, None)
  }

  test("RequestIdMiddleware should work with short-circuit middleware") {
    // When middleware short-circuits during pre-process, postProcess doesn't run
    // So request ID won't be in the response
    val authMiddleware = Middleware.preOnly[String, String] { _ =>
      Respond(Response(401, "Unauthorized"))
    }

    val requestIdMiddleware = RequestIdMiddleware[String, String]()

    val combined       = requestIdMiddleware.andThen(authMiddleware)
    val wrappedHandler = combined(baseHandler)

    val response = wrappedHandler.handle(testRequest)

    // Auth short-circuited, so postProcess didn't run and ID is not in response
    assertEquals(response.statusCode, 401)
    assert(!response.headers.contains("X-Request-ID"))
  }

  test("Multiple requests should get different IDs") {
    val middleware     = RequestIdMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response1 = wrappedHandler.handle(testRequest)
    val response2 = wrappedHandler.handle(testRequest)

    val id1 = response1.headers("X-Request-ID").head
    val id2 = response2.headers("X-Request-ID").head

    // IDs should be different for different requests
    assert(id1 != id2)
  }

  test("RequestIdMiddleware should work in a chain with other middleware") {
    val requestIdMiddleware = RequestIdMiddleware[String, String]()
    val loggingMiddleware   = LoggingMiddleware[String, String]()

    val chained = requestIdMiddleware.andThen(loggingMiddleware)

    var capturedRequest: Option[Request[String]] = None
    val handler                                  = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        capturedRequest = Some(request)
        Response(200, "ok")
      }
    }

    val wrappedHandler = chained(handler)
    val response       = wrappedHandler.handle(testRequest)

    // Request should have ID
    assert(capturedRequest.isDefined)
    assert(capturedRequest.get.headers.contains("X-Request-ID"))

    // Response should have ID
    assert(response.headers.contains("X-Request-ID"))

    // IDs should match
    val requestId  = capturedRequest.get.headers("X-Request-ID").head
    val responseId = response.headers("X-Request-ID").head
    assertEquals(requestId, responseId)
  }

  test("RequestIdMiddleware should handle empty header list edge case") {
    // This tests that we handle the Option and List correctly
    val requestWithEmptyList = testRequest.copy(
      headers = testRequest.headers + ("X-Request-ID" -> List())
    )

    val middleware     = RequestIdMiddleware[String, String]()
    val wrappedHandler = middleware(baseHandler)

    val response = wrappedHandler.handle(requestWithEmptyList)

    // Should generate new ID since the list was empty
    assert(response.headers.contains("X-Request-ID"))
    val requestId = response.headers("X-Request-ID").head
    assert(UUID.fromString(requestId) != null)
  }
}
