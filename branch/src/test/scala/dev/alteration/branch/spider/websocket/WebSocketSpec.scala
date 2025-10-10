package dev.alteration.branch.spider.websocket

import munit.FunSuite

import scala.util.{Failure, Success}

class WebSocketSpec extends FunSuite {

  test("WebSocketOpCode.fromByte should parse valid opcodes") {
    assertEquals(WebSocketOpCode.fromByte(0x0), Some(WebSocketOpCode.Continuation))
    assertEquals(WebSocketOpCode.fromByte(0x1), Some(WebSocketOpCode.Text))
    assertEquals(WebSocketOpCode.fromByte(0x2), Some(WebSocketOpCode.Binary))
    assertEquals(WebSocketOpCode.fromByte(0x8), Some(WebSocketOpCode.Close))
    assertEquals(WebSocketOpCode.fromByte(0x9), Some(WebSocketOpCode.Ping))
    assertEquals(WebSocketOpCode.fromByte(0xA), Some(WebSocketOpCode.Pong))
  }

  test("WebSocketOpCode.fromByte should return None for invalid opcodes") {
    assertEquals(WebSocketOpCode.fromByte(0x3), None)
    assertEquals(WebSocketOpCode.fromByte(0xF), None)
  }

  test("WebSocketOpCode.isControl should identify control frames") {
    assert(WebSocketOpCode.isControl(WebSocketOpCode.Close))
    assert(WebSocketOpCode.isControl(WebSocketOpCode.Ping))
    assert(WebSocketOpCode.isControl(WebSocketOpCode.Pong))
    assert(!WebSocketOpCode.isControl(WebSocketOpCode.Text))
    assert(!WebSocketOpCode.isControl(WebSocketOpCode.Binary))
    assert(!WebSocketOpCode.isControl(WebSocketOpCode.Continuation))
  }

  test("WebSocketFrame.text should create a text frame") {
    val frame = WebSocketFrame.text("Hello")
    assert(frame.fin)
    assertEquals(frame.opCode, WebSocketOpCode.Text)
    assert(!frame.masked)
    assertEquals(frame.maskingKey, None)
    assertEquals(frame.payloadAsString, "Hello")
  }

  test("WebSocketFrame.binary should create a binary frame") {
    val data  = Array[Byte](1, 2, 3, 4, 5)
    val frame = WebSocketFrame.binary(data)
    assert(frame.fin)
    assertEquals(frame.opCode, WebSocketOpCode.Binary)
    assert(!frame.masked)
    assertEquals(frame.maskingKey, None)
    assert(frame.payload.sameElements(data))
  }

  test("WebSocketFrame.close should create a close frame with status code") {
    val frame = WebSocketFrame.close(Some(1000), "Normal closure")
    assert(frame.fin)
    assertEquals(frame.opCode, WebSocketOpCode.Close)

    // Verify payload contains status code
    val payload = frame.payload
    assert(payload.length >= 2)
    val statusCode = ((payload(0) & 0xFF) << 8) | (payload(1) & 0xFF)
    assertEquals(statusCode, 1000)

    val reason = new String(payload.drop(2), "UTF-8")
    assertEquals(reason, "Normal closure")
  }

  test("WebSocketFrame.ping should create a ping frame") {
    val frame = WebSocketFrame.ping()
    assert(frame.fin)
    assertEquals(frame.opCode, WebSocketOpCode.Ping)
    assertEquals(frame.payload.length, 0)
  }

  test("WebSocketFrame.pong should create a pong frame") {
    val data  = Array[Byte](1, 2, 3)
    val frame = WebSocketFrame.pong(data)
    assert(frame.fin)
    assertEquals(frame.opCode, WebSocketOpCode.Pong)
    assert(frame.payload.sameElements(data))
  }

  test("WebSocketFrameCodec should encode and decode a simple text frame") {
    val original = WebSocketFrame.text("Hello, WebSocket!")
    val encoded  = WebSocketFrameCodec.encode(original)
    val decoded  = WebSocketFrameCodec.decode(encoded)

    decoded match {
      case Success(frame) =>
        assertEquals(frame.fin, original.fin)
        assertEquals(frame.opCode, original.opCode)
        assertEquals(frame.masked, original.masked)
        assertEquals(frame.payloadAsString, original.payloadAsString)

      case Failure(error) =>
        fail(s"Failed to decode frame: ${error.getMessage}")
    }
  }

  test("WebSocketFrameCodec should encode and decode a masked frame") {
    val maskingKey = Array[Byte](0x12, 0x34, 0x56, 0x78)
    val original = WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Text,
      masked = true,
      maskingKey = Some(maskingKey),
      payload = "Test".getBytes("UTF-8")
    )

    val encoded = WebSocketFrameCodec.encode(original)
    val decoded = WebSocketFrameCodec.decode(encoded)

    decoded match {
      case Success(frame) =>
        assert(frame.masked)
        assertEquals(frame.payloadAsString, "Test")

      case Failure(error) =>
        fail(s"Failed to decode masked frame: ${error.getMessage}")
    }
  }

  test(
    "WebSocketFrameCodec should encode and decode a frame with extended payload length (16-bit)"
  ) {
    val longText = "x" * 200 // More than 125 bytes
    val original = WebSocketFrame.text(longText)
    val encoded  = WebSocketFrameCodec.encode(original)
    val decoded  = WebSocketFrameCodec.decode(encoded)

    decoded match {
      case Success(frame) =>
        assertEquals(frame.payloadAsString, longText)

      case Failure(error) =>
        fail(s"Failed to decode extended frame: ${error.getMessage}")
    }
  }

  test("WebSocketFrameCodec should reject control frames with payload > 125 bytes") {
    val longPayload = new Array[Byte](126)
    val invalidFrame = WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Ping,
      masked = false,
      maskingKey = None,
      payload = longPayload
    )

    val encoded = WebSocketFrameCodec.encode(invalidFrame)
    val decoded = WebSocketFrameCodec.decode(encoded)

    decoded match {
      case Success(_) =>
        fail("Should have rejected control frame with payload > 125 bytes")

      case Failure(error) =>
        assert(error.getMessage.contains("Control frame payload too large"))
    }
  }

  test("WebSocketFrameCodec should reject fragmented control frames") {
    val invalidFrame = WebSocketFrame(
      fin = false,
      opCode = WebSocketOpCode.Ping,
      masked = false,
      maskingKey = None,
      payload = Array.empty
    )

    val encoded = WebSocketFrameCodec.encode(invalidFrame)
    val decoded = WebSocketFrameCodec.decode(encoded)

    decoded match {
      case Success(_) =>
        fail("Should have rejected fragmented control frame")

      case Failure(error) =>
        assert(error.getMessage.contains("Control frames must not be fragmented"))
    }
  }

  test("WebSocketHandshake.computeAcceptKey should compute correct accept key") {
    // Example from RFC 6455
    val clientKey = "dGhlIHNhbXBsZSBub25jZQ=="
    val expected  = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
    val actual    = WebSocketHandshake.computeAcceptKey(clientKey)
    assertEquals(actual, expected)
  }

  test("WebSocketHandshake.validateHandshake should accept valid handshake") {
    val headers = Map(
      "Upgrade"                -> List("websocket"),
      "Connection"             -> List("Upgrade"),
      "Sec-WebSocket-Key"      -> List("dGhlIHNhbXBsZSBub25jZQ=="),
      "Sec-WebSocket-Version"  -> List("13")
    )

    WebSocketHandshake.validateHandshake(headers) match {
      case Success(key) =>
        assertEquals(key, "dGhlIHNhbXBsZSBub25jZQ==")

      case Failure(error) =>
        fail(s"Valid handshake rejected: ${error.getMessage}")
    }
  }

  test("WebSocketHandshake.validateHandshake should reject missing Upgrade header") {
    val headers = Map(
      "Connection"            -> List("Upgrade"),
      "Sec-WebSocket-Key"     -> List("dGhlIHNhbXBsZSBub25jZQ=="),
      "Sec-WebSocket-Version" -> List("13")
    )

    WebSocketHandshake.validateHandshake(headers) match {
      case Success(_) =>
        fail("Should have rejected handshake with missing Upgrade header")

      case Failure(error) =>
        assert(error.getMessage.contains("Missing Upgrade header"))
    }
  }

  test("WebSocketHandshake.validateHandshake should reject invalid Upgrade header") {
    val headers = Map(
      "Upgrade"               -> List("http"),
      "Connection"            -> List("Upgrade"),
      "Sec-WebSocket-Key"     -> List("dGhlIHNhbXBsZSBub25jZQ=="),
      "Sec-WebSocket-Version" -> List("13")
    )

    WebSocketHandshake.validateHandshake(headers) match {
      case Success(_) =>
        fail("Should have rejected handshake with invalid Upgrade header")

      case Failure(error) =>
        assert(error.getMessage.contains("Invalid Upgrade header"))
    }
  }

  test("WebSocketHandshake.validateHandshake should reject unsupported version") {
    val headers = Map(
      "Upgrade"               -> List("websocket"),
      "Connection"            -> List("Upgrade"),
      "Sec-WebSocket-Key"     -> List("dGhlIHNhbXBsZSBub25jZQ=="),
      "Sec-WebSocket-Version" -> List("12")
    )

    WebSocketHandshake.validateHandshake(headers) match {
      case Success(_) =>
        fail("Should have rejected handshake with unsupported version")

      case Failure(error) =>
        assert(error.getMessage.contains("Unsupported WebSocket version"))
    }
  }

  test("WebSocketHandshake.validateHandshake should be case-insensitive") {
    val headers = Map(
      "upgrade"                -> List("WebSocket"),
      "connection"             -> List("upgrade"),
      "sec-websocket-key"      -> List("dGhlIHNhbXBsZSBub25jZQ=="),
      "sec-websocket-version"  -> List("13")
    )

    WebSocketHandshake.validateHandshake(headers) match {
      case Success(key) =>
        assertEquals(key, "dGhlIHNhbXBsZSBub25jZQ==")

      case Failure(error) =>
        fail(s"Case-insensitive handshake rejected: ${error.getMessage}")
    }
  }

  test("WebSocketFrame.unmaskedPayload should unmask masked payload") {
    val maskingKey = Array[Byte](0x12, 0x34, 0x56, 0x78)
    val plaintext  = "Test"
    val masked     = plaintext.getBytes("UTF-8").zipWithIndex.map { case (b, i) =>
      (b ^ maskingKey(i % 4)).toByte
    }

    val frame = WebSocketFrame(
      fin = true,
      opCode = WebSocketOpCode.Text,
      masked = true,
      maskingKey = Some(maskingKey),
      payload = masked
    )

    assertEquals(frame.payloadAsString, plaintext)
  }

}
