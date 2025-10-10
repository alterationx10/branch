package dev.alteration.branch.spider.websocket

import java.security.MessageDigest
import java.util.Base64
import scala.util.Try

/** WebSocket handshake handler according to RFC 6455.
  *
  * The WebSocket handshake involves an HTTP Upgrade request from the client
  * and a 101 Switching Protocols response from the server.
  */
object WebSocketHandshake {

  /** The magic GUID string defined in RFC 6455, section 1.3
    */
  private val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

  /** Compute the Sec-WebSocket-Accept value from a Sec-WebSocket-Key
    *
    * According to RFC 6455, the accept value is computed as:
    * base64(SHA-1(Sec-WebSocket-Key + MAGIC_GUID))
    *
    * @param secWebSocketKey
    *   the Sec-WebSocket-Key from the client request
    * @return
    *   the computed Sec-WebSocket-Accept value
    */
  def computeAcceptKey(secWebSocketKey: String): String = {
    val concatenated = secWebSocketKey + WEBSOCKET_GUID
    val sha1         = MessageDigest.getInstance("SHA-1")
    val hash         = sha1.digest(concatenated.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(hash)
  }

  /** Validate a WebSocket upgrade request
    *
    * Checks that required headers are present:
    * - Upgrade: websocket
    * - Connection: Upgrade (or contains "upgrade")
    * - Sec-WebSocket-Key: present and non-empty
    * - Sec-WebSocket-Version: 13
    *
    * @param headers
    *   the request headers (case-insensitive)
    * @return
    *   Success with Sec-WebSocket-Key if valid, Failure otherwise
    */
  def validateHandshake(headers: Map[String, List[String]]): Try[String] = {
    // Make headers case-insensitive
    val caseInsensitiveHeaders = headers.map { case (k, v) =>
      k.toLowerCase -> v
    }

    def getHeader(name: String): Option[String] = {
      caseInsensitiveHeaders.get(name.toLowerCase).flatMap(_.headOption)
    }

    Try {
      // Check Upgrade header
      val upgrade = getHeader("upgrade").getOrElse {
        throw new IllegalArgumentException("Missing Upgrade header")
      }
      if (upgrade.toLowerCase != "websocket") {
        throw new IllegalArgumentException(
          s"Invalid Upgrade header: $upgrade (expected 'websocket')"
        )
      }

      // Check Connection header (should contain "upgrade")
      val connection = getHeader("connection").getOrElse {
        throw new IllegalArgumentException("Missing Connection header")
      }
      if (!connection.toLowerCase.contains("upgrade")) {
        throw new IllegalArgumentException(
          s"Invalid Connection header: $connection (expected to contain 'upgrade')"
        )
      }

      // Check Sec-WebSocket-Version
      val version = getHeader("sec-websocket-version").getOrElse {
        throw new IllegalArgumentException("Missing Sec-WebSocket-Version header")
      }
      if (version != "13") {
        throw new IllegalArgumentException(
          s"Unsupported WebSocket version: $version (expected 13)"
        )
      }

      // Get Sec-WebSocket-Key
      val key = getHeader("sec-websocket-key").getOrElse {
        throw new IllegalArgumentException("Missing Sec-WebSocket-Key header")
      }
      if (key.trim.isEmpty) {
        throw new IllegalArgumentException("Empty Sec-WebSocket-Key header")
      }

      key
    }
  }

  /** Create the 101 Switching Protocols response headers
    *
    * @param secWebSocketKey
    *   the Sec-WebSocket-Key from the client request
    * @return
    *   the response headers map
    */
  def createHandshakeResponse(secWebSocketKey: String): Map[String, List[String]] = {
    val acceptKey = computeAcceptKey(secWebSocketKey)

    Map(
      "Upgrade"              -> List("websocket"),
      "Connection"           -> List("Upgrade"),
      "Sec-WebSocket-Accept" -> List(acceptKey)
    )
  }

  /** Create the full 101 response as a raw HTTP response string
    *
    * @param secWebSocketKey
    *   the Sec-WebSocket-Key from the client request
    * @return
    *   the raw HTTP response as a byte array
    */
  def createRawHandshakeResponse(secWebSocketKey: String): Array[Byte] = {
    val acceptKey  = computeAcceptKey(secWebSocketKey)
    val response   = s"""HTTP/1.1 101 Switching Protocols\r
Upgrade: websocket\r
Connection: Upgrade\r
Sec-WebSocket-Accept: $acceptKey\r
\r
"""
    response.getBytes("UTF-8")
  }

}
