---
title: Spider
description: A layer over the built-in Java HttpServer
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - server
  - client
  - websocket
---

# Spider

*Oh, what a tangled web we weave when first we practice http*

Spider is a lightweight HTTP framework built on top of Java's built-in `HttpServer`. It provides both server and client
functionality with a clean Scala API.

## Server Components

### RequestHandler

The core building block is the `RequestHandler[I,O]` trait which handles converting HTTP requests into your input model
`I` and converting your output model `O` back into HTTP responses:

```scala
trait RequestHandler[I, O](using
                           requestDecoder: Conversion[Array[Byte], I],
                           responseEncoder: Conversion[O, Array[Byte]]
                          ) {
  def handle(request: Request[I]): Response[O]
}
```

Some common conversions are provided via `RequestHandler.given`, including:

- `Array[Byte] <-> Array[Byte]`
- `Array[Byte] <-> Unit`
- `Array[Byte] <-> String`

Here's a simple example handler:

```scala
case class GreeterHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(200, "Hello!")
  }
}
```

### ContextHandler

Routes are defined using `ContextHandler`s which map HTTP methods and paths to specific `RequestHandler`s:

```scala
val handler = new ContextHandler("/api") {
  override val contextRouter = {
    case HttpMethod.GET -> ("greet" :: Nil) => GreeterHandler()
    case HttpMethod.GET -> ("echo" :: msg :: Nil) => EchoHandler(msg)
  }
}
```

ContextHandlers support:

- Path-based routing with pattern matching via String Lists
- Request filters/middleware
- Authentication via `Authenticator`
- Default 404 handling

### FileContextHandler

For serving static files, Spider provides a `FileContextHandler`:

```scala
val staticFiles = FileContextHandler(
  rootFilePath = Path.of("public"),
  contextPath = "/static"
)
```

This will automatically:

- Serve files from the specified root directory
- Handle directory indexes (index.html)
- Set appropriate content types based on file extensions
- Support default file extensions (.html, .htm)

### SpiderApp

The `SpiderApp` trait provides a simple way to bootstrap your server:

```scala
object MyServer extends SpiderApp {
  val port = 8080 // Default is 9000

  val api = new ContextHandler("/api") {
    override val contextRouter = {
      case HttpMethod.GET -> ("hello" :: Nil) => GreeterHandler()
    }
  }

  ContextHandler.registerHandler(api)
}
```

## Client

Spider also includes a client API built on `java.net.http.HttpClient`:

```scala
val client = Client.build()

val request = ClientRequest.build(
  uri"http://localhost:8080/api/hello",
  _.GET()
)

val response = client.send(request, HttpResponse.BodyHandlers.ofString())
```

The client supports:

- URI string interpolation
- Content type helpers
- Custom request/response body handlers
- All standard HTTP methods

## Response Helpers

Spider provides some helpful response builders:

```scala
// HTML response
html"""
<h1>Hello $name!</h1>
"""

// JSON response  
json"""
{
  "message": "Hello, $name!"
}
"""

// With headers
Response(200, "Hello")
  .withHeader("X-Custom" -> "value")
  .withContentType(ContentType.json)
```

## Content Types

Spider includes a comprehensive `ContentType` enum covering common MIME types. The content type is automatically set
based on file extensions when using `FileContextHandler`.

## Examples

See the test files for more examples of:

- Route handling
- File serving
- Client usage
- JSON responses
- Authentication
- Filters

## WebSocket Support

Spider includes built-in WebSocket support for real-time bidirectional communication. Unfortunately, this is not
integrated with the `com.sun.net.httpserver.HttpServer`, so runs separate from a `SpiderApp`

### WebSocketHandler

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

Lifecycle methods:

- `onConnect`: Called when a WebSocket connection is established
- `onMessage`: Called when a text message is received
- `onBinary`: Called when binary data is received
- `onClose`: Called when the connection is closed
- `onError`: Called when an error occurs

### WebSocketConnection

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

### WebSocketServer

Create a standalone WebSocket server on a specific port:

```scala
val handler = new EchoWebSocketHandler()
val server = new WebSocketServer(9001, handler)
server.start()
```

Or start it in the background:

```scala
val server = WebSocketServer.start(9001, handler)
// Server runs in background thread
```

### SpiderWebSocketApp

The `SpiderWebSocketApp` trait provides an easy way to bootstrap a WebSocket server:

```scala
object MyWebSocketApp extends SpiderWebSocketApp {
  override val wsPort = 9001 // Default is 9001

  override def wsHandler = new WebSocketHandler {
    override def onConnect(connection: WebSocketConnection): Unit = {
      connection.sendText("Welcome!")
    }

    override def onMessage(connection: WebSocketConnection, message: String): Unit = {
      // Broadcast or process message
      connection.sendText(s"Received: $message")
    }
  }
}
```

This automatically:

- Starts the WebSocket server on the specified port
- Handles the WebSocket handshake (HTTP upgrade)
- Manages connections and message routing
- Adds shutdown hooks for graceful termination

### WebSocket Protocol Details

Spider's WebSocket implementation:

- Follows RFC 6455 (WebSocket Protocol)
- Supports text and binary messages
- Handles ping/pong frames automatically
- Supports message fragmentation
- Validates handshake headers
- Manages connection lifecycle (open, closing, closed)

## Other Libraries

If you like Spider, you should check out [Tapir](https://tapir.softwaremill.com/en/latest/)