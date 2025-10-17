package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.websocket.WebSocketHandler

/** A trait for creating HTTP and WebSocket applications using SocketServer
  * (pure ServerSocket implementation).
  *
  * This is similar to SpiderApp but uses SocketServer instead of
  * com.sun.net.httpserver.HttpServer. Supports both HTTP and WebSocket
  * connections on the same port.
  *
  * Example usage:
  * {{{
  * object MyApp extends SocketSpiderApp {
  *   override val port = 8080
  *
  *   // HTTP routes
  *   override val router = {
  *     case (HttpMethod.GET, "hello" :: Nil) => helloHandler
  *     case (HttpMethod.POST, "api" :: "users" :: Nil) => createUserHandler
  *   }
  *
  *   // WebSocket routes
  *   override val webSocketRouter = Map(
  *     "/ws" -> myWebSocketHandler,
  *     "/chat" -> chatHandler
  *   )
  * }
  * }}}
  */
trait SocketSpiderApp {

  /** The port on which the server will listen. Defaults to 9000.
    */
  val port: Int = 9000

  /** The maximum number of pending connections the server will queue.
    */
  val backlog: Int = 0

  /** Server configuration with limits and settings. Defaults to
    * ServerConfig.default.
    *
    * Override this to customize limits:
    *   - ServerConfig.default: Production defaults
    *   - ServerConfig.development: Relaxed limits for dev/testing
    *   - ServerConfig.strict: Tighter limits for high-security environments
    */
  val config: ServerConfig = ServerConfig.default

  /** The router that maps (HttpMethod, path segments) to RequestHandlers.
    *
    * Override this to define your application's HTTP routes.
    */
  val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]]

  /** The WebSocket router that maps paths to WebSocketHandlers.
    *
    * Override this to define your application's WebSocket routes. Defaults to
    * empty (no WebSocket support).
    */
  val webSocketRouter: Map[String, WebSocketHandler] = Map.empty

  /** The underlying SocketServer instance.
    */
  final given server: SpiderServer = SpiderServer.withShutdownHook(
    port = port,
    backlog = backlog,
    router = router,
    webSocketRouter = webSocketRouter,
    config = config
  )

  /** The application's main entry point, which starts the HTTP/WebSocket
    * server.
    */
  final def main(args: Array[String]): Unit = {
    println(s"Starting SocketSpiderApp on port $port")
    if (webSocketRouter.nonEmpty) {
      println(s"WebSocket routes: ${webSocketRouter.keys.mkString(", ")}")
    }
    server.start()
  }
}
