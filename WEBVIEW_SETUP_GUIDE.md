# WebView Setup Guide

This guide explains how to serve your WebView library with HTML pages and JavaScript.

## What Was Built

We've created a complete infrastructure for serving WebView applications with automatic HTML and JavaScript delivery:

### 1. HTTP Layer (`spider/http/`)

- **`HttpHandler`** - Trait for handling HTTP GET requests
- **`HttpResponse`** - Helper methods for sending HTTP responses (200 OK, 404 Not Found, etc.)
- **`ResourceServer`** - Serves static files from the classpath/resources directory

### 2. WebView Server Integration

- **Internal Server** - WebViewServer includes an internal server that handles both HTTP and WebSocket connections
- Routes incoming requests to either HTTP handlers or WebSocket handlers based on the request type
- Automatically detects WebSocket upgrade requests
- No need to manually manage HTTP+WebSocket servers

### 3. WebView Components

- **`WebViewPageHandler`** - Generates HTML pages that:
  - Load the `webview.js` client library
  - Create a root div for rendering
  - Establish WebSocket connections
  - Show connection status

- **Enhanced `WebViewServer`** - Now supports:
  - `.withHtmlPages()` - Automatically generate HTML pages for WebView routes
  - `.withHttpRoute()` - Add custom HTTP endpoints
  - Seamless integration of HTTP and WebSocket routes

## Quick Start

The simplest way to create a WebView application:

```scala
import dev.alteration.branch.spider.webview.*

WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withHtmlPages()  // Enable automatic HTML serving
  .start(port = 8080)
```

Then visit: http://localhost:8080/counter

### What Happens Automatically

1. **HTML Page at `/counter`** - Serves a complete HTML page
2. **JavaScript at `/js/webview.js`** - Serves the WebView client library from resources
3. **WebSocket at `/counter`** - Handles the WebView WebSocket connection
4. **Connection Status** - Shows connected/disconnected indicator
5. **DOM Updates** - Automatically applies server-rendered HTML to the page

## File Structure

```
src/main/
├── scala/
│   └── dev/alteration/branch/spider/
│       ├── http/
│       │   ├── HttpHandler.scala          # HTTP request handler trait
│       │   ├── HttpResponse.scala         # HTTP response helpers
│       │   └── ResourceServer.scala       # Serves files from classpath
│       ├── websocket/
│       │   ├── WebSocketServer.scala      # Pure WebSocket server
│       │   └── WebSocketHandler.scala     # WebSocket handler trait
│       └── webview/
│           ├── WebView.scala              # Core WebView trait
│           ├── WebViewServer.scala        # Fluent WebView server API
│           ├── WebViewHandler.scala       # WebSocket handler for WebViews
│           ├── WebViewPageHandler.scala   # HTML page generator
│           └── examples/
│               ├── CounterWebView.scala        # Counter WebView
│               ├── SimpleCounterExample.scala  # Simplified example
│               ├── CounterExample.scala        # Full manual setup
│               └── README.md                   # Examples documentation
└── resources/
    └── spider/
        └── webview/
            └── webview.js                 # Client-side JavaScript library
```

## Examples

### Example 1: Simple Setup (Recommended)

File: `SimpleCounterExample.scala`

```scala
WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withHtmlPages()
  .start(port = 8080)
```

Run: `sbt "runMain dev.alteration.branch.spider.webview.examples.SimpleCounterExample"`

### Example 2: With Initial Params and DevMode

File: `CounterExample.scala`

```scala
val server = WebViewServer()
  .withRoute("/counter", new CounterWebView(), params = Map("initial" -> "10"))
  .withHtmlPages()
  .withDevMode(true)
  .start(port = 8080)
```

Run: `sbt "runMain dev.alteration.branch.spider.webview.examples.CounterExample"`

## Architecture

### Request Flow

```
Browser Request
    |
    v
WebViewServer (port 8080)
    |
    +-- Is WebSocket? --> WebSocketHandler --> WebView Actor
    |
    +-- Is HTTP? --> HttpHandler
                      |
                      +-- WebViewPageHandler (serves HTML)
                      +-- ResourceServer (serves webview.js)
```

### WebView Lifecycle

```
1. Browser loads HTML page (from WebViewPageHandler)
   └─> HTML includes <script src="/js/webview.js">

2. JavaScript loads (from ResourceServer)
   └─> new BranchWebView('ws://localhost:8080/path')

3. WebSocket connection established
   └─> WebViewHandler creates WebViewActor
       └─> mount() called, initial state created

4. Server sends initial HTML
   └─> Client renders HTML in #root div

5. User clicks button (wv-click="Increment")
   └─> Event sent to server over WebSocket
       └─> handleEvent() called, state updated
           └─> render() called, new HTML sent to client
               └─> Client updates DOM
```

## Advanced Usage

### Custom HTML Page

```scala
class MyPageHandler extends HttpHandler {
  override def handleGet(path: String, headers: Map[String, List[String]], socket: Socket): Unit = {
    val html = """<!DOCTYPE html>
    <html>
    <head><title>My App</title></head>
    <body>
      <div id="app"></div>
      <script src="/js/webview.js"></script>
      <script>
        new BranchWebView('ws://localhost:8080/ws/myview', {
          rootSelector: '#app',
          debug: true
        });
      </script>
    </body>
    </html>"""
    HttpResponse.ok(socket, html)
  }
}

WebViewServer()
  .withRoute("/myview", new MyWebView())
  .withHttpRoute("/custom", new MyPageHandler())
  .start(port = 8080)
```

### Multiple WebViews

```scala
WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withRoute("/todo", new TodoWebView())
  .withRoute("/chat", new ChatWebView())
  .withHtmlPages()
  .start(port = 8080)
```

Each route automatically gets:
- HTML page at the route path (e.g., `/counter`)
- WebSocket connection at the same path
- Shared JavaScript at `/js/webview.js`

### Custom Static Files

```scala
val staticServer = ResourceServer("static")

WebViewServer()
  .withRoute("/app", new MyWebView())
  .withHttpRoute("/static", staticServer)
  .withHtmlPages()
  .start(port = 8080)
```

Now you can put files in `src/main/resources/static/` and they'll be served:
- `src/main/resources/static/css/app.css` → http://localhost:8080/static/css/app.css
- `src/main/resources/static/images/logo.png` → http://localhost:8080/static/images/logo.png

## Key Design Decisions

1. **All-in-one WebViewServer** - Single class handles HTTP, WebSocket, and WebView routing
2. **Internal HTTP+WebSocket server** - No need to manually configure separate servers
3. **Automatic HTML generation** - `.withHtmlPages()` creates pages for all routes automatically
4. **Same path for HTTP and WebSocket** - Simplifies routing (e.g., `/counter` serves both)
5. **ResourceServer for JavaScript** - Loads `webview.js` from classpath at runtime

## Testing

Check if everything compiles:

```bash
sbt compile
```

Run the simple example:

```bash
sbt "runMain dev.alteration.branch.spider.webview.examples.SimpleCounterExample"
```

Then open: http://localhost:8080/counter

You should see:
- A counter with +/- buttons
- A green "Connected" indicator in the top right
- Clicking buttons updates the count instantly
- Network tab shows WebSocket connection established

## Troubleshooting

### "Resource not found: spider/webview/webview.js"

Make sure the file exists at:
```
src/main/resources/spider/webview/webview.js
```

### WebSocket Connection Fails

1. Check the WebSocket URL in the console
2. Make sure the path matches your `withRoute()` path
3. Verify the server is running and listening on the correct port

### HTML Page Shows "Loading..." Forever

1. Check browser console for JavaScript errors
2. Verify webview.js loaded (Network tab)
3. Check WebSocket connection status

### Events Not Working

1. Make sure event names match case class names exactly
2. Check that `EventCodec` is derived: `sealed trait MyEvent derives EventCodec`
3. Verify `given EventCodec[MyEvent]` is in scope when creating handler

## Next Steps

1. Create your own WebView (see examples)
2. Add more routes with `.withRoute()`
3. Customize the HTML page with your own `HttpHandler`
4. Add static files with `ResourceServer`
5. Enable DevTools with `.withDevMode(true)`

## Summary

You now have a complete infrastructure for serving WebView applications:

✅ HTTP server for static files and HTML pages
✅ WebSocket server for real-time state synchronization
✅ Automatic HTML page generation
✅ Resource serving from classpath
✅ Simplified API with `.withHtmlPages()`
✅ Full manual control when needed
✅ Working examples to learn from

Happy coding! 🚀
