package dev.alteration.branch.spider.websocket

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import scala.util.{Failure, Success, Try}

/** A trait for handling WebSocket connections.
  *
  * Extend this trait and override the lifecycle methods to handle WebSocket
  * events:
  * - [[onConnect]]: called when a WebSocket connection is established
  * - [[onMessage]]: called when a message is received
  * - [[onBinary]]: called when binary data is received
  * - [[onClose]]: called when the connection is closed
  * - [[onError]]: called when an error occurs
  *
  * Example:
  * {{{
  *   class EchoWebSocketHandler extends WebSocketHandler {
  *     override def onMessage(connection: WebSocketConnection, message: String): Unit = {
  *       connection.sendText(s"Echo: $$message")
  *     }
  *   }
  * }}}
  */
trait WebSocketHandler {

  /** Called when a WebSocket connection is established
    *
    * @param connection
    *   the WebSocket connection
    */
  def onConnect(connection: WebSocketConnection): Unit = {}

  /** Called when a text message is received
    *
    * @param connection
    *   the WebSocket connection
    * @param message
    *   the text message
    */
  def onMessage(connection: WebSocketConnection, message: String): Unit = {}

  /** Called when binary data is received
    *
    * @param connection
    *   the WebSocket connection
    * @param data
    *   the binary data
    */
  def onBinary(connection: WebSocketConnection, data: Array[Byte]): Unit = {}

  /** Called when the connection is closed
    *
    * @param connection
    *   the WebSocket connection
    * @param statusCode
    *   optional close status code
    * @param reason
    *   optional close reason
    */
  def onClose(
      connection: WebSocketConnection,
      statusCode: Option[Int],
      reason: String
  ): Unit = {}

  /** Called when an error occurs
    *
    * @param connection
    *   the WebSocket connection (may be null if error occurred during handshake)
    * @param error
    *   the error
    */
  def onError(connection: WebSocketConnection, error: Throwable): Unit = {
    error.printStackTrace()
  }

  /** The main message loop for the WebSocket connection.
    *
    * Receives frames and dispatches to the appropriate callback methods.
    * Runs until the connection is closed or an error occurs.
    *
    * @param connection
    *   the WebSocket connection
    */
  private[websocket] def runMessageLoop(connection: WebSocketConnection): Unit = {
    try {
      while (connection.isOpen) {
        connection.receiveFrame() match {
          case Success(frame) =>
            frame.opCode match {
              case WebSocketOpCode.Text =>
                onMessage(connection, frame.payloadAsString)

              case WebSocketOpCode.Binary =>
                onBinary(connection, frame.unmaskedPayload)

              case WebSocketOpCode.Close =>
                // Parse close status code and reason
                val payload = frame.unmaskedPayload
                if (payload.length >= 2) {
                  val statusCode = ((payload(0) & 0xFF) << 8) | (payload(1) & 0xFF)
                  val reason = if (payload.length > 2) {
                    new String(payload.drop(2), "UTF-8")
                  } else {
                    ""
                  }
                  onClose(connection, Some(statusCode), reason)
                } else {
                  onClose(connection, None, "")
                }
                return // Exit message loop

              case WebSocketOpCode.Ping | WebSocketOpCode.Pong =>
              // Already handled by WebSocketConnection

              case WebSocketOpCode.Continuation =>
              // Already handled by WebSocketConnection (fragmentation)
            }

          case Failure(error) =>
            onError(connection, error)
            return // Exit message loop
        }
      }
    } catch {
      case error: Throwable =>
        onError(connection, error)
    }
  }

}

object WebSocketHandler {

  /** Create an HttpHandler that performs the WebSocket handshake and then
    * delegates to the WebSocketHandler.
    *
    * This allows WebSocket handlers to be registered with HttpServer using
    * `server.createContext()`.
    *
    * **IMPORTANT**: Due to Java module system restrictions, this integration
    * requires special JVM flags to access internal HttpServer classes:
    *
    * ```
    * --add-opens jdk.httpserver/sun.net.httpserver=ALL-UNNAMED
    * ```
    *
    * **RECOMMENDED**: Use [[WebSocketServer]] instead for a simpler, more
    * reliable WebSocket server that doesn't require special JVM flags.
    *
    * Example:
    * {{{
    *   val wsHandler = new EchoWebSocketHandler()
    *   server.createContext("/ws", WebSocketHandler.toHttpHandler(wsHandler))
    * }}}
    *
    * @param handler
    *   the WebSocket handler
    * @return
    *   an HttpHandler
    */
  def toHttpHandler(handler: WebSocketHandler): HttpHandler = {
    (exchange: HttpExchange) =>
      try {
        handleWebSocketUpgrade(exchange, handler) match {
          case Success(_) =>
          // Connection handled successfully (will be closed by handler)
          // Do NOT call exchange.close() - we're hijacking the socket

          case Failure(error) =>
            // Handshake failed, send error response
            val isModuleError = error.isInstanceOf[java.lang.reflect.InaccessibleObjectException]

            if (isModuleError) {
              println(s"[WebSocket] ERROR: Cannot access HttpServer internals due to Java module restrictions")
              println(s"[WebSocket] To fix this, either:")
              println(s"[WebSocket]   1. Use WebSocketServer instead (recommended)")
              println(s"[WebSocket]   2. Add JVM flag: --add-opens jdk.httpserver/sun.net.httpserver=ALL-UNNAMED")
            } else {
              println(s"[WebSocket] Handshake failed: ${error.getMessage}")
              error.printStackTrace()
            }

            val errorMessage = if (isModuleError) {
              "WebSocket integration requires JVM flag: --add-opens jdk.httpserver/sun.net.httpserver=ALL-UNNAMED\nOr use WebSocketServer instead."
            } else {
              s"WebSocket handshake failed: ${error.getMessage}"
            }

            val bytes = errorMessage.getBytes("UTF-8")
            exchange.sendResponseHeaders(400, bytes.length)
            exchange.getResponseBody.write(bytes)
            exchange.close()
        }
      } catch {
        case e: Throwable =>
          println(s"[WebSocket] Unexpected error: ${e.getMessage}")
          e.printStackTrace()
          try {
            val errorMessage = s"WebSocket error: ${e.getMessage}"
            val bytes        = errorMessage.getBytes("UTF-8")
            exchange.sendResponseHeaders(500, bytes.length)
            exchange.getResponseBody.write(bytes)
            exchange.close()
          } catch {
            case _: Throwable => // Already failed, can't send response
          }
      }
  }

  /** Handle a WebSocket upgrade request
    *
    * Performs the WebSocket handshake and, if successful, creates a
    * WebSocketConnection and runs the handler's message loop.
    *
    * @param exchange
    *   the HTTP exchange
    * @param handler
    *   the WebSocket handler
    * @return
    *   Success if handshake succeeds, Failure otherwise
    */
  private def handleWebSocketUpgrade(
      exchange: HttpExchange,
      handler: WebSocketHandler
  ): Try[Unit] = {
    import scala.jdk.CollectionConverters.*

    // Extract headers
    val headers = exchange.getRequestHeaders.asScala
      .map { case (k, v) => k -> v.asScala.toList }
      .toMap

    println(s"[WebSocket] Processing upgrade request")
    println(s"[WebSocket] Headers: ${headers.keys.mkString(", ")}")

    // Validate the handshake
    WebSocketHandshake.validateHandshake(headers).flatMap { secWebSocketKey =>
      println(s"[WebSocket] Handshake validated, key: $secWebSocketKey")

      Try {
        println(s"[WebSocket] Extracting socket from exchange")
        println(s"[WebSocket] Exchange class: ${exchange.getClass.getName}")

        // Debug: list all fields
        val allFields = exchange.getClass.getDeclaredFields
        println(s"[WebSocket] Available fields: ${allFields.map(_.getName).mkString(", ")}")

        // Get the underlying socket from the HttpExchange FIRST
        // We need direct socket access to bypass HttpServer's response handling
        val connectionField = allFields.find(f =>
          f.getName.toLowerCase.contains("conn") ||
          f.getName.toLowerCase.contains("impl")
        ).getOrElse {
          throw new NoSuchFieldException(s"Could not find connection field. Available: ${allFields.map(_.getName).mkString(", ")}")
        }

        connectionField.setAccessible(true)
        val connection = connectionField.get(exchange)

        println(s"[WebSocket] Connection field: ${connectionField.getName}")
        println(s"[WebSocket] Connection class: ${connection.getClass.getName}")

        // Debug: list connection fields
        val connectionFields = connection.getClass.getDeclaredFields
        println(s"[WebSocket] Connection fields: ${connectionFields.map(_.getName).mkString(", ")}")

        val socketFieldInner = connectionFields.find(f =>
          f.getName.toLowerCase.contains("chan") ||
          f.getName.toLowerCase.contains("socket")
        ).getOrElse {
          throw new NoSuchFieldException(s"Could not find socket/channel field. Available: ${connectionFields.map(_.getName).mkString(", ")}")
        }

        socketFieldInner.setAccessible(true)
        val socketChannel =
          socketFieldInner.get(connection).asInstanceOf[java.nio.channels.SocketChannel]

        println(s"[WebSocket] Socket field: ${socketFieldInner.getName}")
        println(s"[WebSocket] Got socket channel: $socketChannel")

        val socket = socketChannel.socket()

        println(s"[WebSocket] Got socket: $socket")

        // Now send the 101 response directly to the socket
        val responseBytes = WebSocketHandshake.createRawHandshakeResponse(
          secWebSocketKey
        )

        println(s"[WebSocket] Sending 101 Switching Protocols response directly to socket")

        socket.getOutputStream.write(responseBytes)
        socket.getOutputStream.flush()

        println(s"[WebSocket] Response sent")

        // Create WebSocket connection
        val wsConnection = WebSocketConnection(socket)

        println(s"[WebSocket] WebSocket connection created, calling onConnect")

        // Call onConnect
        handler.onConnect(wsConnection)

        println(s"[WebSocket] Starting message loop")

        // Run message loop (blocks until connection closes)
        handler.runMessageLoop(wsConnection)

        println(s"[WebSocket] Message loop ended")
      }
    }
  }

}
