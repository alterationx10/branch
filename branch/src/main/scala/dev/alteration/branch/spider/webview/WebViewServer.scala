package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.{WebSocketServer, WebSocketHandler}
import scala.concurrent.ExecutionContext.Implicits.global

/** Fluent builder API for setting up Branch WebView servers.
  *
  * This provides an ergonomic way to configure and start WebView servers
  * without manually creating ActorSystems, handlers, and routing.
  *
  * == Quick Start ==
  *
  * {{{
  * import dev.alteration.branch.spider.webview._
  *
  * val server = WebViewServer()
  *   .withRoute("/counter", new CounterWebView())
  *   .withRoute("/todos", new TodoListWebView())
  *   .start(port = 8080)
  *
  * // Later...
  * server.stop()
  * }}}
  *
  * == Advanced Usage ==
  *
  * {{{
  * val customActorSystem = ActorSystem()
  *
  * val server = WebViewServer(customActorSystem)
  *   .withRoute("/counter", new CounterWebView(),
  *     params = Map("initial" -> "10"),
  *     session = Session(Map("userId" -> user.id)))
  *   .withRoute("/admin", new AdminWebView())
  *   .start(port = 8080)
  * }}}
  *
  * @param actorSystem
  *   The actor system to use (or create a default one)
  * @param routes
  *   Map of paths to route configurations
  */
case class WebViewServer(
    actorSystem: ActorSystem = ActorSystem(),
    routes: Map[String, WebViewRoute[?]] = Map.empty
) {

  /** Add a WebView route to the server.
    *
    * @param path
    *   The URL path for this WebView (e.g., "/counter")
    * @param webView
    *   The WebView instance to serve
    * @param params
    *   URL query parameters to pass to mount
    * @param session
    *   Session data to pass to mount
    * @tparam State
    *   The state type of the WebView
    * @return
    *   A new WebViewServer with the route added
    */
  def withRoute[State](
      path: String,
      webView: WebView[State],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  ): WebViewServer = {
    val route = WebViewRoute[State](
      factory = () => webView,
      params = params,
      session = session
    )
    copy(routes = routes + (path -> route))
  }

  /** Add a WebView route using a factory function.
    *
    * Useful when you need to create a new instance for each connection.
    *
    * @param path
    *   The URL path for this WebView
    * @param factory
    *   Factory function to create WebView instances
    * @param params
    *   URL query parameters
    * @param session
    *   Session data
    * @tparam State
    *   The state type of the WebView
    * @return
    *   A new WebViewServer with the route added
    */
  def withRouteFactory[State](
      path: String,
      factory: () => WebView[State],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  ): WebViewServer = {
    val route = WebViewRoute[State](
      factory = factory,
      params = params,
      session = session
    )
    copy(routes = routes + (path -> route))
  }

  /** Start the WebView server on the specified port.
    *
    * This creates a WebSocket server and registers all routes.
    *
    * @param port
    *   The port to listen on
    * @param host
    *   The host to bind to (default: localhost)
    * @return
    *   A running server instance
    */
  def start(port: Int, host: String = "localhost"): RunningWebViewServer = {
    if (routes.isEmpty) {
      throw new IllegalStateException(
        "Cannot start WebViewServer with no routes. Use withRoute() to add at least one route."
      )
    }

    println(s"Starting Branch WebView server on $host:$port")
    println(s"Routes:")
    routes.keys.foreach(path => println(s"  ws://$host:$port$path"))

    // For now, we only support a single route at the root level
    // In the future, we could implement path-based routing
    val (mainPath, mainRoute) = routes.head

    if (routes.size > 1) {
      println(s"Warning: Multiple routes detected, but only one is currently supported.")
      println(s"Serving only: $mainPath")
      println(s"Full routing support is coming in a future release.")
    }

    // Create the handler for the main route
    val handler = createHandler(mainRoute)

    // Start the WebSocket server
    val wsServer = WebSocketServer.start(port, handler)

    // Return running server
    RunningWebViewServer(
      actorSystem = actorSystem,
      wsServer = wsServer,
      port = port,
      host = host,
      routes = routes
    )
  }

  /** Create a WebSocketHandler for a given route.
    *
    * @param route
    *   The route configuration
    * @tparam State
    *   The state type
    * @return
    *   A WebSocketHandler
    */
  private def createHandler[State](route: WebViewRoute[State]): WebSocketHandler = {
    WebViewHandler[State](
      actorSystem = actorSystem,
      webViewFactory = route.factory,
      params = route.params,
      session = route.session
    )
  }
}

/** Configuration for a single WebView route.
  *
  * @param factory
  *   Factory function to create WebView instances
  * @param params
  *   URL query parameters to pass to mount
  * @param session
  *   Session data to pass to mount
  * @tparam State
  *   The state type of the WebView
  */
case class WebViewRoute[State](
    factory: () => WebView[State],
    params: Map[String, String],
    session: Session
)

/** A running WebView server instance.
  *
  * Use this to stop the server when you're done.
  *
  * @param actorSystem
  *   The actor system
  * @param wsServer
  *   The underlying WebSocket server
  * @param port
  *   The port the server is listening on
  * @param host
  *   The host the server is bound to
  * @param routes
  *   The routes that were registered
  */
case class RunningWebViewServer(
    actorSystem: ActorSystem,
    wsServer: WebSocketServer,
    port: Int,
    host: String,
    routes: Map[String, WebViewRoute[?]]
) {

  /** Stop the server and cleanup resources. */
  def stop(): Unit = {
    println("Stopping WebView server...")
    wsServer.stop()
    actorSystem.shutdownAwait()
    println("WebView server stopped.")
  }

  /** Get the URL for a specific route.
    *
    * @param path
    *   The route path
    * @return
    *   The WebSocket URL
    */
  def urlFor(path: String): String = {
    s"ws://$host:$port$path"
  }

  /** Print server information to stdout. */
  def printInfo(): Unit = {
    println()
    println("=" * 60)
    println("Branch WebView Server Running")
    println("=" * 60)
    println(s"Host: $host")
    println(s"Port: $port")
    println()
    println("Routes:")
    routes.keys.foreach { path =>
      println(s"  ${urlFor(path)}")
    }
    println("=" * 60)
    println()
  }
}

object WebViewServer {

  /** Create a new WebViewServer with default settings. */
  def apply(): WebViewServer = new WebViewServer()

  /** Create a new WebViewServer with a custom ActorSystem. */
  def apply(actorSystem: ActorSystem): WebViewServer =
    new WebViewServer(actorSystem = actorSystem)
}
