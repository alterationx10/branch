package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.{WebSocketServer, WebSocketHandler, WebSocketConnection, WebSocketHandshake}
import dev.alteration.branch.spider.http.{HttpHandler, HttpResponse, ResourceServer}
import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import ExecutionContext.Implicits.global

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
  * == DevMode ==
  *
  * {{{
  * val server = WebViewServer()
  *   .withDevMode(enabled = true) // Adds DevTools at /__devtools
  *   .withRoute("/counter", new CounterWebView())
  *   .start(port = 8080)
  * }}}
  *
  * @param actorSystem
  *   The actor system to use (or create a default one)
  * @param routes
  *   Map of paths to route configurations
  * @param devMode
  *   Enable development mode (adds DevTools route)
  */
case class WebViewServer(
    actorSystem: ActorSystem = ActorSystem(),
    routes: Map[String, WebViewRoute[?, ?]] = Map.empty,
    devMode: Boolean = false,
    httpRoutes: Map[String, HttpHandler] = Map.empty,
    serveHtml: Boolean = false
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
    * @param eventCodec
    *   Codec for encoding/decoding events (provided implicitly)
    * @tparam State
    *   The state type of the WebView
    * @tparam Event
    *   The event type of the WebView
    * @return
    *   A new WebViewServer with the route added
    */
  def withRoute[State, Event](
      path: String,
      webView: WebView[State, Event],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  )(using eventCodec: EventCodec[Event]): WebViewServer = {
    val route = WebViewRoute[State, Event](
      factory = () => webView,
      params = params,
      session = session,
      eventCodec = eventCodec
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
    * @param eventCodec
    *   Codec for encoding/decoding events (provided implicitly)
    * @tparam State
    *   The state type of the WebView
    * @tparam Event
    *   The event type of the WebView
    * @return
    *   A new WebViewServer with the route added
    */
  def withRouteFactory[State, Event](
      path: String,
      factory: () => WebView[State, Event],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  )(using eventCodec: EventCodec[Event]): WebViewServer = {
    val route = WebViewRoute[State, Event](
      factory = factory,
      params = params,
      session = session,
      eventCodec = eventCodec
    )
    copy(routes = routes + (path -> route))
  }

  /** Enable or disable development mode.
    *
    * When enabled, adds a DevTools route at /__devtools for monitoring
    * and debugging WebViews.
    *
    * @param enabled
    *   Whether to enable dev mode
    * @return
    *   A new WebViewServer with dev mode enabled/disabled
    */
  def withDevMode(enabled: Boolean): WebViewServer = {
    copy(devMode = enabled)
  }

  /** Enable automatic HTML page serving for WebView routes.
    *
    * When enabled, creates HTTP endpoints that serve HTML pages with
    * the WebView client JavaScript for each WebView route.
    *
    * Example: If you have a route at "/counter", this will create:
    * - HTTP GET /counter - Serves HTML page
    * - WebSocket /ws/counter - WebView WebSocket endpoint
    * - HTTP GET /js/webview.js - Serves the WebView client library
    *
    * @param enabled
    *   Whether to enable HTML serving (default: true)
    * @return
    *   A new WebViewServer with HTML serving enabled/disabled
    */
  def withHtmlPages(enabled: Boolean = true): WebViewServer = {
    copy(serveHtml = enabled)
  }

  /** Add a custom HTTP route.
    *
    * This allows you to serve custom pages, static files, or APIs
    * alongside your WebView routes.
    *
    * @param path
    *   The HTTP path (e.g., "/api/data")
    * @param handler
    *   The HTTP handler
    * @return
    *   A new WebViewServer with the HTTP route added
    */
  def withHttpRoute(path: String, handler: HttpHandler): WebViewServer = {
    copy(httpRoutes = httpRoutes + (path -> handler))
  }

  /** Start the WebView server on the specified port.
    *
    * This creates a server and registers all routes.
    * If HTML serving is enabled or HTTP routes are configured,
    * an internal server will handle both HTTP and WebSocket.
    * Otherwise, a pure WebSocket server is used.
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

    // Add DevTools route if devMode is enabled
    val finalRoutes = if (devMode) {
      import dev.alteration.branch.spider.webview.devtools.*
      import dev.alteration.branch.spider.webview.devtools.DevToolsEvent.given

      val devToolsRoute = WebViewRoute[DevToolsUIState, DevToolsEvent](
        factory = () => new DevToolsWebView(),
        params = Map.empty,
        session = Session(),
        eventCodec = summon[EventCodec[DevToolsEvent]]
      )

      routes + ("/__devtools" -> devToolsRoute)
    } else {
      routes
    }

    // Create handlers for all routes
    // For DevTools route, we'll use a shared actor name that all WebViews can send updates to
    val devToolsActorName = if (devMode) Some("__devtools-actor") else None

    val wsHandlers = finalRoutes.map { case (path, route) =>
      if (path == "/__devtools") {
        // DevTools handler: use shared actor name, don't send updates to self
        path -> createHandler(route, devToolsActorName = None, useSharedActorName = devToolsActorName)
      } else {
        // Regular WebView handler: send updates to DevTools
        path -> createHandler(route, devToolsActorName = devToolsActorName, useSharedActorName = None)
      }
    }

    // Build HTTP routes if HTML serving is enabled
    val finalHttpRoutes = if (serveHtml) {
      // Create HTML page handlers for each WebView route
      val pageHandlers = finalRoutes.map { case (path, _) =>
        val wsPath = if (path.startsWith("/")) path else s"/$path"
        path -> WebViewPageHandler(
          wsUrl = s"ws://$host:$port$wsPath",
          title = s"WebView - ${path.stripPrefix("/")}",
          jsPath = "/js/webview.js",
          debug = devMode
        )
      }

      // Add resource server for webview.js
      // Strip "/js/" prefix so "/js/webview.js" looks up "spider/webview/webview.js"
      val resourceServer = ResourceServer("spider/webview", stripPrefix = Some("/js"))

      httpRoutes ++ pageHandlers + ("/js" -> resourceServer)
    } else {
      httpRoutes
    }

    println(s"Starting Branch WebView server on $host:$port")

    // Decide whether to use internal server or pure WebSocket server
    val server: AutoCloseable = if (finalHttpRoutes.nonEmpty) {
      println("HTTP routes:")
      finalHttpRoutes.keys.foreach(path => println(s"  http://$host:$port$path"))
      println("WebSocket routes:")
      finalRoutes.keys.foreach(path => println(s"  ws://$host:$port$path"))

      if (devMode) {
        println(s"[DevMode] DevTools available at ws://$host:$port/__devtools")
      }

      val internalServer = new InternalWebViewServer(port, finalHttpRoutes, wsHandlers)
      Future { internalServer.start() }
      Thread.sleep(100) // Give it a moment to start
      internalServer
    } else {
      println("WebSocket routes:")
      finalRoutes.keys.foreach(path => println(s"  ws://$host:$port$path"))

      if (devMode) {
        println(s"[DevMode] DevTools available at ws://$host:$port/__devtools")
      }

      WebSocketServer.startWithRoutes(port, wsHandlers)
    }

    // Return running server
    RunningWebViewServer(
      actorSystem = actorSystem,
      server = server,
      port = port,
      host = host,
      routes = finalRoutes,
      devMode = devMode,
      httpEnabled = finalHttpRoutes.nonEmpty
    )
  }

  /** Create a WebSocketHandler for a given route.
    *
    * @param route
    *   The route configuration
    * @param devToolsActorName
    *   Optional DevTools actor name for integration
    * @tparam State
    *   The state type
    * @tparam Event
    *   The event type
    * @return
    *   A WebSocketHandler
    */
  private def createHandler[State, Event](
      route: WebViewRoute[State, Event],
      devToolsActorName: Option[String] = None,
      useSharedActorName: Option[String] = None
  ): WebSocketHandler = {
    given EventCodec[Event] = route.eventCodec

    new WebViewHandler[State, Event](
      actorSystem = actorSystem,
      webViewFactory = route.factory,
      params = route.params,
      session = route.session,
      devToolsActorName = devToolsActorName,
      useExistingActor = useSharedActorName
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
  * @param eventCodec
  *   Codec for encoding/decoding events
  * @tparam State
  *   The state type of the WebView
  * @tparam Event
  *   The event type of the WebView
  */
case class WebViewRoute[State, Event](
    factory: () => WebView[State, Event],
    params: Map[String, String],
    session: Session,
    eventCodec: EventCodec[Event]
)

/** A running WebView server instance.
  *
  * Use this to stop the server when you're done.
  *
  * @param actorSystem
  *   The actor system
  * @param server
  *   The underlying server (WebSocket or Hybrid)
  * @param port
  *   The port the server is listening on
  * @param host
  *   The host the server is bound to
  * @param routes
  *   The routes that were registered
  * @param devMode
  *   Whether dev mode is enabled
  * @param httpEnabled
  *   Whether HTTP routes are enabled
  */
case class RunningWebViewServer(
    actorSystem: ActorSystem,
    server: AutoCloseable,
    port: Int,
    host: String,
    routes: Map[String, WebViewRoute[?, ?]],
    devMode: Boolean = false,
    httpEnabled: Boolean = false
) {

  /** Stop the server and cleanup resources. */
  def stop(): Unit = {
    println("Stopping WebView server...")
    server.close()
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

/** Internal server that handles both HTTP and WebSocket connections.
  *
  * This is used internally by WebViewServer and is not part of the public API.
  */
private class InternalWebViewServer(
    port: Int,
    httpRoutes: Map[String, HttpHandler],
    wsRoutes: Map[String, WebSocketHandler]
)(using ec: ExecutionContext) extends AutoCloseable {

  @volatile private var running = false
  private var serverSocket: Option[ServerSocket] = None

  /** Start the server. */
  def start(): Unit = {
    if (running) {
      throw new IllegalStateException("Server is already running")
    }

    running = true
    val socket = new ServerSocket(port)
    serverSocket = Some(socket)

    // Accept connections in a loop
    while (running) {
      try {
        val clientSocket = socket.accept()
        // Handle each connection in its own thread
        Future {
          handleConnection(clientSocket)
        }
      } catch {
        case _: java.net.SocketException if !running =>
          // Server was stopped, exit gracefully
        case e: Exception =>
          println(s"Error accepting connection: ${e.getMessage}")
          e.printStackTrace()
      }
    }
  }

  /** Stop the server. */
  def stop(): Unit = {
    running = false
    serverSocket.foreach { socket =>
      Try(socket.close())
    }
    serverSocket = None
  }

  /** Handle a single connection (HTTP or WebSocket). */
  private def handleConnection(socket: Socket): Unit = {
    val result = Try {
      val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

      // Read HTTP headers
      val headers = scala.collection.mutable.Map.empty[String, List[String]]
      val requestLine = in.readLine()

      if (requestLine == null || !requestLine.startsWith("GET")) {
        socket.close()
      } else {

      // Parse the request path from "GET /path HTTP/1.1"
      val requestPath = requestLine.split(" ") match {
        case Array(_, path, _) => path.split("\\?").head // Remove query params
        case _ => "/"
      }

      // Read headers until empty line
      var line = in.readLine()
      while (line != null && line.nonEmpty) {
        val colonIndex = line.indexOf(':')
        if (colonIndex > 0) {
          val key = line.substring(0, colonIndex).trim
          val value = line.substring(colonIndex + 1).trim
          headers.updateWith(key) {
            case Some(values) => Some(values :+ value)
            case None => Some(List(value))
          }
        }
        line = in.readLine()
      }

      val headersMap = headers.toMap

      // Check if this is a WebSocket upgrade request
      val isWebSocket = WebSocketHandshake.validateHandshake(headersMap).isSuccess

      if (isWebSocket) {
        // Try to find a WebSocket handler
        wsRoutes.get(requestPath) match {
          case Some(wsHandler) =>
            // Handle WebSocket upgrade
            WebSocketHandshake.validateHandshake(headersMap) match {
              case Success(secWebSocketKey) =>
                // Send 101 Switching Protocols response
                val responseBytes = WebSocketHandshake.createRawHandshakeResponse(secWebSocketKey)
                socket.getOutputStream.write(responseBytes)
                socket.getOutputStream.flush()

                // Create WebSocket connection and handle it
                val wsConnection = WebSocketConnection(socket)
                WebSocketHandler.handleConnection(wsHandler, wsConnection)

              case Failure(handshakeError) =>
                HttpResponse.internalError(socket, s"WebSocket handshake failed: ${handshakeError.getMessage}")
                socket.close()
            }

          case None =>
            HttpResponse.notFound(socket, s"WebSocket route not found: $requestPath")
            socket.close()
        }
      } else {
        // Try to find an HTTP handler
        // First try exact match
        httpRoutes.get(requestPath) match {
          case Some(httpHandler) =>
            httpHandler.handleGet(requestPath, headersMap, socket)
            socket.close()

          case None =>
            // Try prefix match (for serving directories)
            httpRoutes.find { case (routePath, _) =>
              requestPath.startsWith(routePath)
            } match {
              case Some((_, httpHandler)) =>
                httpHandler.handleGet(requestPath, headersMap, socket)
                socket.close()

              case None =>
                HttpResponse.notFound(socket, s"Route not found: $requestPath")
                socket.close()
            }
        }
      }
      }
    }

    result match {
      case Success(_) =>
        // Connection handled successfully
      case Failure(err) =>
        println(s"Error handling connection: ${err.getMessage}")
        err.printStackTrace()
        Try(socket.close())
    }
  }

  override def close(): Unit = stop()
}
