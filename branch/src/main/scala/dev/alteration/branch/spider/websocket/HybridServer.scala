package dev.alteration.branch.spider.websocket

import dev.alteration.branch.spider.http.{HttpHandler, HttpResponse}
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** A server that handles both HTTP and WebSocket requests.
  *
  * This allows you to serve static files (HTML, JS, CSS) alongside WebSocket
  * endpoints, which is perfect for WebView applications.
  *
  * Example:
  * {{{
  * val server = HybridServer(
  *   port = 8080,
  *   httpRoutes = Map(
  *     "/" -> (_ => HttpResponse.ok(_, "<h1>Hello</h1>")),
  *     "/static" -> ResourceServer("static")
  *   ),
  *   wsRoutes = Map(
  *     "/ws" -> new MyWebSocketHandler()
  *   )
  * )
  * server.start()
  * }}}
  *
  * @param port
  *   The port to listen on
  * @param httpRoutes
  *   Map of paths to HTTP handlers
  * @param wsRoutes
  *   Map of paths to WebSocket handlers
  * @param ec
  *   Execution context for handling connections
  */
class HybridServer(
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

    println(s"HybridServer listening on port $port")
    if (httpRoutes.nonEmpty) {
      println("HTTP routes:")
      httpRoutes.keys.foreach(path => println(s"  http://localhost:$port$path"))
    }
    if (wsRoutes.nonEmpty) {
      println("WebSocket routes:")
      wsRoutes.keys.foreach(path => println(s"  ws://localhost:$port$path"))
    }

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
    println("HybridServer stopped")
  }

  /** Handle a single connection (HTTP or WebSocket). */
  private def handleConnection(socket: Socket): Unit = {
    Try {
      val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
      val out = new PrintWriter(socket.getOutputStream, true)

      // Read HTTP headers
      val headers = scala.collection.mutable.Map.empty[String, List[String]]
      val requestLine = in.readLine()

      if (requestLine == null || !requestLine.startsWith("GET")) {
        socket.close()
        return
      }

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

                // Create WebSocket connection
                val wsConnection = WebSocketConnection(socket)

                // Call onConnect
                wsHandler.onConnect(wsConnection)

                // Run message loop (blocks until connection closes)
                wsHandler.runMessageLoop(wsConnection)

              case Failure(error) =>
                HttpResponse.internalError(socket, s"WebSocket handshake failed: ${error.getMessage}")
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
    } match {
      case Success(_) =>
        // Connection handled successfully
      case Failure(error) =>
        println(s"Error handling connection: ${error.getMessage}")
        error.printStackTrace()
        Try(socket.close())
    }
  }

  override def close(): Unit = stop()
}

object HybridServer {

  /** Create and start a hybrid server with HTTP and WebSocket routes.
    *
    * @param port
    *   The port to listen on
    * @param httpRoutes
    *   Map of paths to HTTP handlers (default: empty)
    * @param wsRoutes
    *   Map of paths to WebSocket handlers (default: empty)
    * @param ec
    *   Execution context
    * @return
    *   The server instance
    */
  def apply(
      port: Int,
      httpRoutes: Map[String, HttpHandler] = Map.empty,
      wsRoutes: Map[String, WebSocketHandler] = Map.empty
  )(using ec: ExecutionContext): HybridServer = {
    new HybridServer(port, httpRoutes, wsRoutes)
  }

  /** Create and start a hybrid server in the background.
    *
    * @param port
    *   The port to listen on
    * @param httpRoutes
    *   Map of paths to HTTP handlers
    * @param wsRoutes
    *   Map of paths to WebSocket handlers
    * @param ec
    *   Execution context
    * @return
    *   The server instance (already running in background)
    */
  def startInBackground(
      port: Int,
      httpRoutes: Map[String, HttpHandler] = Map.empty,
      wsRoutes: Map[String, WebSocketHandler] = Map.empty
  )(using ec: ExecutionContext): HybridServer = {
    val server = new HybridServer(port, httpRoutes, wsRoutes)
    Future {
      server.start()
    }
    // Give it a moment to start
    Thread.sleep(100)
    server
  }
}
