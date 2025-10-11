package dev.alteration.branch.spider.websocket

import java.io.{DataInputStream, InputStream, IOException}
import java.nio.ByteBuffer
import scala.util.Try

/** Codec for encoding and decoding WebSocket frames according to RFC 6455.
  */
object WebSocketFrameCodec {

  /** Encode a WebSocketFrame to bytes
    * @param frame
    *   the frame to encode
    * @return
    *   the encoded frame as a byte array
    */
  def encode(frame: WebSocketFrame): Array[Byte] = {
    val payloadLength = frame.payload.length
    val buffer        = scala.collection.mutable.ArrayBuffer.empty[Byte]

    // Byte 0: FIN (1 bit) + RSV (3 bits) + OpCode (4 bits)
    val byte0 = {
      val finBit = if (frame.fin) 0x80 else 0x00
      (finBit | frame.opCode.code).toByte
    }
    buffer += byte0

    // Byte 1: MASK (1 bit) + Payload Length (7 bits)
    val maskBit = if (frame.masked) 0x80 else 0x00

    if (payloadLength < 126) {
      buffer += (maskBit | payloadLength).toByte
    } else if (payloadLength <= 0xffff) {
      buffer += (maskBit | 126).toByte
      // Extended payload length (16 bits)
      buffer += (payloadLength >> 8).toByte
      buffer += payloadLength.toByte
    } else {
      buffer += (maskBit | 127).toByte
      // Extended payload length (64 bits)
      val longBytes =
        ByteBuffer.allocate(8).putLong(payloadLength.toLong).array()
      buffer ++= longBytes
    }

    // Masking key (if present)
    frame.maskingKey.foreach { key =>
      buffer ++= key
    }

    // Payload data (masked if necessary)
    val payloadData = if (frame.masked && frame.maskingKey.isDefined) {
      val key = frame.maskingKey.get
      frame.payload.zipWithIndex.map { case (byte, i) =>
        (byte ^ key(i % 4)).toByte
      }
    } else {
      frame.payload
    }
    buffer ++= payloadData

    buffer.toArray
  }

  /** Decode a WebSocketFrame from an InputStream
    * @param inputStream
    *   the input stream to read from
    * @return
    *   Success(frame) or Failure(exception)
    */
  def decode(inputStream: InputStream): Try[WebSocketFrame] = {
    val dis = new DataInputStream(inputStream)

    Try {
      // Read byte 0: FIN + RSV + OpCode
      val byte0      = dis.readByte()
      val fin        = (byte0 & 0x80) != 0
      val rsv        = (byte0 & 0x70) >> 4
      val opCodeByte = (byte0 & 0x0f).toByte

      // Validate RSV bits (must be 0 unless extensions are negotiated)
      if (rsv != 0) {
        throw new IOException(s"RSV bits must be 0, got: $rsv")
      }

      // Parse opcode
      val opCode = WebSocketOpCode.fromByte(opCodeByte).getOrElse {
        throw new IOException(s"Invalid opcode: $opCodeByte")
      }

      // Read byte 1: MASK + Payload Length
      val byte1       = dis.readByte()
      val masked      = (byte1 & 0x80) != 0
      val payloadLen7 = byte1 & 0x7f

      // Determine actual payload length
      val payloadLength: Long = payloadLen7 match {
        case len if len < 126 =>
          len.toLong
        case 126              =>
          // Read 16-bit extended payload length
          val len16 = dis.readUnsignedShort()
          len16.toLong
        case 127              =>
          // Read 64-bit extended payload length
          dis.readLong()
        case _                =>
          throw new IOException(
            s"Invalid payload length indicator: $payloadLen7"
          )
      }

      // Validate payload length
      if (payloadLength < 0) {
        throw new IOException(s"Negative payload length: $payloadLength")
      }

      // Control frames must have payload <= 125 bytes
      if (WebSocketOpCode.isControl(opCode) && payloadLength > 125) {
        throw new IOException(
          s"Control frame payload too large: $payloadLength (max 125)"
        )
      }

      // Control frames must not be fragmented
      if (WebSocketOpCode.isControl(opCode) && !fin) {
        throw new IOException("Control frames must not be fragmented")
      }

      // Read masking key (if present)
      val maskingKey: Option[Array[Byte]] = if (masked) {
        val key = new Array[Byte](4)
        dis.readFully(key)
        Some(key)
      } else {
        None
      }

      // Read payload data
      if (payloadLength > Int.MaxValue) {
        throw new IOException(
          s"Payload too large: $payloadLength (max ${Int.MaxValue})"
        )
      }

      val payload = new Array[Byte](payloadLength.toInt)
      dis.readFully(payload)

      WebSocketFrame(
        fin = fin,
        opCode = opCode,
        masked = masked,
        maskingKey = maskingKey,
        payload = payload
      )
    }
  }

  /** Decode a WebSocketFrame from a byte array
    * @param bytes
    *   the byte array to decode
    * @return
    *   Success(frame) or Failure(exception)
    */
  def decode(bytes: Array[Byte]): Try[WebSocketFrame] = {
    decode(new java.io.ByteArrayInputStream(bytes))
  }

}
