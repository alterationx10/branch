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
      handleWebSocketUpgrade(exchange, handler) match {
        case Success(_) =>
        // Connection handled successfully (will be closed by handler)

        case Failure(error) =>
          // Handshake failed, send error response
          val errorMessage = s"WebSocket handshake failed: ${error.getMessage}"
          val bytes        = errorMessage.getBytes("UTF-8")
          exchange.sendResponseHeaders(400, bytes.length)
          exchange.getResponseBody.write(bytes)
          exchange.close()
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

    // Validate the handshake
    WebSocketHandshake.validateHandshake(headers).flatMap { secWebSocketKey =>
      Try {
        // Send 101 Switching Protocols response
        val responseBytes = WebSocketHandshake.createRawHandshakeResponse(
          secWebSocketKey
        )

        // Write the raw response
        val outputStream = exchange.getResponseBody
        outputStream.write(responseBytes)
        outputStream.flush()

        // Get the underlying socket from the HttpExchange
        // This is a bit of a hack, but necessary to get direct socket access
        val socketField =
          exchange.getClass.getDeclaredField("connection")
        socketField.setAccessible(true)
        val connection = socketField.get(exchange)

        val socketFieldInner = connection.getClass.getDeclaredField("chan")
        socketFieldInner.setAccessible(true)
        val socketChannel =
          socketFieldInner.get(connection).asInstanceOf[java.nio.channels.SocketChannel]

        val socket = socketChannel.socket()

        // Create WebSocket connection
        val wsConnection = WebSocketConnection(socket)

        // Call onConnect
        handler.onConnect(wsConnection)

        // Run message loop (blocks until connection closes)
        handler.runMessageLoop(wsConnection)
      }
    }
  }

}
