# Spider WebSocket Support

Zero-dependency WebSocket implementation for the Spider module, following RFC 6455.

## Features

- ✅ Full WebSocket protocol support (RFC 6455)
- ✅ Automatic handshake handling
- ✅ Frame encoding/decoding
- ✅ Automatic ping/pong handling
- ✅ Message fragmentation support
- ✅ Clean lifecycle callbacks
- ✅ Zero external dependencies

## Quick Start

### Approach 1: Standalone WebSocket Server (Recommended)

The standalone approach is simpler and more reliable.

```scala
import dev.alteration.branch.spider.websocket._
import dev.alteration.branch.macaroni.runtimes.BranchExecutors

given scala.concurrent.ExecutionContext = BranchExecutors.executionContext

// Create your handler
class EchoHandler extends WebSocketHandler {

  override def onConnect(connection: WebSocketConnection): Unit = {
    println("Client connected")
    connection.sendText("Welcome!")
  }

  override def onMessage(connection: WebSocketConnection, message: String): Unit = {
    println(s"Received: $message")
    connection.sendText(s"Echo: $message")
  }

  override def onClose(
    connection: WebSocketConnection,
    statusCode: Option[Int],
    reason: String
  ): Unit = {
    println(s"Connection closed: ${statusCode.getOrElse("unknown")}")
  }
}

// Start the server
object MyWebSocketServer {
  def main(args: Array[String]): Unit = {
    val server = new WebSocketServer(9000, new EchoHandler())
    println("WebSocket server running on ws://localhost:9000/")
    server.start()
  }
}
```

### Approach 2: HttpServer Integration (Requires JVM Flags)

**Note**: Due to Java module system restrictions, this approach requires special JVM flags:
```bash
--add-opens jdk.httpserver/sun.net.httpserver=ALL-UNNAMED
```

```scala
import dev.alteration.branch.spider.server.SpiderApp
import dev.alteration.branch.spider.websocket._

object MyWebSocketServer extends SpiderApp {

  val echoHandler = new EchoHandler()
  server.createContext("/echo", WebSocketHandler.toHttpHandler(echoHandler))

  println("WebSocket server running on ws://localhost:9000/echo")
}
```

Run with:
```bash
sbt -J--add-opens -Jjdk.httpserver/sun.net.httpserver=ALL-UNNAMED run
```

### 3. Connect from a Client

```javascript
// In browser console or JavaScript
const ws = new WebSocket('ws://localhost:9000/echo');

ws.onopen = () => {
  console.log('Connected!');
  ws.send('Hello, WebSocket!');
};

ws.onmessage = (event) => {
  console.log('Received:', event.data);
};

ws.onclose = () => {
  console.log('Disconnected');
};
```

## API Reference

### WebSocketHandler

Lifecycle methods to override:

- `onConnect(connection)` - Called when a connection is established
- `onMessage(connection, message)` - Called when a text message is received
- `onBinary(connection, data)` - Called when binary data is received
- `onClose(connection, statusCode, reason)` - Called when the connection closes
- `onError(connection, error)` - Called when an error occurs

### WebSocketConnection

Methods for sending data:

- `sendText(text: String)` - Send a text message
- `sendBinary(data: Array[Byte])` - Send binary data
- `sendPing(data: Array[Byte])` - Send a ping frame
- `sendPong(data: Array[Byte])` - Send a pong frame
- `close(statusCode, reason)` - Close the connection

Connection state:

- `isOpen: Boolean` - Check if connection is open
- `isClosing: Boolean` - Check if connection is closing
- `isClosed: Boolean` - Check if connection is closed

## Examples

See the test directory for complete examples:

- `EchoWebSocketExample` - Simple echo server
- `ChatWebSocketExample` - Broadcast chat room

## Architecture

The WebSocket implementation consists of:

1. **WebSocketOpCode** - Frame opcodes (Text, Binary, Close, Ping, Pong)
2. **WebSocketFrame** - Frame data structure
3. **WebSocketFrameCodec** - Encode/decode frames to/from bytes
4. **WebSocketHandshake** - Handle HTTP Upgrade handshake
5. **WebSocketConnection** - Manage individual connections
6. **WebSocketHandler** - User-facing API for handling events

## Protocol Details

- Supports WebSocket version 13 (RFC 6455)
- Automatic ping/pong keep-alive
- Handles fragmented messages transparently
- Validates frame structure according to RFC 6455
- Proper close handshake (status codes 1000-1015)

## Testing

Run the test suite:

```bash
sbt "branch/testOnly *WebSocketSpec"
```

All core protocol features are tested (20 tests).
