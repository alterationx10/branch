package spider.server

import dev.alteration.branch.spider.server.{
  SpiderApp,
  RequestHandler,
  Request,
  Response,
  StreamingResponse,
  ServerSentEvents
}
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*

/** Example demonstrating the various response builders:
  * - Redirect helpers (301, 302, 307, 308)
  * - Error response builders (4xx, 5xx)
  * - Streaming response support
  * - SSE (Server-Sent Events) support
  *
  * Test with:
  *   # Redirects
  *   curl -i http://localhost:9000/old-page
  *   curl -i http://localhost:9000/temp-redirect
  *
  *   # Error responses
  *   curl -i http://localhost:9000/errors/bad-request
  *   curl -i http://localhost:9000/errors/unauthorized
  *   curl -i http://localhost:9000/errors/forbidden
  *   curl -i http://localhost:9000/errors/not-found
  *   curl -i http://localhost:9000/errors/server-error
  *
  *   # Streaming
  *   curl http://localhost:9000/stream
  *
  *   # SSE (keeps connection open)
  *   curl http://localhost:9000/events
  */
object ResponseBuildersExample extends SpiderApp {

  // === Redirect Examples ===

  val permanentRedirectHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.movedPermanently("/new-location")
    }
  }

  val temporaryRedirectHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.found("/temporary-location")
    }
  }

  val redirect307Handler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.temporaryRedirect("/temp-preserve-method")
    }
  }

  val redirect308Handler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.permanentRedirect("/perm-preserve-method")
    }
  }

  // === Error Response Examples ===

  val badRequestHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.badRequest("Missing required parameters")
    }
  }

  val unauthorizedHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.unauthorized("Please provide valid credentials")
    }
  }

  val forbiddenHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.forbidden("You don't have permission to access this resource")
    }
  }

  val notFoundHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.notFound("The requested resource was not found")
    }
  }

  val conflictHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.conflict("Resource already exists")
    }
  }

  val unprocessableHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.unprocessableEntity("Invalid data format")
    }
  }

  val rateLimitHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.tooManyRequests("Rate limit exceeded", retryAfter = Some(60))
    }
  }

  val serverErrorHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.internalServerError("Something went wrong on our end")
    }
  }

  val notImplementedHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.notImplemented("This feature is not yet implemented")
    }
  }

  val serviceUnavailableHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response.serviceUnavailable(
        "Service temporarily unavailable",
        retryAfter = Some(120)
      )
    }
  }

  // === Streaming Response Example ===

  val streamHandler = new RequestHandler[Unit, StreamingResponse] {
    override def handle(request: Request[Unit]): Response[StreamingResponse] = {
      val stream = StreamingResponse.create { writer =>
        writer.writeLine("Starting streaming response...")
        writer.writeLine("")

        // Simulate streaming data
        for (i <- 1 to 10) {
          writer.writeLine(s"Chunk $i of 10")
          Thread.sleep(100) // Simulate processing time
        }

        writer.writeLine("")
        writer.writeLine("Streaming complete!")
      }

      Response(
        statusCode = 200,
        body = stream,
        headers = Map("Content-Type" -> List("text/plain"))
      )
    }
  }

  // === Server-Sent Events (SSE) Example ===

  val sseHandler = new RequestHandler[Unit, StreamingResponse] {
    override def handle(request: Request[Unit]): Response[StreamingResponse] = {
      val stream = StreamingResponse.create { writer =>
        val sse = ServerSentEvents(writer)

        // Send initial connection event
        sse.sendEvent("Connected to SSE stream", eventType = Some("connection"))

        // Send periodic updates
        for (i <- 1 to 10) {
          sse.sendEvent(
            s"""{"count": $i, "timestamp": ${System.currentTimeMillis()}}""",
            eventType = Some("update"),
            id = Some(i.toString)
          )
          Thread.sleep(500)
        }

        // Send completion event
        sse.sendEvent("Stream complete", eventType = Some("complete"))
      }

      Response(
        statusCode = 200,
        body = stream,
        headers = Map(
          "Content-Type" -> List("text/event-stream"),
          "Cache-Control" -> List("no-cache"),
          "Connection" -> List("keep-alive")
        )
      )
    }
  }

  // === Home Page ===

  val homeHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      html"""
      <!DOCTYPE html>
      <html>
      <head>
        <title>Response Builders Example</title>
        <style>
          body { font-family: sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; }
          h1 { color: #333; }
          h2 { color: #666; margin-top: 30px; }
          .section { margin: 20px 0; }
          a { color: #0066cc; text-decoration: none; }
          a:hover { text-decoration: underline; }
          ul { line-height: 1.8; }
          code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
        </style>
      </head>
      <body>
        <h1>Response Builders Example</h1>
        <p>This example demonstrates various HTTP response builders including redirects, errors, streaming, and SSE.</p>

        <div class="section">
          <h2>Redirects</h2>
          <ul>
            <li><a href="/old-page">/old-page</a> - 301 Moved Permanently</li>
            <li><a href="/temp-redirect">/temp-redirect</a> - 302 Found</li>
            <li><a href="/redirect-307">/redirect-307</a> - 307 Temporary Redirect</li>
            <li><a href="/redirect-308">/redirect-308</a> - 308 Permanent Redirect</li>
          </ul>
        </div>

        <div class="section">
          <h2>Error Responses</h2>
          <ul>
            <li><a href="/errors/bad-request">/errors/bad-request</a> - 400</li>
            <li><a href="/errors/unauthorized">/errors/unauthorized</a> - 401</li>
            <li><a href="/errors/forbidden">/errors/forbidden</a> - 403</li>
            <li><a href="/errors/not-found">/errors/not-found</a> - 404</li>
            <li><a href="/errors/conflict">/errors/conflict</a> - 409</li>
            <li><a href="/errors/unprocessable">/errors/unprocessable</a> - 422</li>
            <li><a href="/errors/rate-limit">/errors/rate-limit</a> - 429</li>
            <li><a href="/errors/server-error">/errors/server-error</a> - 500</li>
            <li><a href="/errors/not-implemented">/errors/not-implemented</a> - 501</li>
            <li><a href="/errors/service-unavailable">/errors/service-unavailable</a> - 503</li>
          </ul>
        </div>

        <div class="section">
          <h2>Streaming</h2>
          <ul>
            <li><a href="/stream">/stream</a> - Streaming text response</li>
            <li><a href="/events">/events</a> - Server-Sent Events (use curl or browser EventSource)</li>
          </ul>
          <p>For SSE, try this JavaScript in your browser console:</p>
          <code>
            const es = new EventSource('/events');<br>
            es.onmessage = e => console.log(e.data);
          </code>
        </div>
      </body>
      </html>
      """
    }
  }

  // === Router ===

  override val router
      : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    // Home
    case (HttpMethod.GET, Nil) => homeHandler

    // Redirects
    case (HttpMethod.GET, "old-page" :: Nil)      => permanentRedirectHandler
    case (HttpMethod.GET, "temp-redirect" :: Nil) => temporaryRedirectHandler
    case (HttpMethod.GET, "redirect-307" :: Nil)  => redirect307Handler
    case (HttpMethod.GET, "redirect-308" :: Nil)  => redirect308Handler

    // Errors
    case (HttpMethod.GET, "errors" :: "bad-request" :: Nil) => badRequestHandler
    case (HttpMethod.GET, "errors" :: "unauthorized" :: Nil) =>
      unauthorizedHandler
    case (HttpMethod.GET, "errors" :: "forbidden" :: Nil) => forbiddenHandler
    case (HttpMethod.GET, "errors" :: "not-found" :: Nil) => notFoundHandler
    case (HttpMethod.GET, "errors" :: "conflict" :: Nil)  => conflictHandler
    case (HttpMethod.GET, "errors" :: "unprocessable" :: Nil) =>
      unprocessableHandler
    case (HttpMethod.GET, "errors" :: "rate-limit" :: Nil) => rateLimitHandler
    case (HttpMethod.GET, "errors" :: "server-error" :: Nil) =>
      serverErrorHandler
    case (HttpMethod.GET, "errors" :: "not-implemented" :: Nil) =>
      notImplementedHandler
    case (HttpMethod.GET, "errors" :: "service-unavailable" :: Nil) =>
      serviceUnavailableHandler

    // Streaming
    case (HttpMethod.GET, "stream" :: Nil) => streamHandler
    case (HttpMethod.GET, "events" :: Nil) => sseHandler
  }
}
