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

## Counter Example with Params

An example showing how to use initial params and DevMode.
Use this to understand more advanced configuration options.

### Running the Example

```bash
# From the project root
sbt "runMain dev.alteration.branch.spider.webview.examples.CounterExample"
```

Then open your browser to: http://localhost:8080/counter

The counter will start at 10 instead of 0 because of the `params = Map("initial" -> "10")` configuration.

### What's Different

1. **Initial params** - Pass URL query parameters to `mount()`
2. **DevMode enabled** - Adds DevTools and debug logging
3. **Same simple API** - Still uses `.withHtmlPages()` for automatic setup

### File Structure

```
SimpleCounterExample.scala     # Simplest example
CounterExample.scala           # With params and devmode
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

## Creating Your Own WebView

Now that you've seen the examples, here's how to create your own:

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

### 2. Start the Server

```scala
import dev.alteration.branch.spider.webview.*

// Simple approach - let WebViewServer handle everything
WebViewServer()
  .withRoute("/myview", new MyWebView())
  .withHtmlPages()  // Automatically serves HTML and JS
  .start(port = 8080)

// Or with more options
WebViewServer()
  .withRoute("/myview", new MyWebView(), params = Map("initial" -> "Hello!"))
  .withHtmlPages()
  .withDevMode(true)  // Enable debug mode
  .start(port = 8080)
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
WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withRoute("/todo", new TodoWebView())
  .withRoute("/chat", new ChatWebView())
  .withHtmlPages()
  .start(port = 8080)

// Each route automatically gets:
// - HTML page at the route path (e.g., /counter)
// - WebSocket connection at the same path
// - Shared JavaScript at /js/webview.js
```

### Serving Static Assets

Use `ResourceServer` with `.withHttpRoute()` to serve entire directories:

```scala
// Serve all files from src/main/resources/static/
val staticServer = ResourceServer("static")

WebViewServer()
  .withRoute("/app", new MyWebView())
  .withHttpRoute("/static", staticServer)  // Serves /static/css/app.css, etc.
  .withHtmlPages()
  .start(port = 8080)
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

1. **Development Mode**: Enable debug logging with `.withDevMode(true)`
2. **Hot Reload**: Changes to your WebView's `render()` method take effect on the next state update
3. **Resource Loading**: Make sure resources are in `src/main/resources/` so they're included in the classpath
4. **CORS**: WebViewServer handles same-origin requests; for cross-origin, you'll need to add CORS headers

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
