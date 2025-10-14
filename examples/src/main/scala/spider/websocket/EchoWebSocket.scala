package spider.websocket

import dev.alteration.branch.spider.websocket.*

/** A simple WebSocket echo server example.
  *
  * This example demonstrates:
  * - Creating a WebSocket server using SpiderWebSocketApp
  * - Handling WebSocket connections
  * - Receiving and sending text messages
  * - Managing connection lifecycle
  *
  * Run this server and connect with a WebSocket client:
  *
  * From browser console:
  * {{{
  *   const ws = new WebSocket('ws://localhost:9001/');
  *   ws.onmessage = (e) => console.log('Received:', e.data);
  *   ws.send('Hello!');
  * }}}
  *
  * Or use a tool like websocat:
  * {{{
  *   websocat ws://localhost:9001/
  * }}}
  */
object EchoWebSocket {

  def main(args: Array[String]): Unit = {

    // Create a WebSocket handler that echoes messages back
    val echoHandler = new WebSocketHandler {

      override def onConnect(connection: WebSocketConnection): Unit = {
        println(s"New connection")
        connection.sendText("Welcome! I will echo back everything you send.")
      }

      override def onMessage(connection: WebSocketConnection, message: String): Unit = {
        println(s"Received: $message")
        // Echo the message back with a prefix
        connection.sendText(s"Echo: $message")
      }

      override def onBinary(connection: WebSocketConnection, data: Array[Byte]): Unit = {
        println(s"Received binary data (${data.length} bytes)")
        // Echo binary data back
        connection.sendBinary(data)
      }

      override def onClose(
          connection: WebSocketConnection,
          statusCode: Option[Int],
          reason: String
      ): Unit = {
        val status = statusCode.map(c => s"code=$c").getOrElse("no code")
        val msg = if (reason.nonEmpty) s", reason=$reason" else ""
        println(s"Connection closed:($status$msg)")
      }

      override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
        println(s"Error on connection: ${error.getMessage}")
        error.printStackTrace()
      }
    }

    // Create and start the WebSocket server
    val server = new SpiderWebSocketApp {
      override val wsPort = 9001
      override def wsHandler = echoHandler
    }

    println()
    println("WebSocket echo server started on port 9001")
    println()
    println("Connect with:")
    println("  Browser:  const ws = new WebSocket('ws://localhost:9001/')")
    println("  websocat: websocat ws://localhost:9001/")
    println()
    println("Press Ctrl+C to stop")
    println()

    server.main(Array.empty)
  }
}
