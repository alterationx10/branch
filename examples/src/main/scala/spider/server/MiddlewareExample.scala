package spider.server

import dev.alteration.branch.spider.common.HttpMethod.*
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.middleware.*
import dev.alteration.branch.spider.server.Response.*

/** Example demonstrating middleware usage with the Spider HTTP server.
  *
  * This example shows:
  *   - Request ID injection (adds unique ID to each request/response)
  *   - Timing middleware (logs request duration)
  *   - Logging middleware (logs request/response details)
  *   - Middleware composition (chaining multiple middleware)
  *   - Short-circuit responses (authentication example)
  */
object MiddlewareExample {
  import RequestHandler.given
  import Middleware.{given, *}
  import dev.alteration.branch.spider.common.HttpMethod

  def main(args: Array[String]): Unit = {

    // Example 1: Simple handler with timing middleware
    val simpleHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        Thread.sleep(100) // Simulate some work
        html"""
          <h1>Simple Handler</h1>
          <p>This handler has timing middleware applied.</p>
        """
      }
    }.withMiddleware(TimingMiddleware())

    // Example 2: Handler with multiple middleware chained
    val chainedHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        Thread.sleep(50)
        val requestId = request.requestId.getOrElse("unknown")
        html"""
          <h1>Chained Middleware</h1>
          <p>Request ID: $requestId</p>
          <p>This handler has Request ID + Timing + Logging middleware.</p>
        """
      }
    }.withMiddlewares(
      RequestIdMiddleware(),
      TimingMiddleware(),
      LoggingMiddleware()
    )

    // Example 3: Custom middleware with short-circuit (simple auth)
    val authMiddleware = new Middleware[String, String] {
      override def preProcess(
          request: Request[String]
      ): MiddlewareResult[Response[String], Request[String]] = {
        val authHeader = request.headers.get("Authorization").flatMap(_.headOption)

        authHeader match {
          case Some(token) if token == "Bearer secret-token" =>
            Continue(request)
          case _ =>
            Respond(
              Response(
                statusCode = 401,
                body = """{"error": "Unauthorized"}""",
                headers = Map("Content-Type" -> List("application/json"))
              )
            )
        }
      }
    }

    val protectedHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        html"""
          <h1>Protected Resource</h1>
          <p>You are authenticated!</p>
        """
      }
    }.withMiddlewares(
      authMiddleware,
      RequestIdMiddleware(),
      LoggingMiddleware()
    )

    // Example 4: Custom middleware using Monoid composition
    val combinedMiddleware =
      RequestIdMiddleware[String, String]() |+|
      TimingMiddleware[String, String]() |+|
      LoggingMiddleware[String, String]()


    val composedHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        html"""
          <h1>Composed Middleware</h1>
          <p>Using Monoid |+| operator for composition</p>
        """
      }
    }.withMiddleware(combinedMiddleware)

    // Example 5: Custom pre-only and post-only middleware
    val headerInjector = Middleware.postOnly[String, String] { (req, resp) =>
      resp.withHeader("X-Powered-By" -> "Spider Server")
          .withHeader("X-Example" -> "true")
    }

    val queryLogger = Middleware.preOnly[String, String] { req =>
      val query = Option(req.uri.getQuery).getOrElse("none")
      println(s"Query params: $query")
      Continue(req)
    }

    val customHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        html"""
          <h1>Custom Middleware</h1>
          <p>Check response headers for X-Powered-By and X-Example</p>
        """
      }
    }.withMiddlewares(queryLogger, headerInjector)

    // Create the server with all handlers
    val server = new SpiderApp {
      override val port: Int = 9000

      override val router: PartialFunction[
        (HttpMethod, List[String]),
        RequestHandler[?, ?]
      ] = {
        case (GET, Nil) =>
          new RequestHandler[String, String] {
            def handle(request: Request[String]): Response[String] = {
              html"""
                <h1>Middleware Examples</h1>
                <ul>
                  <li><a href="/simple">Simple (timing)</a></li>
                  <li><a href="/chained">Chained (ID + timing + logging)</a></li>
                  <li><a href="/protected">Protected (auth required)</a></li>
                  <li><a href="/composed">Composed (Monoid |+|)</a></li>
                  <li><a href="/custom">Custom (pre/post only)</a></li>
                </ul>
                <p>For protected endpoint, send: <code>Authorization: Bearer secret-token</code></p>
              """
            }
          }

        case (GET, "simple" :: Nil)    => simpleHandler
        case (GET, "chained" :: Nil)   => chainedHandler
        case (GET, "protected" :: Nil) => protectedHandler
        case (GET, "composed" :: Nil)  => composedHandler
        case (GET, "custom" :: Nil)    => customHandler
      }
    }

    println("Starting middleware example server on http://localhost:9000")
    println("Try these endpoints:")
    println("  GET http://localhost:9000/simple")
    println("  GET http://localhost:9000/chained")
    println("  GET http://localhost:9000/protected")
    println("  GET http://localhost:9000/composed")
    println("  GET http://localhost:9000/custom")
    println("\nFor protected endpoint, use:")
    println("  curl -H 'Authorization: Bearer secret-token' http://localhost:9000/protected")
    println("\nPress Ctrl+C to stop")

    server.main(args)
  }
}
