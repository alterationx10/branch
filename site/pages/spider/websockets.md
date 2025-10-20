---
title: Spider WebSocket Support
description: Real-time bidirectional communication with WebSockets
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - websocket
  - realtime
---

# WebSocket Support

Spider includes built-in WebSocket support for real-time bidirectional communication, fully integrated with `SpiderServer`.

## WebSocketHandler

The `WebSocketHandler` trait provides lifecycle methods for handling WebSocket events:

```scala
class EchoWebSocketHandler extends WebSocketHandler {
  override def onConnect(connection: WebSocketConnection): Unit = {
    println("Client connected")
  }

  override def onMessage(connection: WebSocketConnection, message: String): Unit = {
    connection.sendText(s"Echo: $message")
  }

  override def onBinary(connection: WebSocketConnection, data: Array[Byte]): Unit = {
    connection.sendBinary(data)
  }

  override def onClose(
                        connection: WebSocketConnection,
                        statusCode: Option[Int],
                        reason: String
                      ): Unit = {
    println(s"Connection closed: $statusCode - $reason")
  }

  override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
    error.printStackTrace()
  }
}
```

### Lifecycle Methods

- `onConnect`: Called when a WebSocket connection is established
- `onMessage`: Called when a text message is received
- `onBinary`: Called when binary data is received
- `onClose`: Called when the connection is closed
- `onError`: Called when an error occurs

## WebSocketConnection

The `WebSocketConnection` class manages the WebSocket connection after the handshake is complete:

```scala
// Send text messages
connection.sendText("Hello, client!")

// Send binary data
connection.sendBinary(Array[Byte](1, 2, 3))

// Send ping/pong frames
connection.sendPing()
connection.sendPong()

// Close the connection
connection.close(Some(1000), "Normal closure")

// Check connection state
if (connection.isOpen) {
  // Connection is open
}
```

The connection automatically:

- Responds to ping frames with pong
- Handles message fragmentation
- Manages connection state and lifecycle

## Integrating with SpiderServer

WebSockets are integrated directly into `SpiderServer` via the `webSocketRouter` parameter:

```scala
import dev.alteration.branch.spider.server.*

val webSocketRouter: Map[String, WebSocketHandler] = Map(
  "/ws/echo" -> new EchoWebSocketHandler(),
  "/ws/chat" -> new ChatWebSocketHandler()
)

val server = new SpiderServer(
  port = 8080,
  router = httpRouter,
  webSocketRouter = webSocketRouter,
  config = ServerConfig.default
).withShutdownHook()

server.start()
```

## Example: Chat Server

```scala
import scala.collection.mutable

class ChatWebSocketHandler extends WebSocketHandler {
  private val connections = mutable.Set.empty[WebSocketConnection]

  override def onConnect(connection: WebSocketConnection): Unit = {
    connections.synchronized {
      connections += connection
    }
    broadcast(s"User joined. Total users: ${connections.size}")
  }

  override def onMessage(connection: WebSocketConnection, message: String): Unit = {
    broadcast(s"User: $message")
  }

  override def onClose(
                        connection: WebSocketConnection,
                        statusCode: Option[Int],
                        reason: String
                      ): Unit = {
    connections.synchronized {
      connections -= connection
    }
    broadcast(s"User left. Total users: ${connections.size}")
  }

  override def onError(connection: WebSocketConnection, error: Throwable): Unit = {
    println(s"WebSocket error: ${error.getMessage}")
  }

  private def broadcast(message: String): Unit = {
    connections.synchronized {
      connections.foreach { conn =>
        if (conn.isOpen) {
          conn.sendText(message)
        }
      }
    }
  }
}
```

## WebSocket Protocol Details

Spider's WebSocket implementation:

- Follows RFC 6455 (WebSocket Protocol)
- Supports text and binary messages
- Handles ping/pong frames automatically
- Supports message fragmentation
- Validates handshake headers
- Manages connection lifecycle (open, closing, closed)

## Client Example

Here's an example JavaScript client for testing:

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/echo');

ws.onopen = () => {
  console.log('Connected');
  ws.send('Hello, server!');
};

ws.onmessage = (event) => {
  console.log('Received:', event.data);
};

ws.onclose = (event) => {
  console.log('Disconnected:', event.code, event.reason);
};

ws.onerror = (error) => {
  console.error('Error:', error);
};
```

## Next Steps

- Build HTTP applications with the [HTTP Server](/spider/server)
- Create reactive UIs with [WebView](/spider/webview) which uses WebSockets internally
- Make HTTP requests with the [HTTP Client](/spider/client)
