package dev.alteration.branch.spider.websocket

/** WebSocket frame opcodes as defined in RFC 6455.
  *
  * Opcodes define the interpretation of the payload data:
  *   - Continuation: continuation of a fragmented message
  *   - Text: UTF-8 text data
  *   - Binary: arbitrary binary data
  *   - Close: connection close control frame
  *   - Ping: heartbeat/keepalive ping
  *   - Pong: response to ping
  */
enum WebSocketOpCode(val code: Byte) {
  case Continuation extends WebSocketOpCode(0x0)
  case Text         extends WebSocketOpCode(0x1)
  case Binary       extends WebSocketOpCode(0x2)
  case Close        extends WebSocketOpCode(0x8)
  case Ping         extends WebSocketOpCode(0x9)
  case Pong         extends WebSocketOpCode(0xa)
}

object WebSocketOpCode {

  /** Convert a byte code to a WebSocketOpCode
    * @param code
    *   the opcode byte value
    * @return
    *   Some(opcode) if valid, None otherwise
    */
  def fromByte(code: Byte): Option[WebSocketOpCode] = code match {
    case 0x0 => Some(Continuation)
    case 0x1 => Some(Text)
    case 0x2 => Some(Binary)
    case 0x8 => Some(Close)
    case 0x9 => Some(Ping)
    case 0xa => Some(Pong)
    case _   => None
  }

  /** Check if an opcode represents a control frame
    * @param opCode
    *   the opcode to check
    * @return
    *   true if this is a control frame (Close, Ping, or Pong)
    */
  def isControl(opCode: WebSocketOpCode): Boolean = opCode match {
    case Close | Ping | Pong => true
    case _                   => false
  }

}
