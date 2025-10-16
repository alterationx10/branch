package spider.server

import java.net.{Socket, URI}
import java.io.{InputStream, OutputStream}
import java.util.Base64
import scala.util.Random

/** Simple WebSocket test client to verify SocketServer WebSocket support.
  *
  * This connects to a WebSocket endpoint, sends a few messages, and prints
  * responses.
  */
object WebSocketTestClient {

  def main(args: Array[String]): Unit = {
    val uri = new URI("ws://localhost:9000/ws/echo")
    println(s"Connecting to $uri...")

    val socket = new Socket(uri.getHost, uri.getPort)
    val input = socket.getInputStream
    val output = socket.getOutputStream

    try {
      // Perform WebSocket handshake
      val key = performHandshake(uri, input, output)
      println("✅ WebSocket connected!")

      // Send some test messages
      sendTextMessage(output, "Hello WebSocket!")
      Thread.sleep(100)
      readAndPrintMessages(input)

      sendTextMessage(output, "Testing echo...")
      Thread.sleep(100)
      readAndPrintMessages(input)

      sendTextMessage(output, "Final message")
      Thread.sleep(100)
      readAndPrintMessages(input)

      println("✅ Test completed successfully")

    } finally {
      socket.close()
    }
  }

  /** Perform WebSocket handshake */
  private def performHandshake(
      uri: URI,
      input: InputStream,
      output: OutputStream
  ): String = {
    // Generate random key
    val keyBytes = new Array[Byte](16)
    Random.nextBytes(keyBytes)
    val key = Base64.getEncoder.encodeToString(keyBytes)

    // Send handshake request
    val request = s"""GET ${uri.getPath} HTTP/1.1\r
Host: ${uri.getHost}:${uri.getPort}\r
Upgrade: websocket\r
Connection: Upgrade\r
Sec-WebSocket-Key: $key\r
Sec-WebSocket-Version: 13\r
\r
"""
    output.write(request.getBytes("UTF-8"))
    output.flush()

    // Read response
    val response = readUntilBlankLine(input)
    if (!response.contains("101 Switching Protocols")) {
      throw new RuntimeException(s"Handshake failed: $response")
    }

    key
  }

  /** Read HTTP headers until blank line */
  private def readUntilBlankLine(input: InputStream): String = {
    val sb = new StringBuilder
    var lastChar = '\u0000'
    var current = input.read()

    while (current != -1) {
      val char = current.toChar
      sb.append(char)

      // Check for \r\n\r\n (blank line)
      if (sb.length >= 4 && sb.takeRight(4).toString == "\r\n\r\n") {
        return sb.toString
      }

      lastChar = char
      current = input.read()
    }

    sb.toString
  }

  /** Send a text message frame */
  private def sendTextMessage(output: OutputStream, text: String): Unit = {
    val payload = text.getBytes("UTF-8")
    val frame = buildFrame(0x1, payload, masked = true) // 0x1 = text
    output.write(frame)
    output.flush()
    println(s"📤 Sent: $text")
  }

  /** Build a WebSocket frame */
  private def buildFrame(
      opcode: Int,
      payload: Array[Byte],
      masked: Boolean
  ): Array[Byte] = {
    val buffer = new scala.collection.mutable.ArrayBuffer[Byte]()

    // Byte 0: FIN + opcode
    buffer += (0x80 | opcode).toByte

    // Byte 1: MASK + payload length
    val len = payload.length
    val maskBit = if (masked) 0x80 else 0x00

    if (len < 126) {
      buffer += (maskBit | len).toByte
    } else if (len <= 0xffff) {
      buffer += (maskBit | 126).toByte
      buffer += ((len >> 8) & 0xff).toByte
      buffer += (len & 0xff).toByte
    } else {
      buffer += (maskBit | 127).toByte
      // 64-bit length (we'll only use lower 32 bits)
      buffer ++= Array[Byte](0, 0, 0, 0)
      buffer += ((len >> 24) & 0xff).toByte
      buffer += ((len >> 16) & 0xff).toByte
      buffer += ((len >> 8) & 0xff).toByte
      buffer += (len & 0xff).toByte
    }

    // Masking key (if masked)
    val maskingKey = if (masked) {
      val key = new Array[Byte](4)
      Random.nextBytes(key)
      buffer ++= key
      key
    } else {
      null
    }

    // Payload (masked if needed)
    if (masked) {
      payload.zipWithIndex.foreach { case (byte, i) =>
        buffer += (byte ^ maskingKey(i % 4)).toByte
      }
    } else {
      buffer ++= payload
    }

    buffer.toArray
  }

  /** Read and print any available messages */
  private def readAndPrintMessages(input: InputStream): Unit = {
    // Try to read a frame (timeout after 1 second)
    var attempts = 0
    while (attempts < 10 && input.available() == 0) {
      Thread.sleep(100)
      attempts += 1
    }

    if (input.available() > 0) {
      val frame = readFrame(input)
      println(s"📨 Received: ${new String(frame, "UTF-8")}")
    }
  }

  /** Read a WebSocket frame */
  private def readFrame(input: InputStream): Array[Byte] = {
    // Read first byte (FIN + opcode)
    val byte0 = input.read()
    if (byte0 == -1) throw new RuntimeException("Connection closed")

    val fin = (byte0 & 0x80) != 0
    val opcode = byte0 & 0x0f

    // Read second byte (MASK + payload length)
    val byte1 = input.read()
    if (byte1 == -1) throw new RuntimeException("Connection closed")

    val masked = (byte1 & 0x80) != 0
    var len = byte1 & 0x7f

    // Extended payload length
    if (len == 126) {
      val b1 = input.read()
      val b2 = input.read()
      len = ((b1 & 0xff) << 8) | (b2 & 0xff)
    } else if (len == 127) {
      // Read 8 bytes (we'll only use lower 4)
      input.read()
      input.read()
      input.read()
      input.read()
      val b1 = input.read()
      val b2 = input.read()
      val b3 = input.read()
      val b4 = input.read()
      len = ((b1 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 8) | (b4 & 0xff)
    }

    // Masking key (if present, but server shouldn't mask)
    val maskingKey = if (masked) {
      val key = new Array[Byte](4)
      input.read(key)
      key
    } else {
      null
    }

    // Read payload
    val payload = new Array[Byte](len)
    var offset = 0
    while (offset < len) {
      val bytesRead = input.read(payload, offset, len - offset)
      if (bytesRead == -1) throw new RuntimeException("Connection closed while reading payload")
      offset += bytesRead
    }

    // Unmask if needed
    if (masked) {
      payload.zipWithIndex.foreach { case (byte, i) =>
        payload(i) = (byte ^ maskingKey(i % 4)).toByte
      }
    }

    payload
  }
}
