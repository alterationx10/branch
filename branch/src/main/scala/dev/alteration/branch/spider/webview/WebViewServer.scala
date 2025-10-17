package dev.alteration.branch.spider.webview

import dev.alteration.branch.keanu.actors.ActorSystem
import dev.alteration.branch.spider.websocket.WebSocketHandler
import dev.alteration.branch.spider.webview.http.ResourceServer

import scala.concurrent.ExecutionContext
import scala.annotation.nowarn

/** Represents a route in the server - either WebSocket or HTTP.
  */
sealed trait ServerRoute

object ServerRoute {

  /** A WebSocket route (including WebView routes).
    *
    * @param handler
    *   The WebSocket handler
    */
  case class WebSocket(handler: WebSocketHandler) extends ServerRoute

  /** An HTTP route.
    *
    * @param handler
    *   The HTTP request handler
    */
  case class Http(
      handler: dev.alteration.branch.spider.server.RequestHandler[?, ?]
  ) extends ServerRoute
}

/** Fluent builder API for setting up Branch WebView servers.
  *
  * This provides an ergonomic way to configure and start WebView servers
  * without manually creating ActorSystems, handlers, and routing.
  *
  * ==Quick Start==
  *
  * {{{
  * import dev.alteration.branch.spider.webview._
  *
  * val server = WebViewServer()
  *   .withWebViewRoute("/counter", new CounterWebView())
  *   .withWebViewRoute("/todos", new TodoListWebView())
  *   .start(port = 8080)
  *
  * // Later...
  * server.stop()
  * }}}
  *
  * ==Advanced Usage==
  *
  * {{{
  * val customActorSystem = ActorSystem()
  *
  * val server = WebViewServer(customActorSystem)
  *   .withWebViewRoute("/counter", new CounterWebView(),
  *     params = Map("initial" -> "10"),
  *     session = Session(Map("userId" -> user.id)))
  *   .withWebViewRoute("/admin", new AdminWebView())
  *   .start(port = 8080)
  * }}}
  *
  * ==DevMode==
  *
  * {{{
  * val server = WebViewServer()
  *   .withDevMode(enabled = true) // Adds DevTools at /__devtools
  *   .withWebViewRoute("/counter", new CounterWebView())
  *   .start(port = 8080)
  * }}}
  *
  * @param actorSystem
  *   The actor system to use (or create a default one)
  * @param routes
  *   Map of paths to server routes (WebSocket or HTTP)
  * @param devMode
  *   Enable development mode (adds DevTools route)
  * @param webViewMeta
  *   Metadata for WebView routes (params, session, etc.)
  */
case class WebViewServer(
    actorSystem: ActorSystem = ActorSystem(),
    routes: Map[String, ServerRoute] = Map.empty,
    devMode: Boolean = false,
    webViewMeta: Map[String, WebViewRoute[?, ?]] = Map.empty
) {

  /** Add a WebView route to the server.
    *
    * This automatically creates both a WebSocket endpoint for the WebView and
    * an HTTP endpoint that serves an HTML page with the WebView client.
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
  def withWebViewRoute[State, Event](
      path: String,
      webView: WebView[State, Event],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  )(using eventCodec: EventCodec[Event]): WebViewServer = {
    val meta = WebViewRoute[State, Event](
      factory = () => webView,
      params = params,
      session = session,
      eventCodec = eventCodec
    )
    // Store metadata for later handler creation in start()
    copy(webViewMeta = webViewMeta + (path -> meta))
  }

  /** Add a WebView route using a factory function.
    *
    * Useful when you need to create a new instance for each connection. This
    * automatically creates both a WebSocket endpoint and an HTTP page.
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
  def withWebViewRouteFactory[State, Event](
      path: String,
      factory: () => WebView[State, Event],
      params: Map[String, String] = Map.empty,
      session: Session = Session()
  )(using eventCodec: EventCodec[Event]): WebViewServer = {
    val meta = WebViewRoute[State, Event](
      factory = factory,
      params = params,
      session = session,
      eventCodec = eventCodec
    )
    copy(webViewMeta = webViewMeta + (path -> meta))
  }

  /** Add a raw WebSocket route (without HTML page generation).
    *
    * Use this for WebSocket endpoints that don't need the WebView framework or
    * for custom WebSocket handlers.
    *
    * @param path
    *   The URL path for this WebSocket
    * @param handler
    *   The WebSocket handler
    * @return
    *   A new WebViewServer with the WebSocket route added
    */
  def withWebSocketRoute(
      path: String,
      handler: WebSocketHandler
  ): WebViewServer = {
    copy(routes = routes + (path -> ServerRoute.WebSocket(handler)))
  }

  /** Enable or disable development mode.
    *
    * When enabled, adds a DevTools route at /__devtools for monitoring and
    * debugging WebViews.
    *
    * @param enabled
    *   Whether to enable dev mode
    * @return
    *   A new WebViewServer with dev mode enabled/disabled
    */
  def withDevMode(enabled: Boolean): WebViewServer = {
    copy(devMode = enabled)
  }

  /** Add a custom HTTP route.
    *
    * This allows you to serve custom pages, static files, or APIs alongside
    * your WebView routes. Supports full REST capabilities via RequestHandler.
    *
    * @param path
    *   The HTTP path (e.g., "/api/data")
    * @param handler
    *   The HTTP request handler
    * @return
    *   A new WebViewServer with the HTTP route added
    */
  def withHttpRoute(
      path: String,
      handler: dev.alteration.branch.spider.server.RequestHandler[?, ?]
  ): WebViewServer = {
    copy(routes = routes + (path -> ServerRoute.Http(handler)))
  }

  /** Start the WebView server on the specified port.
    *
    * This creates a SpiderServer and registers all routes (both HTTP and
    * WebSocket).
    *
    * @param port
    *   The port to listen on
    * @param host
    *   The host to bind to (default: localhost)
    * @return
    *   A running server instance
    */
  def start(port: Int, host: String = "localhost"): RunningWebViewServer = {
    import dev.alteration.branch.spider.server.SpiderServer
    import dev.alteration.branch.spider.server.RequestHandler.given
    import dev.alteration.branch.spider.common.HttpMethod
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    if (routes.isEmpty && webViewMeta.isEmpty) {
      throw new IllegalStateException(
        "Cannot start WebViewServer with no routes. Use withWebViewRoute(), withWebSocketRoute(), or withHttpRoute() to add at least one route."
      )
    }

    // Add DevTools to webViewMeta if devMode is enabled
    val finalWebViewMeta = if (devMode) {
      import dev.alteration.branch.spider.webview.devtools.*
      import dev.alteration.branch.spider.webview.devtools.DevToolsEvent.given

      val devToolsRoute = WebViewRoute[DevToolsUIState, DevToolsEvent](
        factory = () => new DevToolsWebView(),
        params = Map.empty,
        session = Session(),
        eventCodec = summon[EventCodec[DevToolsEvent]]
      )

      webViewMeta + ("/__devtools" -> devToolsRoute)
    } else {
      webViewMeta
    }

    val devToolsActorName = if (devMode) Some("__devtools-actor") else None

    // Partition user-defined routes by type
    val (userWsRoutes, userHttpRoutes) = routes.partition { case (_, route) =>
      route.isInstanceOf[ServerRoute.WebSocket]
    }

    // For each WebView, create both WebSocket handler and HTML page handler
    val webViewWsHandlers = finalWebViewMeta.map { case (path, meta) =>
      // Create WebSocket handler
      val wsHandler = if (path == "/__devtools") {
        createHandler(
          meta.asInstanceOf[WebViewRoute[Any, Any]],
          devToolsActorName = None,
          useSharedActorName = devToolsActorName
        )
      } else {
        createHandler(
          meta.asInstanceOf[WebViewRoute[Any, Any]],
          devToolsActorName = devToolsActorName,
          useSharedActorName = None
        )
      }
      path -> wsHandler
    }

    val webViewHttpHandlers = finalWebViewMeta.map { case (path, _) =>
      // Create HTML page handler
      val wsPath      = if (path.startsWith("/")) path else s"/$path"
      val pageHandler = WebViewPageHandler(
        wsUrl = s"ws://$host:$port$wsPath",
        title = s"WebView - ${path.stripPrefix("/")}",
        jsPath = "/js/webview.js",
        debug = devMode
      )
      path -> pageHandler
    }

    // Add resource server for webview.js if we have any WebViews
    val jsHandler = if (finalWebViewMeta.nonEmpty) {
      val resourceServer =
        ResourceServer("spider/webview", stripPrefix = Some("/js"))
      Map("/js" -> resourceServer)
    } else {
      Map.empty
    }

    // Combine WebSocket handlers
    val wsHandlers = userWsRoutes.map { case (path, route) =>
      path -> route.asInstanceOf[ServerRoute.WebSocket].handler
    } ++ webViewWsHandlers

    // Combine HTTP handlers
    val httpHandlerMap = userHttpRoutes.map { case (path, route) =>
      path -> route.asInstanceOf[ServerRoute.Http].handler
    } ++ webViewHttpHandlers ++ jsHandler

    // Convert HTTP routes to SpiderServer router (PartialFunction)
    val httpRouter: PartialFunction[
      (HttpMethod, List[String]),
      dev.alteration.branch.spider.server.RequestHandler[?, ?]
    ] = { case (HttpMethod.GET, pathSegments) =>
      val path = "/" + pathSegments.mkString("/")

      // Try exact match first
      httpHandlerMap.get(path) match {
        case Some(handler) => handler
        case None          =>
          // Try prefix match for resource serving
          httpHandlerMap
            .find { case (routePath, _) =>
              path.startsWith(routePath)
            }
            .map(_._2)
            .getOrElse(
              // Return 404 handler
              new dev.alteration.branch.spider.server.RequestHandler[
                Unit,
                String
              ] {
                override def handle(
                    request: dev.alteration.branch.spider.server.Request[Unit]
                ): dev.alteration.branch.spider.server.Response[String] = {
                  dev.alteration.branch.spider.server
                    .Response(404, s"Not found: $path")
                    .htmlContent
                }
              }
            )
      }
    }

    println(s"Starting Branch WebView server on $host:$port")

    if (httpHandlerMap.nonEmpty) {
      println("HTTP routes:")
      httpHandlerMap.keys.foreach(path => println(s"  http://$host:$port$path"))
    }

    if (wsHandlers.nonEmpty) {
      println("WebSocket routes:")
      wsHandlers.keys.foreach(path => println(s"  ws://$host:$port$path"))
    }

    if (devMode) {
      println(s"[DevMode] DevTools available at ws://$host:$port/__devtools")
    }

    // Create SpiderServer with both HTTP and WebSocket support
    val spiderServer = new SpiderServer(
      port = port,
      router = httpRouter,
      webSocketRouter = wsHandlers
    )

    // Start server in background
    Future { spiderServer.start() }
    Thread.sleep(100) // Give it a moment to start

    // Return running server
    RunningWebViewServer(
      actorSystem = actorSystem,
      server = spiderServer,
      port = port,
      host = host,
      routes = finalWebViewMeta,
      devMode = devMode,
      httpEnabled = httpHandlerMap.nonEmpty
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
  @nowarn
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
