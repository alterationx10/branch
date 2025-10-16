package spider.server

import dev.alteration.branch.spider.server.{
  SocketSpiderApp,
  RequestHandler,
  Request,
  Response
}
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*
import dev.alteration.branch.spider.websocket.{
  WebSocketHandler,
  WebSocketConnection
}

/** Example application demonstrating both HTTP and WebSocket on the same
  * SocketServer.
  *
  * This shows how to mix HTTP REST APIs with WebSocket real-time connections.
  *
  * Test HTTP endpoints:
  *   curl http://localhost:9000/
  *   curl http://localhost:9000/api/status
  *   curl http://localhost:9000/api/users
  *
  * Test WebSocket endpoints:
  *   # Using wscat (npm install -g wscat)
  *   wscat -c ws://localhost:9000/ws/echo
  *   wscat -c ws://localhost:9000/ws/chat
  *
  * Or use JavaScript in browser console:
  *   const ws = new WebSocket('ws://localhost:9000/ws/echo');
  *   ws.onmessage = (e) => console.log('Received:', e.data);
  *   ws.onopen = () => ws.send('Hello WebSocket!');
  */
object MixedHttpWebSocketExample extends SocketSpiderApp {

  // ==================== HTTP Handlers ====================

  val homeHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      html"""
      <!DOCTYPE html>
      <html>
      <head>
        <title>Mixed HTTP + WebSocket Server</title>
        <style>
          body { font-family: system-ui; max-width: 800px; margin: 40px auto; padding: 0 20px; }
          h1 { color: #333; }
          .section { margin: 30px 0; padding: 20px; background: #f5f5f5; border-radius: 8px; }
          code { background: #e0e0e0; padding: 2px 6px; border-radius: 3px; }
          button { padding: 8px 16px; margin: 5px; cursor: pointer; }
          #output { margin-top: 10px; padding: 10px; background: white; border: 1px solid #ddd; min-height: 100px; }
        </style>
      </head>
      <body>
        <h1>Mixed HTTP + WebSocket Server</h1>

        <div class="section">
          <h2>HTTP Endpoints</h2>
          <ul>
            <li><a href="/api/status">/api/status</a> - Server status</li>
            <li><a href="/api/users">/api/users</a> - User list</li>
          </ul>
        </div>

        <div class="section">
          <h2>WebSocket Demo</h2>
          <button onclick="connectEcho()">Connect to Echo</button>
          <button onclick="connectChat()">Connect to Chat</button>
          <button onclick="sendMessage()">Send Message</button>
          <button onclick="disconnect()">Disconnect</button>
          <div id="output"></div>
        </div>

        <script>
          let ws = null;
          const output = document.getElementById('output');

          function log(msg) {
            output.innerHTML += msg + '<br>';
            output.scrollTop = output.scrollHeight;
          }

          function connectEcho() {
            if (ws) ws.close();
            ws = new WebSocket('ws://localhost:9000/ws/echo');
            ws.onopen = () => log('✅ Connected to Echo WebSocket');
            ws.onmessage = (e) => log('📨 Received: ' + e.data);
            ws.onclose = () => log('❌ Disconnected');
            ws.onerror = (e) => log('❌ Error: ' + e);
          }

          function connectChat() {
            if (ws) ws.close();
            ws = new WebSocket('ws://localhost:9000/ws/chat');
            ws.onopen = () => log('✅ Connected to Chat WebSocket');
            ws.onmessage = (e) => log('📨 ' + e.data);
            ws.onclose = () => log('❌ Disconnected');
            ws.onerror = (e) => log('❌ Error: ' + e);
          }

          function sendMessage() {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
              log('❌ Not connected');
              return;
            }
            const msg = 'Hello at ' + new Date().toLocaleTimeString();
            ws.send(msg);
            log('📤 Sent: ' + msg);
          }

          function disconnect() {
            if (ws) {
              ws.close();
              ws = null;
            }
          }
        </script>
      </body>
      </html>
      """
    }
  }

  val statusHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      json"""
      {
        "status": "ok",
        "server": "SocketServer",
        "features": ["HTTP", "WebSocket"],
        "version": "1.0.0",
        "timestamp": ${System.currentTimeMillis()}
      }
      """
    }
  }

  val usersHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      json"""
      {
        "users": [
          {"id": 1, "name": "Alice", "status": "online"},
          {"id": 2, "name": "Bob", "status": "away"},
          {"id": 3, "name": "Charlie", "status": "offline"}
        ],
        "total": 3
      }
      """
    }
  }

  // ==================== WebSocket Handlers ====================

  /** Echo WebSocket: echoes back any message received */
  val echoHandler = new WebSocketHandler {
    override def onConnect(connection: WebSocketConnection): Unit = {
      println("Echo WebSocket: Client connected")
      connection.sendText("Welcome to Echo WebSocket! Send me a message.")
    }

    override def onMessage(
        connection: WebSocketConnection,
        message: String
    ): Unit = {
      println(s"Echo: Received '$message'")
      connection.sendText(s"Echo: $message")
    }

    override def onBinary(
        connection: WebSocketConnection,
        data: Array[Byte]
    ): Unit = {
      println(s"Echo: Received ${data.length} bytes")
      connection.sendBinary(data)
    }

    override def onClose(
        connection: WebSocketConnection,
        statusCode: Option[Int],
        reason: String
    ): Unit = {
      println(s"Echo: Client disconnected (${statusCode.getOrElse("no code")})")
    }

    override def onError(
        connection: WebSocketConnection,
        error: Throwable
    ): Unit = {
      println(s"Echo: Error - ${error.getMessage}")
    }
  }

  /** Chat WebSocket: simulates a chat room with server messages */
  val chatHandler = new WebSocketHandler {
    private var messageCount = 0

    override def onConnect(connection: WebSocketConnection): Unit = {
      println("Chat WebSocket: Client connected")
      connection.sendText("Welcome to the chat room!")
      connection.sendText("Type anything to chat.")
    }

    override def onMessage(
        connection: WebSocketConnection,
        message: String
    ): Unit = {
      messageCount += 1
      println(s"Chat: Received message #$messageCount: '$message'")

      // Send acknowledgment
      connection.sendText(s"[Server] Message #$messageCount received")

      // Send a response based on content
      if (message.toLowerCase.contains("hello")) {
        connection.sendText("[Server] Hello! How are you?")
      } else if (message.toLowerCase.contains("bye")) {
        connection.sendText("[Server] Goodbye! Thanks for chatting.")
        connection.close(Some(1000), "Normal closure")
      } else {
        connection.sendText(s"[Server] You said: $message")
      }
    }

    override def onClose(
        connection: WebSocketConnection,
        statusCode: Option[Int],
        reason: String
    ): Unit = {
      println(
        s"Chat: Client disconnected after $messageCount messages (${statusCode.getOrElse("no code")})"
      )
    }

    override def onError(
        connection: WebSocketConnection,
        error: Throwable
    ): Unit = {
      println(s"Chat: Error - ${error.getMessage}")
    }
  }

  // ==================== Route Configuration ====================

  // HTTP routes
  override val router
      : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, Nil)                       => homeHandler
    case (HttpMethod.GET, "api" :: "status" :: Nil)  => statusHandler
    case (HttpMethod.GET, "api" :: "users" :: Nil)   => usersHandler
  }

  // WebSocket routes
  override val webSocketRouter: Map[String, WebSocketHandler] = Map(
    "/ws/echo" -> echoHandler,
    "/ws/chat" -> chatHandler
  )

  // Port can be customized
  // override val port = 8080
}
