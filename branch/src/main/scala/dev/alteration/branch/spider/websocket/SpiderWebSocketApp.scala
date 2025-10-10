package dev.alteration.branch.spider.websocket

import dev.alteration.branch.macaroni.runtimes.BranchExecutors

import scala.concurrent.ExecutionContext

/** A WebSocket App that handles websockets on a dedicated port.
  *
  * This provides a clean way to run HTTP and WebSocket servers together without
  * requiring JVM module flags.
  *
  * Example:
  * {{{
  *   object MyApp extends SpiderWebSocketApp {
  *     override val wsPort = 9001 // WebSocket port
  *     override def wsHandler = new MyWebSocketHandler()
  *   }
  * }}}
  */
trait SpiderWebSocketApp {

  given ExecutionContext = BranchExecutors.executionContext

  /** The port for WebSocket connections (defaults to HTTP 9001)
    */
  val wsPort: Int =
    9001

  /** The WebSocket handler
    */
  def wsHandler: WebSocketHandler

  /** The WebSocket server instance
    */
  private lazy val websocketServer: WebSocketServer =
    new WebSocketServer(wsPort, wsHandler)

  // Add shutdown hook for WebSocket server
  Runtime.getRuntime.addShutdownHook {
    new Thread(() => {
      websocketServer.stop()
      println(s"WebSocket server on port $wsPort stopped")
    })
  }

  /** The application's main entry point, which starts the HTTP server.
    */
  final def main(args: Array[String]): Unit =
    websocketServer.start()

}
