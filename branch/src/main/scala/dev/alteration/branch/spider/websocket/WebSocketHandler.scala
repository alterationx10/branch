package dev.alteration.branch.spider.websocket

import scala.util.{Failure, Success}

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
