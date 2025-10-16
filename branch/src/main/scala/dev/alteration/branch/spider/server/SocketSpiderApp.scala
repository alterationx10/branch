package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.HttpMethod

/** A trait for creating HTTP applications using SocketServer (pure ServerSocket
  * implementation).
  *
  * This is similar to SpiderApp but uses SocketServer instead of
  * com.sun.net.httpserver.HttpServer.
  *
  * Example usage:
  * {{{
  * object MyApp extends SocketSpiderApp {
  *   override val port = 8080
  *
  *   override val router = {
  *     case (HttpMethod.GET, "hello" :: Nil) => helloHandler
  *     case (HttpMethod.POST, "api" :: "users" :: Nil) => createUserHandler
  *   }
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

  /** Socket read timeout in milliseconds. Defaults to 30 seconds.
    */
  val socketTimeout: Int = 30000

  /** The router that maps (HttpMethod, path segments) to RequestHandlers.
    *
    * Override this to define your application's routes.
    */
  val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]]

  /** The underlying SocketServer instance.
    */
  final given server: SocketServer = SocketServer.withShutdownHook(
    port = port,
    backlog = backlog,
    router = router,
    socketTimeout = socketTimeout
  )

  /** The application's main entry point, which starts the HTTP server.
    */
  final def main(args: Array[String]): Unit = {
    println(s"Starting SocketSpiderApp on port $port")
    server.start()
  }
}
