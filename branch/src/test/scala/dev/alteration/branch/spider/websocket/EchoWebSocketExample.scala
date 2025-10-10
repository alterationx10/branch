package dev.alteration.branch.spider.websocket

import dev.alteration.branch.spider.server.SpiderApp

/** Example WebSocket echo server.
  *
  * This example demonstrates how to create a simple WebSocket server that echoes
  * back any messages it receives.
  *
  * To test this server:
  * 1. Run this application
  * 2. Open a browser console and connect:
  *    ```javascript
  *    const ws = new WebSocket('ws://localhost:9000/echo');
  *    ws.onmessage = (event) => console.log('Received:', event.data);
  *    ws.send('Hello, WebSocket!');
  *    ```
  */
object EchoWebSocketExample extends SpiderApp {

  /** A simple echo WebSocket handler that sends back any message it receives
    */
  class EchoWebSocketHandler extends WebSocketHandler {

    override def onConnect(connection: WebSocketConnection): Unit = {
      println(s"[Echo] Client connected")
      connection.sendText("Welcome to the echo server!")
    }

    override def onMessage(connection: WebSocketConnection, message: String): Unit = {
      println(s"[Echo] Received text: $message")
      connection.sendText(s"Echo: $message")
    }

    override def onBinary(connection: WebSocketConnection, data: Array[Byte]): Unit = {
      println(s"[Echo] Received binary data: ${data.length} bytes")
      connection.sendBinary(data)
    }

    override def onClose(
        connection: WebSocketConnection,
        statusCode: Option[Int],
        reason: String
    ): Unit = {
      println(s"[Echo] Connection closed: ${statusCode.getOrElse("no code")} - $reason")
    }

    override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
      println(s"[Echo] Error: ${error.getMessage}")
      error.printStackTrace()
    }

  }

  // Register the echo WebSocket handler at the /echo path
  val echoHandler = new EchoWebSocketHandler()
  server.createContext("/echo", WebSocketHandler.toHttpHandler(echoHandler))

  println("Echo WebSocket server started on ws://localhost:9000/echo")
  println("Press Ctrl+C to stop")

}

/** Example WebSocket chat server.
  *
  * This example demonstrates a simple chat room where all messages are
  * broadcast to all connected clients.
  */
object ChatWebSocketExample extends SpiderApp {

  override val port = 9001

  /** A chat WebSocket handler that broadcasts messages to all connected clients
    */
  class ChatWebSocketHandler extends WebSocketHandler {

    private val connections = scala.collection.mutable.Set.empty[WebSocketConnection]
    private val lock        = new Object()

    override def onConnect(connection: WebSocketConnection): Unit = {
      lock.synchronized {
        connections += connection
      }
      println(s"[Chat] Client connected. Total clients: ${connections.size}")
      broadcast(s"A new user joined the chat. Total users: ${connections.size}")
    }

    override def onMessage(connection: WebSocketConnection, message: String): Unit = {
      println(s"[Chat] Broadcasting message: $message")
      broadcast(message)
    }

    override def onClose(
        connection: WebSocketConnection,
        statusCode: Option[Int],
        reason: String
    ): Unit = {
      lock.synchronized {
        connections -= connection
      }
      println(s"[Chat] Client disconnected. Total clients: ${connections.size}")
      broadcast(s"A user left the chat. Total users: ${connections.size}")
    }

    override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
      println(s"[Chat] Error: ${error.getMessage}")
      lock.synchronized {
        connections -= connection
      }
    }

    /** Broadcast a message to all connected clients
      */
    private def broadcast(message: String): Unit = {
      lock.synchronized {
        connections.foreach { conn =>
          if (conn.isOpen) {
            conn.sendText(message).failed.foreach { error =>
              println(s"[Chat] Failed to send to client: ${error.getMessage}")
            }
          }
        }
      }
    }

  }

  // Register the chat WebSocket handler at the /chat path
  val chatHandler = new ChatWebSocketHandler()
  server.createContext("/chat", WebSocketHandler.toHttpHandler(chatHandler))

  println("Chat WebSocket server started on ws://localhost:9001/chat")
  println("Press Ctrl+C to stop")

}
