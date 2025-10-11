# Branch WebView Examples

This directory contains complete, runnable examples for the Branch WebView library.

## Simple Counter Example (Recommended for Getting Started)

The easiest way to get started with WebView. Just 3 lines of code!

### Running the Example

```bash
# From the project root
sbt "runMain dev.alteration.branch.spider.webview.examples.SimpleCounterExample"
```

Then open your browser to: http://localhost:8080/counter

This example uses the enhanced `WebViewServer` API that automatically:
- Serves HTML pages for each WebView route
- Loads the `webview.js` client library
- Sets up WebSocket connections

```scala
val server = WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withHtmlPages()  // Enable automatic HTML serving
  .start(port = 8080)
```

## Full Counter Example (Manual Setup)

A complete example showing how to manually configure HTTP and WebSocket routes.
Use this when you need more control over the server setup.

### Running the Example

```bash
# From the project root
sbt "runMain dev.alteration.branch.spider.webview.examples.CounterExample"
```

Then open your browser to: http://localhost:8080/

### What's Happening

1. **HTTP Server** serves the initial HTML page at `/`
2. **Resource Server** serves `webview.js` from `src/main/resources/spider/webview/webview.js` at `/js/webview.js`
3. **WebSocket** connection is established to `/ws/counter`
4. **WebView** renders on the server, sends HTML updates to the client
5. **Client events** (button clicks) are sent to the server over WebSocket
6. **State updates** trigger re-render and DOM updates

### File Structure

```
CounterExample.scala           # Main application
CounterWebView.scala           # WebView implementation
src/main/resources/
  └── spider/
      └── webview/
          └── webview.js       # Client-side JavaScript
```

## Quick Start (Simplified API)

The easiest way to create a WebView application:

```scala
// 1. Define your WebView
case class MyState(count: Int)
sealed trait MyEvent derives EventCodec
case object Increment extends MyEvent

class MyWebView extends WebView[MyState, MyEvent] {
  override def mount(params: Map[String, String], session: Session): MyState =
    MyState(0)

  override def handleEvent(event: MyEvent, state: MyState): MyState = event match {
    case Increment => state.copy(count = state.count + 1)
  }

  override def render(state: MyState): String =
    s"""<div>
      <h1>Count: ${state.count}</h1>
      <button wv-click="Increment">+</button>
    </div>"""
}

// 2. Start the server
object MyApp extends App {
  WebViewServer()
    .withRoute("/myview", new MyWebView())
    .withHtmlPages()
    .start(port = 8080)
}
```

That's it! Visit http://localhost:8080/myview and your WebView is live.

The `.withHtmlPages()` method automatically:
- Creates an HTML page at `/myview` that loads the WebView
- Serves the `webview.js` client library at `/js/webview.js`
- Sets up the WebSocket connection at `/myview` (same path)

## Creating Your Own WebView (Manual Setup)

If you need more control, you can manually configure the HTTP and WebSocket routes:

### 1. Define Your WebView

```scala
// Define state
case class MyState(data: String)

// Define events (use sealed trait for type safety)
sealed trait MyEvent derives EventCodec
case class UpdateData(newData: String) extends MyEvent

// Implement WebView
class MyWebView extends WebView[MyState, MyEvent] {
  override def mount(params: Map[String, String], session: Session): MyState =
    MyState(data = params.getOrElse("initial", "Hello"))

  override def handleEvent(event: MyEvent, state: MyState): MyState = event match {
    case UpdateData(newData) => state.copy(data = newData)
  }

  override def render(state: MyState): String =
    s"""<div>
      <h1>${state.data}</h1>
      <button wv-click="UpdateData">Update</button>
    </div>"""
}
```

### 2. Set Up the Server

```scala
import dev.alteration.branch.spider.http.{HybridServer, ResourceServer}
import dev.alteration.branch.spider.webview.*

given ExecutionContext = ...
val actorSystem = ActorSystem()

// Create handlers
given EventCodec[MyEvent] = EventCodec.derived
val webViewHandler = WebViewHandler[MyState, MyEvent](
  actorSystem = actorSystem,
  webViewFactory = () => new MyWebView()
)

val pageHandler = WebViewPageHandler(
  wsUrl = "ws://localhost:8080/ws/my-view",
  title = "My WebView"
)

val resourceServer = ResourceServer("spider/webview")

// Create server
val server = HybridServer(
  port = 8080,
  httpRoutes = Map(
    "/" -> pageHandler,
    "/js/webview.js" -> resourceServer
  ),
  wsRoutes = Map(
    "/ws/my-view" -> webViewHandler
  )
)

server.start()
```

### 3. Client-Side Events

Use these attributes in your HTML to bind events:

- `wv-click="EventName"` - Handle clicks
- `wv-change="EventName"` - Handle input changes
- `wv-submit="EventName"` - Handle form submissions
- `wv-keyup="EventName"` - Handle keyboard events

The client automatically sends events to the server with the event name and any input values.

## Advanced Usage

### Custom HTML Page

Instead of using `WebViewPageHandler`, create your own `HttpHandler`:

```scala
class CustomPageHandler extends HttpHandler {
  override def handleGet(path: String, headers: Map[String, List[String]], socket: Socket): Unit = {
    val html = """<!DOCTYPE html>
    <html>
    <head><title>Custom Page</title></head>
    <body>
      <div id="app"></div>
      <script src="/js/webview.js"></script>
      <script>
        new BranchWebView('ws://localhost:8080/ws/my-view', {
          rootSelector: '#app',
          debug: true
        });
      </script>
    </body>
    </html>"""
    HttpResponse.ok(socket, html)
  }
}
```

### Multiple WebViews

You can serve multiple WebViews on different paths:

```scala
val server = HybridServer(
  port = 8080,
  httpRoutes = Map(
    "/counter" -> WebViewPageHandler("ws://localhost:8080/ws/counter", "Counter"),
    "/todo" -> WebViewPageHandler("ws://localhost:8080/ws/todo", "Todo List"),
    "/js/webview.js" -> resourceServer
  ),
  wsRoutes = Map(
    "/ws/counter" -> counterHandler,
    "/ws/todo" -> todoHandler
  )
)
```

### Serving Static Assets

Use `ResourceServer` to serve entire directories:

```scala
// Serve all files from src/main/resources/static/
val staticServer = ResourceServer("static")

val server = HybridServer(
  port = 8080,
  httpRoutes = Map(
    "/static" -> staticServer  // Serves /static/css/app.css, /static/js/app.js, etc.
  ),
  wsRoutes = ...
)
```

## Directory Structure

Recommended project structure:

```
src/main/
├── scala/
│   └── your/package/
│       ├── MyWebView.scala
│       └── MyApp.scala
└── resources/
    ├── spider/
    │   └── webview/
    │       └── webview.js        # WebView client library
    └── static/                   # Your static assets
        ├── css/
        ├── js/
        └── images/
```

## Tips

1. **Development Mode**: Enable debug logging with `debug = true` in `WebViewPageHandler`
2. **Hot Reload**: Changes to your WebView's `render()` method take effect on the next state update
3. **Resource Loading**: Make sure resources are in `src/main/resources/` so they're included in the classpath
4. **CORS**: The `HybridServer` handles same-origin requests; for cross-origin, you'll need to add CORS headers

## Troubleshooting

### "Resource not found: spider/webview/webview.js"

Make sure `webview.js` exists at:
```
src/main/resources/spider/webview/webview.js
```

### WebSocket Connection Fails

Check that:
1. The WebSocket URL matches your server's host and port
2. The WebSocket path is registered in `wsRoutes`
3. The server is running and listening on the correct port

### Events Not Working

1. Make sure your events derive `EventCodec`: `sealed trait MyEvent derives EventCodec`
2. Check that `given EventCodec[MyEvent]` is in scope when creating the handler
3. Event names in HTML must match case class names exactly
