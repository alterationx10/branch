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

## WebView - Server-Side Reactive UI Framework

WebView is a server-side reactive UI framework that brings a Phoenix LiveView-inspired approach to Scala. It enables you
to build dynamic, real-time web applications where the UI state lives on the server and updates are pushed to clients
over WebSocket.

This is new and ambitious, so there are probably a few bugs and likely to change a bit in the early iterations.

### Key Features

- **Server-Side State**: All application state lives on the server in type-safe Scala code
- **Automatic Updates**: UI updates are pushed to clients in real-time via WebSocket
- **Type-Safe Events**: Strongly-typed event system with compile-time guarantees
- **HTML DSL**: Scalatags-inspired type-safe HTML construction with automatic XSS protection
- **Actor-Based**: Built on Keanu actors for concurrent, isolated component state
- **Lifecycle Hooks**: Rich lifecycle hooks for side effects, pub/sub, and actor integration
- **Error Boundaries**: Graceful error recovery with customizable error handling
- **CSS-in-Scala**: Scoped styling with StyleSheet for collision-free CSS
- **DevTools**: Built-in debugging and monitoring tools for development
- **Component Library**: Some pre-built components for forms, inputs, buttons, and layouts

### Quick Start

The simplest way to get started is using the WebViewServer builder API with automatic HTML page serving:

```scala
import dev.alteration.branch.spider.webview.*

// Define your state
case class CounterState(count: Int = 0)

// Define events with type safety
sealed trait CounterEvent derives EventCodec

case object Increment extends CounterEvent

case object Decrement extends CounterEvent

case object Reset extends CounterEvent

// Define your WebView
class CounterWebView extends WebView[CounterState, CounterEvent] {

  override def mount(params: Map[String, String], session: Session): CounterState = {
    CounterState(count = 0)
  }

  override def handleEvent(event: CounterEvent, state: CounterState): CounterState = {
    event match {
      case Increment => state.copy(count = state.count + 1)
      case Decrement => state.copy(count = state.count - 1)
      case Reset => state.copy(count = 0)
    }
  }

  override def render(state: CounterState): String = {
    s"""
    <div>
      <h1>Count: ${state.count}</h1>
      <button wv-click="Increment">+</button>
      <button wv-click="Decrement">-</button>
      <button wv-click="Reset">Reset</button>
    </div>
    """
  }
}

// Start the server
@main def run(): Unit = {
  val server = WebViewServer()
    .withRoute("/counter", new CounterWebView())
    .withHtmlPages() // Automatically serve HTML pages
    .withDevMode(true) // Enable DevTools
    .start(port = 8080)

  println("Visit http://localhost:8080/counter")
  scala.io.StdIn.readLine()
}
```

### Core Concepts

#### WebView Trait

The `WebView[State, Event]` trait is the foundation of all components. It defines the lifecycle of a reactive component:

```scala
trait WebView[State, Event] {
  // Initialize state when a client connects
  def mount(params: Map[String, String], session: Session): State

  // Handle events from the client
  def handleEvent(event: Event, state: State): State

  // Handle messages from the actor system (pub/sub, timers, etc.)
  def handleInfo(msg: Any, state: State): State = state

  // Render state as HTML
  def render(state: State): String

  // Clean up when the component terminates
  def terminate(reason: Option[Throwable], state: State): Unit = {}

  // Lifecycle hooks (see Lifecycle Hooks section)
  def afterMount(state: State, context: WebViewContext): Unit = {}

  def beforeUpdate(event: Event, state: State, context: WebViewContext): Unit = {}

  def afterUpdate(event: Event, oldState: State, newState: State, context: WebViewContext): Unit = {}

  def beforeRender(state: State): State = state

  // Error boundaries (see Error Handling section)
  def onError(error: Throwable, state: State, phase: ErrorPhase): Option[State] = None

  def renderError(error: Throwable, phase: ErrorPhase): String = {
    /* default error UI */
  }
}
```

#### State Management

State is immutable and type-safe. Each WebView instance maintains its own state, isolated via the actor model:

```scala
case class TodoState(
                      todos: List[Todo],
                      filter: Filter,
                      inputValue: String
                    )

class TodoWebView extends WebView[TodoState, TodoEvent] {
  override def mount(params: Map[String, String], session: Session): TodoState = {
    TodoState(todos = List.empty, filter = Filter.All, inputValue = "")
  }

  override def handleEvent(event: TodoEvent, state: TodoState): TodoState = {
    event match {
      case AddTodo(text) =>
        val newTodo = Todo(id = UUID.randomUUID().toString, text = text, completed = false)
        state.copy(todos = state.todos :+ newTodo, inputValue = "")

      case ToggleTodo(id) =>
        state.copy(todos = state.todos.map { todo =>
          if (todo.id == id) todo.copy(completed = !todo.completed)
          else todo
        })

      case SetFilter(filter) =>
        state.copy(filter = filter)
    }
  }
}
```

#### Event System

Events are strongly-typed using sealed traits and the `EventCodec` type class, which provides automatic JSON
encoding/decoding:

```scala
// Define events as a sealed trait ADT
sealed trait TodoEvent derives EventCodec

case class AddTodo(text: String) extends TodoEvent

case class ToggleTodo(id: String) extends TodoEvent

case class DeleteTodo(id: String) extends TodoEvent

case class SetFilter(filter: Filter) extends TodoEvent

case object ClearCompleted extends TodoEvent

// The compiler enforces exhaustiveness checking
override def handleEvent(event: TodoEvent, state: TodoState): TodoState = {
  event match {
    case AddTodo(text) => // handle
    case ToggleTodo(id) => // handle
    case DeleteTodo(id) => // handle
    case SetFilter(filter) => // handle
    case ClearCompleted => // handle
    // Compiler error if you forget a case!
  }
}
```

The `EventCodec` automatically handles serialization between client and server using Friday's JSON codec:

```scala
// Client sends: { "event": "AddTodo", "value": "{\"text\":\"Buy milk\"}" }
// Server receives: AddTodo(text = "Buy milk")
```

### HTML DSL

WebView includes a comprehensive type-safe HTML DSL inspired by Scalatags:

#### Basic HTML Construction

```scala
import dev.alteration.branch.spider.webview.html._
import dev.alteration.branch.spider.webview.html.Tags._
import dev.alteration.branch.spider.webview.html.Attributes._

override def render(state: CounterState): String = {
  div(cls := "container")(
    h1("Counter"),
    div(cls := "count-display")(
      text(state.count)
    ),
    div(cls := "buttons")(
      button(wvClick := Increment, cls := "btn")("Increment"),
      button(wvClick := Decrement, cls := "btn")("Decrement")
    )
  ).render
}
```

#### Available Tags

The DSL provides all standard HTML5 tags:

```
// Container elements
div, span, section, article, header, footer, main, aside, nav

// Text elements
h1, h2, h3, h4, h5, h6, p, strong, em, code, pre, br, hr

// Lists
ul, ol, li, dl, dt, dd

// Forms
form, input, textarea, button, label, select, option, fieldset, legend

// Tables
table, thead, tbody, tfoot, tr, th, td

// Links & Media
a, img, video, audio, source

// Semantic
figure, figcaption, time, mark, blockquote
```

#### Attributes

Standard HTML attributes with type-safe builders:

```scala
// Common attributes
cls := "my-class" // class attribute
id := "my-id"
style := "color: red"
title := "Tooltip text"

// Form attributes
name := "username"
value := state.username
placeholder := "Enter name"
tpe := "text" // type attribute

// Link attributes
href := "/path"
target := "_blank"
rel := "noopener"

// Boolean attributes
disabled := true
checked := state.isChecked
readonly := false
required := true

// ARIA attributes
ariaLabel := "Close button"
ariaHidden := false
role := "button"

// Data attributes
data("user-id") := "123" // renders: data-user-id="123"

// Custom attributes
attr("my-custom") := "value"
```

#### Conditional Rendering

```scala
// Conditional HTML
when(state.showMessage)(
  div(cls := "alert")("Hello!")
)

// If-else conditional
cond(state.isLoggedIn)(
  div("Welcome, user!")
)(
  div("Please log in")
)

// Conditional attributes
div(
  attrWhen(state.isActive, cls := "active"),
  classWhen(
    "loading" -> state.isLoading,
    "error" -> state.hasError
  )
)

// Conditional styles
div(
  styleWhen(
    ("color", "red", state.isError),
    ("display", "none", state.isHidden)
  )
)
```

#### Component Library

Pre-built components for common UI patterns:

```scala
import dev.alteration.branch.spider.webview.html.Components._

// Form inputs
textInput("username", state.username, "UpdateUsername",
  placeholder = Some("Enter username"))

emailInput("email", state.email, "UpdateEmail")

passwordInput("password", state.password, "UpdatePassword")

numberInput("age", state.age.toString, "UpdateAge",
  min = Some(0), max = Some(120))

textArea("bio", state.bio, "UpdateBio", rows = 5)

// Checkboxes and radio buttons
checkbox("terms", state.acceptedTerms, "ToggleTerms",
  labelText = Some("I accept the terms"))

radio("color", "red", state.color == "red", "SelectColor",
  labelText = Some("Red"))

// Select dropdowns
selectDropdown("country",
  options = List("US" -> "United States", "UK" -> "United Kingdom"),
  selectedValue = state.country,
  changeEvent = "SelectCountry"
)

// Buttons
clickButton("Save", "SaveForm", extraAttrs = Seq(cls := "btn-primary"))

targetButton("Delete", "DeleteItem", state.itemId,
  extraAttrs = Seq(cls := "btn-danger"))

// Lists
keyedList(state.items,
  renderItem = (item, index) => div(cls := "item")(text(item.name)),
  containerAttrs = Seq(cls := "item-list")
)

unorderedList(state.items,
  renderItem = item => text(item.name))

// Layout helpers
container(
  h1("My App"),
  p("Content here")
)(maxWidth = Some("600px"), padding = Some("20px"))

flexContainer(
  div("Left"),
  div("Right")
)(direction = "row", gap = Some("10px"), justifyContent = Some("space-between"))
```

### WebView Attributes

WebView attributes (`wv-*`) enable reactive event handling. When an element with a WebView attribute is interacted with,
an event is sent to the server:

#### Event Attributes

```scala
// Click events
button(wvClick := "submit")("Submit")
button(wvClick := SaveForm)("Save") // Typed event

// Form events
input(wvChange := "update", value := state.text)
input(wvInput := "search") // Fires on every keystroke
form(wvSubmit := "save-form")

// Focus events
input(wvFocus := "field-focused", wvBlur := "field-blurred")

// Keyboard events
input(wvKeydown := "handle-key", wvKeyup := "key-released")

// Mouse events
div(wvMouseenter := "show-tooltip", wvMouseleave := "hide-tooltip")
```

#### Event Modifiers

```scala
// Debounce - wait for user to stop typing
input(
  wvInput := "search",
  wvDebounce := "300" // Wait 300ms after last keystroke
)

// Throttle - rate-limit events
div(
  wvClick := "track-click",
  wvThrottle := "1000" // Max once per second
)

// Attach values to events
button(
  wvClick := "delete-item",
  wvTarget := item.id // Include item.id in event payload
)

button(
  wvClick := "set-filter",
  wvValue := "active"
)

// Prevent DOM updates for specific elements
input(
  wvChange := "update",
  wvIgnore := true // Preserve focus/scroll position
)
```

#### Helper Functions

```scala
// Click with target value
button(wvClickTarget("delete", todo.id) *)(

)("Delete")

// Click with custom value
button(wvClickValue("filter", "active") *)("Active")

// Debounced input
input(wvDebounceInput("search", 300) *)

// Throttled click
div(wvThrottleClick("track", 1000) *)
```

### Styling

WebView provides CSS-in-Scala via the `StyleSheet` abstraction with automatic scoping:

```scala
import dev.alteration.branch.spider.webview.styling._

object TodoStyles extends StyleSheet {
  val container = style(
    "max-width" -> "600px",
    "margin" -> "0 auto",
    "padding" -> "20px",
    "font-family" -> "sans-serif"
  )

  val todoItem = style(
    "padding" -> "10px",
    "border-bottom" -> "1px solid #ccc",
    "display" -> "flex",
    "justify-content" -> "space-between"
  )

  val completed = style(
    "text-decoration" -> "line-through",
    "opacity" -> "0.6"
  )
}

// Use in render
override def render(state: TodoState): String = {
  div(cls := TodoStyles.container)(
    h1("Todos"),
    ul()(
      state.todos.map { todo =>
        li(cls := TodoStyles.todoItem + " " + (if (todo.completed) TodoStyles.completed else ""))(
          span()(text(todo.text)),
          button(wvClick := ToggleTodo(todo.id))("Toggle")
        )
      } *
    ),
    // Include styles in output
    raw(TodoStyles.toStyleTag)
  ).render
}
```

#### CSS Utilities

```scala
import dev.alteration.branch.spider.webview.styling.CSSUtils._

// Color palette
Colors.primary // "#667eea"
Colors.danger // "#f56565"
Colors.success // "#48bb78"

// Spacing
Spacing.sm // "8px"
Spacing.md // "16px"

// Border radius
Radius.md // "8px"
Radius.full // "9999px"

// Shadows
Shadows.md // "0 4px 6px rgba(0,0,0,0.1)"

// Helper functions
val flexStyle = style(
  flex(direction = "row", justify = "space-between", gap = "10px") *
)

val gridStyle = style(
  grid(columns = "repeat(3, 1fr)", gap = "20px") *
)

val absoluteStyle = style(
  absolute(top = Some("10px"), right = Some("10px")) *
)
```

### Lifecycle Hooks

WebView provides rich lifecycle hooks for side effects and actor integration:

```scala
class ChatWebView extends WebView[ChatState, ChatEvent] {

  // Called after initial mount with WebViewContext
  override def afterMount(state: ChatState, context: WebViewContext): Unit = {
    // Subscribe to pub/sub for chat messages
    // Note: Pub/sub would need to be implemented separately via actors

    // Load initial data asynchronously
    Future {
      val messages = loadMessagesFromDB()
      context.sendSelf(LoadedMessages(messages))
    }
  }

  // Called before processing an event
  override def beforeUpdate(event: ChatEvent, state: ChatState, context: WebViewContext): Unit = {
    // Log events for debugging
    println(s"Processing event: $event")

    // Check authorization
    if (!isAuthorized(event, state.user)) {
      throw new UnauthorizedException()
    }
  }

  // Called after processing an event
  override def afterUpdate(
                            event: ChatEvent,
                            oldState: ChatState,
                            newState: ChatState,
                            context: WebViewContext
                          ): Unit = {
    event match {
      case SendMessage(msg) =>
        // Broadcast to other users
        context.tellPath("/user/chat-broadcaster", BroadcastMessage(msg))

      case _ => ()
    }
  }

  // Called before rendering (transform state for view)
  override def beforeRender(state: ChatState): ChatState = {
    // Add computed fields
    state.copy(
      unreadCount = state.messages.count(!_.read),
      sortedMessages = state.messages.sortBy(_.timestamp)
    )
  }

  // Clean up on termination
  override def terminate(reason: Option[Throwable], state: ChatState): Unit = {
    // Close resources and clean up
    state.connection.foreach(_.close())
  }
}
```

#### WebViewContext

The `WebViewContext` provides access to the actor system for actor communication:

```scala
case class WebViewContext(
                           system: ActorSystem,
                           sendSelf: Any => Unit // Send message to this WebView's actor
                         )

// Use in lifecycle hooks
override def afterMount(state: State, context: WebViewContext): Unit = {
  // Send message to self (received via handleInfo)
  context.sendSelf(InitComplete)

  // Send message to another actor by path
  context.tellPath("/user/my-actor", SomeMessage)

  // Create a new actor
  val worker = context.system.actorOf[WorkerActor]("worker")

  // Send message to the worker
  context.system.tell(worker, DoWork)
}
```

### Error Boundaries

WebView includes built-in error boundaries for graceful error recovery:

```scala
class ResilientWebView extends WebView[MyState, MyEvent] {

  // Attempt to recover from errors
  override def onError(
                        error: Throwable,
                        state: MyState,
                        phase: ErrorPhase
                      ): Option[MyState] = {
    phase match {
      case ErrorPhase.Mount =>
        // Recover to default state
        Some(MyState.default)

      case ErrorPhase.Event =>
        // Clear problematic data
        Some(state.copy(errorMessage = Some(error.getMessage)))

      case ErrorPhase.Render =>
        // Reset to last good state
        Some(state.copy(debugMode = true))

      case _ =>
        None // No recovery, show error UI
    }
  }

  // Custom error UI
  override def renderError(error: Throwable, phase: ErrorPhase): String = {
    s"""
    <div class="error-container">
      <h2>Oops! Something went wrong in ${phase.name}</h2>
      <p>${error.getMessage}</p>
      <button wv-click="Retry">Try Again</button>
      <button onclick="location.reload()">Reload Page</button>
    </div>
    """
  }

  // Control retry logic
  override def shouldRetry(
                            error: Throwable,
                            phase: ErrorPhase,
                            attemptCount: Int
                          ): Boolean = {
    // Retry up to 3 times for transient errors
    error match {
      case _: NetworkException if attemptCount < 3 => true
      case _ => false
    }
  }
}
```

Error phases:

- `ErrorPhase.Mount` - Error during initial mount
- `ErrorPhase.Event` - Error during event handling
- `ErrorPhase.Info` - Error during info message handling
- `ErrorPhase.Render` - Error during rendering
- `ErrorPhase.Lifecycle` - Error in lifecycle hooks

### WebView Server

The `WebViewServer` provides a fluent builder API for configuring and starting WebView applications:

#### Basic Server

```scala
val server = WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withRoute("/todos", new TodoWebView())
  .start(port = 8080)

// Later
server.stop()
```

#### With Automatic HTML Pages

```scala
val server = WebViewServer()
  .withRoute("/counter", new CounterWebView())
  .withHtmlPages() // Automatically serve HTML pages + webview.js
  .start(port = 8080)

// Now you can visit:
// http://localhost:8080/counter (HTML page)
// ws://localhost:8080/counter (WebSocket endpoint)
// http://localhost:8080/js/webview.js (Client library)
```

#### With DevMode

```scala
val server = WebViewServer()
  .withRoute("/app", new MyWebView())
  .withDevMode(true) // Adds DevTools at /__devtools
  .withHtmlPages()
  .start(port = 8080)

// Visit http://localhost:8080/__devtools to see:
// - Component hierarchy
// - State timeline
// - Event log
// - Performance metrics
```

#### With Custom HTTP Routes

```scala
import dev.alteration.branch.spider.webview.http._

val server = WebViewServer()
  .withRoute("/app", new MyWebView())
  .withHttpRoute("/api/data", new JsonApiHandler())
  .withHttpRoute("/static", ResourceServer("public"))
  .withHtmlPages()
  .start(port = 8080)
```

#### With Route Factory

```scala
// Create new instance per connection
val server = WebViewServer()
  .withRouteFactory("/chat", () => new ChatWebView())
  .start(port = 8080)
```

#### With Parameters and Session

```scala
val server = WebViewServer()
  .withRoute(
    path = "/app",
    webView = new MyWebView(),
    params = Map("theme" -> "dark", "lang" -> "en"),
    session = Session(Map("userId" -> user.id))
  )
  .start(port = 8080)

// Access in mount:
override def mount(params: Map[String, String], session: Session): MyState = {
  val theme = params.get("theme")
  val userId = session.get[String]("userId")
  MyState(theme = theme, userId = userId)
}
```

#### Custom Actor System

```scala
val actorSystem = ActorSystem()

val server = WebViewServer(actorSystem)
  .withRoute("/app", new MyWebView())
  .start(port = 8080)
```

### DevTools

WebView includes built-in DevTools for debugging and monitoring (enabled with `.withDevMode(true)`):

#### Features

- **Component Inspector**: View active WebView instances and their state
- **Timeline**: See all events, state changes, and info messages
- **Performance Metrics**: Track render times, event processing, and memory usage
- **Connection Status**: Monitor WebSocket connections and disconnections
- **State Diff**: Compare state before and after events

#### Accessing DevTools

```scala
val server = WebViewServer()
  .withRoute("/app", new MyWebView())
  .withDevMode(true)
  .withHtmlPages()
  .start(port = 8080)

// Visit http://localhost:8080/__devtools
```

#### DevTools Integration

DevTools automatically tracks all WebView activity:

 <!-- @formatter:off -->
```scala
// All of this is automatically captured:
override def mount(...) = MyState(...) // Recorded: Mount event
override def handleEvent(Increment, state) =... // Recorded: Event + state diff
override def handleInfo(msg, state) =... // Recorded: Info message
// Render time is automatically measured
```
 <!-- @formatter:on -->

### Advanced Topics

#### Actor Communication

WebViews can communicate with other actors in the system:

```scala
override def afterMount(state: State, context: WebViewContext): Unit = {
  // Create a worker actor
  val workerRef = context.system.actorOf[DataLoaderActor]("data-loader")

  // Store reference and send initial message
  context.sendSelf(WorkerCreated(workerRef))
  context.system.tell(workerRef, StartLoading)
}

override def afterUpdate(
                          event: Event,
                          oldState: State,
                          newState: State,
                          context: WebViewContext
                        ): Unit = {
  event match {
    case RequestData =>
      // Send message to worker actor
      newState.workerRef.foreach { ref =>
        context.system.tell(ref, LoadDataRequest)
      }
    case _ => ()
  }
}

override def handleInfo(msg: Any, state: State): State = {
  msg match {
    case DataLoaded(data) =>
      state.copy(data = Some(data), loading = false)
    case LoadError(error) =>
      state.copy(error = Some(error), loading = false)
    case WorkerCreated(ref) =>
      state.copy(workerRef = Some(ref))
    case _ =>
      state
  }
}
```

### Complete Example

Here's a complete example showing many WebView features:

```scala
import dev.alteration.branch.spider.webview.*
import dev.alteration.branch.spider.webview.html.*
import dev.alteration.branch.spider.webview.html.Tags.*
import dev.alteration.branch.spider.webview.html.Attributes.*
import dev.alteration.branch.spider.webview.html.Components.*
import dev.alteration.branch.spider.webview.styling.*

// State
case class TodoState(
                      todos: List[Todo],
                      inputValue: String,
                      filter: Filter,
                      error: Option[String] = None
                    )

case class Todo(id: String, text: String, completed: Boolean, createdAt: Instant)

enum Filter {
  case All, Active, Completed
}

// Events
sealed trait TodoEvent derives EventCodec

case class AddTodo(text: String) extends TodoEvent

case class ToggleTodo(id: String) extends TodoEvent

case class DeleteTodo(id: String) extends TodoEvent

case class UpdateInput(value: String) extends TodoEvent

case class SetFilter(filter: Filter) extends TodoEvent

case object ClearCompleted extends TodoEvent

// Styles
object TodoStyles extends StyleSheet {
  val container = style(
    "max-width" -> "600px",
    "margin" -> "0 auto",
    "padding" -> "20px"
  )

  val todoItem = style(
    "display" -> "flex",
    "gap" -> "10px",
    "padding" -> "10px",
    "border-bottom" -> "1px solid #eee"
  )

  val completed = style(
    "text-decoration" -> "line-through",
    "opacity" -> "0.6"
  )
}

// WebView
class TodoWebView extends WebView[TodoState, TodoEvent] {

  override def mount(params: Map[String, String], session: Session): TodoState = {
    TodoState(todos = List.empty, inputValue = "", filter = Filter.All)
  }

  override def handleEvent(event: TodoEvent, state: TodoState): TodoState = {
    event match {
      case AddTodo(text) if text.trim.nonEmpty =>
        val todo = Todo(
          id = UUID.randomUUID().toString,
          text = text.trim,
          completed = false,
          createdAt = Instant.now()
        )
        state.copy(todos = state.todos :+ todo, inputValue = "")

      case ToggleTodo(id) =>
        state.copy(todos = state.todos.map { t =>
          if (t.id == id) t.copy(completed = !t.completed) else t
        })

      case DeleteTodo(id) =>
        state.copy(todos = state.todos.filterNot(_.id == id))

      case UpdateInput(value) =>
        state.copy(inputValue = value)

      case SetFilter(filter) =>
        state.copy(filter = filter)

      case ClearCompleted =>
        state.copy(todos = state.todos.filterNot(_.completed))

      case _ =>
        state
    }
  }

  override def beforeRender(state: TodoState): TodoState = {
    // Add computed fields
    val filteredTodos = state.filter match {
      case Filter.All => state.todos
      case Filter.Active => state.todos.filterNot(_.completed)
      case Filter.Completed => state.todos.filter(_.completed)
    }
    state.copy(todos = filteredTodos)
  }

  override def render(state: TodoState): String = {
    val visibleTodos = state.filter match {
      case Filter.All => state.todos
      case Filter.Active => state.todos.filterNot(_.completed)
      case Filter.Completed => state.todos.filter(_.completed)
    }

    div(cls := TodoStyles.container)(
      h1()("Todo List"),

      // Input form
      div()(
        textInput("todo-input", state.inputValue, "UpdateInput",
          placeholder = Some("What needs to be done?")),
        button(wvClick := AddTodo(state.inputValue))("Add")
      ),

      // Filter buttons
      div()(
        clickButton("All", "SetFilter",
          extraAttrs = Seq(classWhen("active" -> (state.filter == Filter.All)))),
        clickButton("Active", "SetFilter",
          extraAttrs = Seq(classWhen("active" -> (state.filter == Filter.Active)))),
        clickButton("Completed", "SetFilter",
          extraAttrs = Seq(classWhen("active" -> (state.filter == Filter.Completed))))
      ),

      // Todo list
      ul()(
        visibleTodos.map { todo =>
          li(cls := TodoStyles.todoItem + (if (todo.completed) " " + TodoStyles.completed else ""))(
            checkbox(todo.id, todo.completed, s"ToggleTodo:${todo.id}"),
            span()(text(todo.text)),
            targetButton("Delete", "DeleteTodo", todo.id)
          )
        } *
      ),

      // Stats
      div()(
        text(s"${state.todos.count(!_.completed)} items left"),
        when(state.todos.exists(_.completed))(
          button(wvClick := ClearCompleted)("Clear completed")
        )
      ),

      // Include styles
      raw(TodoStyles.toStyleTag)
    ).render
  }

  override def onError(
                        error: Throwable,
                        state: TodoState,
                        phase: ErrorPhase
                      ): Option[TodoState] = {
    Some(state.copy(error = Some(error.getMessage)))
  }
}

// Server
@main def runTodoApp(): Unit = {
  val server = WebViewServer()
    .withRoute("/todos", new TodoWebView())
    .withHtmlPages()
    .withDevMode(true)
    .start(port = 8080)

  println("Todo app running at http://localhost:8080/todos")
  println("DevTools at http://localhost:8080/__devtools")

  scala.io.StdIn.readLine()
  server.stop()
}
```

## Other Libraries

If you like Spider, you should check out [Tapir](https://tapir.softwaremill.com/en/latest/)