package dev.alteration.branch.spider.websocket

import java.io.{InputStream, OutputStream}
import java.net.Socket
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/** Represents the state of a WebSocket connection
  */
enum WebSocketConnectionState {
  case Open
  case Closing
  case Closed
}

/** Manages a WebSocket connection after the handshake is complete.
  *
  * Handles frame-level communication, including:
  *   - Sending and receiving frames
  *   - Automatically responding to ping frames
  *   - Managing connection state and lifecycle
  *   - Handling message fragmentation
  *
  * @param socket
  *   the underlying TCP socket
  * @param inputStream
  *   the socket input stream
  * @param outputStream
  *   the socket output stream
  */
class WebSocketConnection(
    socket: Socket,
    inputStream: InputStream,
    outputStream: OutputStream
) {

  @volatile private var state: WebSocketConnectionState =
    WebSocketConnectionState.Open
  private val writeLock                                 = new Object()
  private val fragmentBuffer                            = mutable.ArrayBuffer.empty[Array[Byte]]
  private var fragmentOpCode: Option[WebSocketOpCode]   = None

  /** Check if the connection is open
    * @return
    *   true if the connection is open
    */
  def isOpen: Boolean = state == WebSocketConnectionState.Open

  /** Check if the connection is closing
    * @return
    *   true if the connection is closing
    */
  def isClosing: Boolean = state == WebSocketConnectionState.Closing

  /** Check if the connection is closed
    * @return
    *   true if the connection is closed
    */
  def isClosed: Boolean = state == WebSocketConnectionState.Closed

  /** Send a WebSocket frame
    *
    * Thread-safe.
    *
    * @param frame
    *   the frame to send
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def sendFrame(frame: WebSocketFrame): Try[Unit] = {
    if (isClosed) {
      return Failure(
        new IllegalStateException("Cannot send frame: connection is closed")
      )
    }

    Try {
      writeLock.synchronized {
        val encoded = WebSocketFrameCodec.encode(frame)
        outputStream.write(encoded)
        outputStream.flush()
      }
    }
  }

  /** Send a text message
    * @param text
    *   the text to send
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def sendText(text: String): Try[Unit] = {
    sendFrame(WebSocketFrame.text(text))
  }

  /** Send binary data
    * @param data
    *   the binary data to send
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def sendBinary(data: Array[Byte]): Try[Unit] = {
    sendFrame(WebSocketFrame.binary(data))
  }

  /** Send a ping frame
    * @param data
    *   optional ping payload
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def sendPing(data: Array[Byte] = Array.empty): Try[Unit] = {
    sendFrame(WebSocketFrame.ping(data))
  }

  /** Send a pong frame (usually in response to a ping)
    * @param data
    *   optional pong payload (should echo ping data)
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def sendPong(data: Array[Byte] = Array.empty): Try[Unit] = {
    sendFrame(WebSocketFrame.pong(data))
  }

  /** Initiate a close handshake
    *
    * Sends a close frame and transitions to Closing state.
    *
    * @param statusCode
    *   optional close status code
    * @param reason
    *   optional close reason
    * @return
    *   Success if sent successfully, Failure otherwise
    */
  def close(
      statusCode: Option[Int] = Some(1000),
      reason: String = ""
  ): Try[Unit] = {
    if (state == WebSocketConnectionState.Closed) {
      return Success(())
    }

    state = WebSocketConnectionState.Closing

    sendFrame(WebSocketFrame.close(statusCode, reason)).flatMap { _ =>
      Try {
        // Give the other side time to respond, then close socket
        Thread.sleep(100)
        closeSocket()
      }
    }
  }

  /** Receive a frame from the connection
    *
    * Blocks until a frame is received. Automatically handles:
    *   - Ping frames (responds with pong)
    *   - Close frames (responds and closes connection)
    *   - Fragmentation (assembles fragmented messages)
    *
    * @return
    *   Success with the received frame, or Failure on error
    */
  def receiveFrame(): Try[WebSocketFrame] = {
    if (isClosed) {
      return Failure(
        new IllegalStateException("Cannot receive frame: connection is closed")
      )
    }

    WebSocketFrameCodec.decode(inputStream).flatMap { frame =>
      frame.opCode match {
        case WebSocketOpCode.Ping =>
          // Automatically respond to pings with pongs
          sendPong(frame.unmaskedPayload)
          receiveFrame() // Continue to next frame

        case WebSocketOpCode.Pong =>
          // Pong received (response to our ping), just pass it through
          Success(frame)

        case WebSocketOpCode.Close =>
          // Close frame received
          handleCloseFrame(frame)
          Success(frame)

        case WebSocketOpCode.Continuation =>
          // Continuation frame - part of a fragmented message
          handleContinuationFrame(frame)

        case WebSocketOpCode.Text | WebSocketOpCode.Binary =>
          // Data frame
          if (!frame.fin) {
            // Start of a fragmented message
            fragmentOpCode = Some(frame.opCode)
            fragmentBuffer.clear()
            fragmentBuffer += frame.unmaskedPayload
            receiveFrame() // Wait for more fragments
          } else {
            // Complete message
            Success(frame)
          }
      }
    }
  }

  /** Handle a continuation frame (part of a fragmented message)
    */
  private def handleContinuationFrame(
      frame: WebSocketFrame
  ): Try[WebSocketFrame] = {
    if (fragmentOpCode.isEmpty) {
      return Failure(
        new IllegalStateException(
          "Received continuation frame without initial fragment"
        )
      )
    }

    fragmentBuffer += frame.unmaskedPayload

    if (frame.fin) {
      // Final fragment - assemble complete message
      val completePayload = fragmentBuffer.flatten.toArray
      val opCode          = fragmentOpCode.get
      fragmentOpCode = None
      fragmentBuffer.clear()

      Success(
        WebSocketFrame(
          fin = true,
          opCode = opCode,
          masked = false,
          maskingKey = None,
          payload = completePayload
        )
      )
    } else {
      // More fragments to come
      receiveFrame()
    }
  }

  /** Handle a close frame
    */
  private def handleCloseFrame(frame: WebSocketFrame): Unit = {
    state match {
      case WebSocketConnectionState.Open =>
        // We received a close frame first, send close frame back
        val payload = frame.unmaskedPayload
        if (payload.length >= 2) {
          val statusCode = ((payload(0) & 0xff) << 8) | (payload(1) & 0xff)
          sendFrame(WebSocketFrame.close(Some(statusCode), ""))
        } else {
          sendFrame(WebSocketFrame.close(None, ""))
        }
        closeSocket()

      case WebSocketConnectionState.Closing =>
        // We initiated close, this is the response
        closeSocket()

      case WebSocketConnectionState.Closed =>
      // Already closed
    }
  }

  /** Close the underlying socket
    */
  private def closeSocket(): Unit = {
    state = WebSocketConnectionState.Closed
    Try {
      socket.close()
    }
  }

}

object WebSocketConnection {

  /** Create a WebSocketConnection from a socket
    * @param socket
    *   the socket
    * @return
    *   a new WebSocketConnection
    */
  def apply(socket: Socket): WebSocketConnection = {
    new WebSocketConnection(
      socket,
      socket.getInputStream,
      socket.getOutputStream
    )
  }

}
