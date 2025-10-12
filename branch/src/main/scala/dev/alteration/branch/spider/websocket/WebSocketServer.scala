package dev.alteration.branch.spider.websocket

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** A standalone WebSocket server that listens on its own port.
  *
  * Supports path-based routing to different WebSocket handlers.
  *
  * Example (single handler):
  * {{{
  *   val server = new WebSocketServer(9001, new EchoWebSocketHandler())
  *   server.start()
  * }}}
  *
  * Example (multiple routes):
  * {{{
  *   val routes = Map(
  *     "/echo" -> new EchoWebSocketHandler(),
  *     "/chat" -> new ChatWebSocketHandler()
  *   )
  *   val server = new WebSocketServer(9001, routes)
  *   server.start()
  * }}}
  *
  * @param port
  *   the port to listen on
  * @param routes
  *   map of paths to WebSocket handlers
  */
class WebSocketServer(port: Int, routes: Map[String, WebSocketHandler])(using
    ec: ExecutionContext
) extends AutoCloseable {

  @volatile private var running                  = false
  private var serverSocket: Option[ServerSocket] = None

  /** Start the WebSocket server
    */
  def start(): Unit = {
    if (running) {
      throw new IllegalStateException("Server is already running")
    }

    running = true
    val socket = new ServerSocket(port)
    serverSocket = Some(socket)

    println(s"WebSocket server listening on port $port")

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
        case e: Exception                            =>
          println(s"Error accepting connection: ${e.getMessage}")
          e.printStackTrace()
      }
    }
  }

  /** Stop the WebSocket server
    */
  def stop(): Unit = {
    running = false
    serverSocket.foreach { socket =>
      Try(socket.close())
    }
    serverSocket = None
  }

  /** Handle a single WebSocket connection
    */
  private def handleConnection(socket: Socket): Unit = {
    Try {
      scala.util.boundary {

        val in  =
          new BufferedReader(new InputStreamReader(socket.getInputStream))
        val out = new PrintWriter(socket.getOutputStream, true)

        // Read HTTP headers
        val headers     = scala.collection.mutable.Map.empty[String, List[String]]
        val requestLine = in.readLine()

        if (requestLine == null || !requestLine.startsWith("GET")) {
          socket.close()
          scala.util.boundary.break()
        }

        // Parse the request path from "GET /path HTTP/1.1"
        val requestPath = requestLine.split(" ") match {
          case Array(_, path, _) =>
            path.split("\\?").head // Remove query params if any
          case _                 => "/"
        }

        // Find the handler for this path
        val handlerOpt = routes.get(requestPath)
        if (handlerOpt.isEmpty) {
          // Send 404 Not Found
          val notFoundResponse =
            s"""HTTP/1.1 404 Not Found\r
Content-Type: text/plain\r
Content-Length: ${s"Path not found: $requestPath".length}\r
\r
Path not found: $requestPath"""
          out.print(notFoundResponse)
          out.flush()
          socket.close()
          scala.util.boundary.break()
        }

        val handler = handlerOpt.get

        // Read headers until empty line
        var line = in.readLine()
        while (line != null && line.nonEmpty) {
          val colonIndex = line.indexOf(':')
          if (colonIndex > 0) {
            val key   = line.substring(0, colonIndex).trim
            val value = line.substring(colonIndex + 1).trim
            headers.updateWith(key) {
              case Some(values) => Some(values :+ value)
              case None         => Some(List(value))
            }
          }
          line = in.readLine()
        }

        // Validate WebSocket handshake
        WebSocketHandshake.validateHandshake(headers.toMap) match {
          case Success(secWebSocketKey) =>
            // Send 101 Switching Protocols response
            val responseBytes =
              WebSocketHandshake.createRawHandshakeResponse(secWebSocketKey)
            socket.getOutputStream.write(responseBytes)
            socket.getOutputStream.flush()

            // Create WebSocket connection
            val wsConnection = WebSocketConnection(socket)

            // Call onConnect
            handler.onConnect(wsConnection)

            // Run message loop (blocks until connection closes)
            handler.runMessageLoop(wsConnection)

          case Failure(error) =>
            // Send 400 Bad Request
            val errorResponse =
              s"""HTTP/1.1 400 Bad Request\r
Content-Type: text/plain\r
Content-Length: ${error.getMessage.length}\r
\r
${error.getMessage}"""
            out.print(errorResponse)
            out.flush()
            socket.close()
        }
      }
    } match {
      case Success(_)     =>
      // Connection handled successfully
      case Failure(error) =>
        println(s"Error handling WebSocket connection: ${error.getMessage}")
        error.printStackTrace()
        Try(socket.close())
    }
  }

  override def close(): Unit = stop()
}

object WebSocketServer {

  /** Create and start a WebSocket server with a single handler (backward
    * compatibility)
    *
    * @param port
    *   the port to listen on
    * @param handler
    *   the WebSocket handler (will be mounted at root "/")
    * @return
    *   the WebSocketServer instance
    */
  def start(port: Int, handler: WebSocketHandler)(using
      ec: ExecutionContext
  ): WebSocketServer = {
    val server = new WebSocketServer(port, Map("/" -> handler))
    Future {
      server.start()
    }
    server
  }

  /** Create and start a WebSocket server with multiple route handlers
    *
    * @param port
    *   the port to listen on
    * @param routes
    *   map of paths to WebSocket handlers
    * @return
    *   the WebSocketServer instance
    */
  def startWithRoutes(port: Int, routes: Map[String, WebSocketHandler])(using
      ec: ExecutionContext
  ): WebSocketServer = {
    val server = new WebSocketServer(port, routes)
    Future {
      server.start()
    }
    server
  }

}
