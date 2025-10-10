package dev.alteration.branch.spider.websocket

/** A WebSocket frame as defined in RFC 6455.
  *
  * Frame structure:
  * {{{
  *   0                   1                   2                   3
  *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  *  +-+-+-+-+-------+-+-------------+-------------------------------+
  *  |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
  *  |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
  *  |N|V|V|V|       |S|             |   (if payload len==126/127)   |
  *  | |1|2|3|       |K|             |                               |
  *  +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
  *  |     Extended payload length continued, if payload len == 127  |
  *  + - - - - - - - - - - - - - - - +-------------------------------+
  *  |                               |Masking-key, if MASK set to 1  |
  *  +-------------------------------+-------------------------------+
  *  | Masking-key (continued)       |          Payload Data         |
  *  +-------------------------------- - - - - - - - - - - - - - - - +
  *  :                     Payload Data continued ...                :
  *  + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
  *  |                     Payload Data continued ...                |
  *  +---------------------------------------------------------------+
  * }}}
  *
  * @param fin
  *   indicates if this is the final fragment in a message
  * @param opCode
  *   the frame opcode
  * @param masked
  *   indicates if the payload is masked (must be true for client frames)
  * @param maskingKey
  *   the 4-byte masking key (if masked)
  * @param payload
  *   the frame payload data
  */
case class WebSocketFrame(
    fin: Boolean,
    opCode: WebSocketOpCode,
    masked: Boolean,
    maskingKey: Option[Array[Byte]],
    payload: Array[Byte]
) {

  /** Get the unmasked payload data
    * @return
    *   the unmasked payload
    */
  def unmaskedPayload: Array[Byte] = {
    if (!masked) {
      payload
    } else {
      maskingKey match {
        case Some(key) =>
          payload.zipWithIndex.map { case (byte, i) =>
            (byte ^ key(i % 4)).toByte
          }
        case None =>
          payload
      }
    }
  }

  /** Get the payload as a UTF-8 string (for text frames)
    * @return
    *   the payload as a string
    */
  def payloadAsString: String = {
    new String(unmaskedPayload, "UTF-8")
  }

}

object WebSocketFrame {

  /** Create a text frame
    * @param text
    *   the text content
    * @param fin
    *   whether this is the final fragment (default true)
    * @return
    *   a WebSocketFrame
    */
  def text(text: String, fin: Boolean = true): WebSocketFrame = {
    WebSocketFrame(
      fin = fin,
      opCode = WebSocketOpCode.Text,
      masked = false,
      maskingKey = None,
      payload = text.getBytes("UTF-8")
    )
  }

  /** Create a binary frame
    * @param data
    *   the binary data
    * @param fin
    *   whether this is the final fragment (default true)
    * @return
    *   a WebSocketFrame
    */
  def binary(data: Array[Byte], fin: Boolean = true): WebSocketFrame = {
    WebSocketFrame(
      fin = fin,
      opCode = WebSocketOpCode.Binary,
      masked = false,
      maskingKey = None,
      payload = data
    )
  }

  /** Create a close frame
    * @param statusCode
    *   optional close status code
    * @param reason
    *   optional close reason
    * @return
    *   a WebSocketFrame
    */
  def close(statusCode: Option[Int] = None, reason: String = ""): WebSocketFrame = {
    val payload = statusCode match {
      case Some(code) =>
        val codeBytes   = Array((code >> 8).toByte, code.toByte)
        val reasonBytes = reason.getBytes("UTF-8")
        codeBytes ++ reasonBytes
      case None =>
        Array.empty[Byte]
    }

    WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Close,
      masked = false,
      maskingKey = None,
      payload = payload
    )
  }

  /** Create a ping frame
    * @param data
    *   optional ping payload
    * @return
    *   a WebSocketFrame
    */
  def ping(data: Array[Byte] = Array.empty): WebSocketFrame = {
    WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Ping,
      masked = false,
      maskingKey = None,
      payload = data
    )
  }

  /** Create a pong frame
    * @param data
    *   optional pong payload (usually echoes ping data)
    * @return
    *   a WebSocketFrame
    */
  def pong(data: Array[Byte] = Array.empty): WebSocketFrame = {
    WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Pong,
      masked = false,
      maskingKey = None,
      payload = data
    )
  }

}
