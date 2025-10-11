package dev.alteration.branch.spider.webview.examples

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.HybridServer
import dev.alteration.branch.spider.http.ResourceServer
import dev.alteration.branch.spider.webview.*
import dev.alteration.branch.macaroni.runtimes.BranchExecutors

import scala.concurrent.ExecutionContext

/** A complete example showing how to serve a WebView with HTML and JavaScript.
  *
  * This example demonstrates:
  * 1. Setting up a HybridServer to handle both HTTP and WebSocket
  * 2. Serving the initial HTML page
  * 3. Serving the webview.js from resources
  * 4. Connecting the WebView via WebSocket
  *
  * Run this and visit: http://localhost:8080/
  */
object CounterExample extends App {

  given ExecutionContext = BranchExecutors.executionContext

  // Create actor system for WebView
  val actorSystem = ActorSystem()

  // Configure the port and paths
  val port = 8080
  val host = "localhost"
  val wsPath = "/ws/counter"
  val jsPath = "/js/webview.js"

  // Create the WebView handler
  given EventCodec[CounterEvent] = EventCodec.derived
  val counterHandler = WebViewHandler[CounterState, CounterEvent](
    actorSystem = actorSystem,
    webViewFactory = () => new CounterWebView(),
    params = Map.empty,
    session = Session()
  )

  // Create the page handler that serves the initial HTML
  val pageHandler = WebViewPageHandler(
    wsUrl = s"ws://$host:$port$wsPath",
    title = "Counter Demo",
    jsPath = jsPath,
    debug = true // Enable debug logging in the browser
  )

  // Create a resource server to serve the webview.js file
  // This will serve files from src/main/resources/spider/webview/
  val resourceServer = ResourceServer("spider/webview")

  // Create the hybrid server with both HTTP and WebSocket routes
  val server = HybridServer(
    port = port,
    httpRoutes = Map(
      "/" -> pageHandler,           // Serve the HTML page at root
      jsPath -> resourceServer       // Serve JS from resources
    ),
    wsRoutes = Map(
      wsPath -> counterHandler       // WebSocket endpoint for WebView
    )
  )

  // Add shutdown hook
  Runtime.getRuntime.addShutdownHook {
    new Thread {
      override def run(): Unit = {
        println("\nShutting down...")
        server.stop()
        actorSystem.shutdownAwait()
      }
    }
  }

  // Print instructions
  println("=" * 60)
  println("Branch WebView - Counter Example")
  println("=" * 60)
  println()
  println(s"Server running at: http://$host:$port/")
  println(s"WebSocket at: ws://$host:$port$wsPath")
  println()
  println("Open your browser to http://localhost:8080/ to see the counter")
  println("Press Ctrl+C to stop")
  println("=" * 60)
  println()

  // Start the server (blocks)
  server.start()
}
