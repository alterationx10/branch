package dev.alteration.branch.spider.websocket

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** A standalone WebSocket server that listens on its own port.
  *
  * This is an alternative to integrating with HttpServer, providing a simpler
  * and more reliable approach for WebSocket connections.
  *
  * Example:
  * {{{
  *   val server = new WebSocketServer(9001, new EchoWebSocketHandler())
  *   server.start()
  * }}}
  *
  * @param port
  *   the port to listen on
  * @param handler
  *   the WebSocket handler
  */
class WebSocketServer(port: Int, handler: WebSocketHandler)(using
    ec: ExecutionContext
) {

  @volatile private var running = false
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
      val in  = new BufferedReader(new InputStreamReader(socket.getInputStream))
      val out = new PrintWriter(socket.getOutputStream, true)

      // Read HTTP headers
      val headers     = scala.collection.mutable.Map.empty[String, List[String]]
      var requestLine = in.readLine()

      if (requestLine == null || !requestLine.startsWith("GET")) {
        socket.close()
        return
      }

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
          val errorResponse = s"""HTTP/1.1 400 Bad Request\r
Content-Type: text/plain\r
Content-Length: ${error.getMessage.length}\r
\r
${error.getMessage}"""
          out.print(errorResponse)
          out.flush()
          socket.close()
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

}

object WebSocketServer {

  /** Create and start a WebSocket server in a background thread
    *
    * @param port
    *   the port to listen on
    * @param handler
    *   the WebSocket handler
    * @return
    *   the WebSocketServer instance
    */
  def start(port: Int, handler: WebSocketHandler)(using
      ec: ExecutionContext
  ): WebSocketServer = {
    val server = new WebSocketServer(port, handler)
    Future {
      server.start()
    }
    server
  }

}
