package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import dev.alteration.branch.spider.server.RequestHandler.given
import munit.FunSuite

import java.net.URI

class MiddlewareSpec extends FunSuite {

  val testRequest: Request[String] = Request(
    URI.create("http://localhost:9000/test"),
    Map("Content-Type" -> List("text/plain")),
    "test body"
  )

  val baseHandler = new RequestHandler[String, String] {
    def handle(request: Request[String]): Response[String] =
      Response(200, s"Handled: ${request.body}")
  }

  test("identity middleware should not modify request or response") {
    val middleware = Middleware.identity[String, String]
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Handled: test body")
  }

  test("preOnly middleware should modify request") {
    val middleware = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = "modified body"))
    }
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: modified body")
  }

  test("preOnly middleware can short-circuit with Respond") {
    val middleware = Middleware.preOnly[String, String] { _ =>
      Respond(Response(403, "Forbidden"))
    }
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.statusCode, 403)
    assertEquals(response.body, "Forbidden")
  }

  test("postOnly middleware should modify response") {
    val middleware = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + " + modified")
    }
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body + modified")
  }

  test("custom middleware with both pre and post processing") {
    val middleware = new Middleware[String, String] {
      override def preProcess(
          request: Request[String]
      ): MiddlewareResult[Response[String], Request[String]] =
        Continue(request.copy(body = request.body.toUpperCase))

      override def postProcess(
          request: Request[String],
          response: Response[String]
      ): Response[String] =
        response.copy(body = response.body + "!")
    }
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: TEST BODY!")
  }

  test("andThen should compose middleware in order") {
    val middleware1 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " -> m1"))
    }
    val middleware2 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " -> m2"))
    }

    val composed = middleware1.andThen(middleware2)
    val handler  = composed(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body -> m1 -> m2")
  }

  test("andThen should short-circuit on first Respond") {
    val middleware1 = Middleware.preOnly[String, String] { _ =>
      Respond(Response(401, "Unauthorized"))
    }
    val middleware2 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = "should not run"))
    }

    val composed = middleware1.andThen(middleware2)
    val handler  = composed(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.statusCode, 401)
    assertEquals(response.body, "Unauthorized")
  }

  test(">> operator should compose middleware") {
    val middleware1 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + " + m1")
    }
    val middleware2 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + " + m2")
    }

    val composed = middleware1 >> middleware2
    val handler  = composed(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body + m1 + m2")
  }

  test("Monoid.empty should be identity") {
    import Middleware.given
    val middleware =
      summon[dev.alteration.branch.macaroni.typeclasses.Monoid[Middleware[
        String,
        String
      ]]].empty
    val handler    = middleware(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body")
  }

  test("Monoid.combine should compose middleware") {
    import Middleware.given
    val monoid =
      summon[dev.alteration.branch.macaroni.typeclasses.Monoid[Middleware[
        String,
        String
      ]]]

    val middleware1 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = "modified"))
    }
    val middleware2 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + "!")
    }

    val combined = monoid.combine(middleware1, middleware2)
    val handler  = combined(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: modified!")
  }

  test("|+| operator should combine middleware") {
    import Middleware.given
    val monoid =
      summon[dev.alteration.branch.macaroni.typeclasses.Monoid[Middleware[
        String,
        String
      ]]]

    val middleware1 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = "first"))
    }
    val middleware2 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body.toUpperCase)
    }

    val combined = monoid.combine(middleware1, middleware2)
    val handler  = combined(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "HANDLED: FIRST")
  }

  test("chain should combine multiple middleware") {
    val m1 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " 1"))
    }
    val m2 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " 2"))
    }
    val m3 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + " 3")
    }

    val chained = Middleware.chain(m1, m2, m3)
    val handler = chained(baseHandler)

    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body 1 2 3")
  }

  test("withMiddleware extension method should apply middleware") {
    import Middleware.*

    val middleware = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = "via extension"))
    }

    val handler  = baseHandler.withMiddleware(middleware)
    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: via extension")
  }

  test("withMiddlewares extension method should apply multiple middleware") {
    import Middleware.*

    val m1 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " x"))
    }
    val m2 = Middleware.preOnly[String, String] { req =>
      Continue(req.copy(body = req.body + " y"))
    }
    val m3 = Middleware.postOnly[String, String] { (_, resp) =>
      resp.copy(body = resp.body + " z")
    }

    val handler  = baseHandler.withMiddlewares(m1, m2, m3)
    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: test body x y z")
  }

  test("middleware should preserve request in postProcess") {
    var capturedRequest: Option[Request[String]] = None

    val middleware = new Middleware[String, String] {
      override def postProcess(
          request: Request[String],
          response: Response[String]
      ): Response[String] = {
        capturedRequest = Some(request)
        response
      }
    }

    val handler = middleware(baseHandler)
    handler.handle(testRequest)

    assert(capturedRequest.isDefined)
    assertEquals(capturedRequest.get, testRequest)
  }

  test("multiple middleware should see cumulative request changes") {
    val m1 = new Middleware[String, String] {
      override def preProcess(
          request: Request[String]
      ): MiddlewareResult[Response[String], Request[String]] =
        Continue(
          request.copy(headers = request.headers + ("X-M1" -> List("true")))
        )
    }

    val m2 = new Middleware[String, String] {
      override def preProcess(
          request: Request[String]
      ): MiddlewareResult[Response[String], Request[String]] = {
        // Should see the header added by m1
        if (request.headers.contains("X-M1")) {
          Continue(
            request.copy(headers = request.headers + ("X-M2" -> List("true")))
          )
        } else {
          Respond(Response(500, "M1 header missing"))
        }
      }
    }

    val handler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        val hasM1 = request.headers.contains("X-M1")
        val hasM2 = request.headers.contains("X-M2")
        Response(200, s"M1: $hasM1, M2: $hasM2")
      }
    }

    val composed       = m1.andThen(m2)
    val wrappedHandler = composed(handler)
    val response       = wrappedHandler.handle(testRequest)

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "M1: true, M2: true")
  }

  test("factory method apply should create middleware with both pre and post") {
    val middleware = Middleware[String, String](
      pre = req => Continue(req.copy(body = "pre")),
      post = (_, resp) => resp.copy(body = resp.body + " post")
    )

    val handler  = middleware(baseHandler)
    val response = handler.handle(testRequest)

    assertEquals(response.body, "Handled: pre post")
  }
}
