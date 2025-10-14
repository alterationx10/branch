package spider.server

import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.HttpMethod.GET
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.Response.{html, json}

/** A simple HTTP server example using SpiderApp.
  *
  * This example demonstrates:
  * - Creating a basic HTTP server
  * - Handling different routes
  * - Using RequestHandler with String requests/responses
  * - Returning JSON and HTML responses
  *
  * Run this and visit:
  * - http://localhost:8080/
  * - http://localhost:8080/hello
  * - http://localhost:8080/api/status
  */
object HelloWorldServer {

  import RequestHandler.given 
  
  def main(args: Array[String]): Unit = {
    // Create handlers for different routes
    val homeHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>Hello World Server</title>
          <style>
            body { font-family: sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
            h1 { color: #667eea; }
            a { color: #667eea; text-decoration: none; }
            a:hover { text-decoration: underline; }
            .link-list { margin-top: 20px; }
            .link-list a { display: block; margin: 10px 0; }
          </style>
        </head>
        <body>
          <h1>Welcome to Spider HTTP Server!</h1>
          <p>This is a simple HTTP server built with Branch Spider.</p>
          <div class="link-list">
            <a href="/hello">Visit /hello</a>
            <a href="/api/status">Visit /api/status (JSON)</a>
          </div>
        </body>
        </html>
        """
      }
    }

    val helloHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        // Extract query parameters from URI
        val query = Option(request.uri.getQuery).getOrElse("")
        val name = if (query.startsWith("name=")) {
          query.stripPrefix("name=")
        } else {
          "World"
        }

        html"""
        <!DOCTYPE html>
        <html>
        <head>
          <title>Hello</title>
          <style>
            body { font-family: sans-serif; text-align: center; margin-top: 100px; }
            h1 { color: #48bb78; font-size: 3rem; }
          </style>
        </head>
        <body>
          <h1>Hello, $name!</h1>
          <p>Try: <a href="/hello?name=Alice">/hello?name=Alice</a></p>
          <p><a href="/">Back to home</a></p>
        </body>
        </html>
        """
      }
    }

    val statusHandler = new RequestHandler[String, String] {
      def handle(request: Request[String]): Response[String] = {
        val uptime = java.lang.management.ManagementFactory.getRuntimeMXBean.getUptime
        json"""
        {
          "status": "ok",
          "message": "Server is running",
          "uptime_ms": $uptime,
          "timestamp": ${System.currentTimeMillis()}
        }
        """
      }
    }

    val mainRouter = new ContextHandler("/") {
      /** A partial function to route requests to specific handlers.
       */
      override val contextRouter: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
        case (GET, Nil) => homeHandler
        case (GET, "hello" :: Nil) => helloHandler
        case (GET, "api" :: "status" :: Nil) => statusHandler
      }
    }
    
    // Create server with SpiderApp trait
    // Normally your "app" object would extend this
    val appServer = new SpiderApp {
      override val port = 8080
      ContextHandler.registerHandler(mainRouter)
    }

    println()
    println("Server started on port 8080")
    println("Visit:")
    println("  - http://localhost:8080/")
    println("  - http://localhost:8080/hello")
    println("  - http://localhost:8080/api/status")
    println()
    println("Press Ctrl+C to stop")
    println()

    // Start the server
    appServer.main(Array.empty)
  }
}
