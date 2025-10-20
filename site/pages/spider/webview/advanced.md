---
title: WebView Advanced Topics
description: Lifecycle hooks, error boundaries, actor communication, and DevTools
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - webview
  - lifecycle
  - actors
  - devtools
---

# Advanced Topics

## Lifecycle Hooks

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

### WebViewContext

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

## Error Boundaries

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

### Error Phases

- `ErrorPhase.Mount` - Error during initial mount
- `ErrorPhase.Event` - Error during event handling
- `ErrorPhase.Info` - Error during info message handling
- `ErrorPhase.Render` - Error during rendering
- `ErrorPhase.Lifecycle` - Error in lifecycle hooks

## Actor Communication

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

## WebView Server Configuration

### With Parameters and Session

```scala
val server = WebViewServer()
  .withWebViewRoute(
    path = "/app",
    webView = new MyWebView(),
    params = Map("theme" -> "dark"),
    session = Session(Map("userId" -> "123"))
  )
  .start(port = 8080)

// Access in mount():
override def mount(params: Map[String, String], session: Session): MyState = {
  val theme = params.get("theme") // Some("dark")
  val userId = session.get[String]("userId") // Some("123")
  MyState(theme = theme, userId = userId)
}
```

### With Route Factory (Per-Connection Instances)

```scala
val server = WebViewServer()
  .withWebViewRouteFactory("/chat", () => new ChatWebView())
  .start(port = 8080)

// Creates a new ChatWebView instance for each WebSocket connection
```

### Mixing WebViews with Custom Routes

```scala
import dev.alteration.branch.spider.server.*
import java.nio.file.Path

val server = WebViewServer()
  .withWebViewRoute("/app", new MyWebView())
  .withHttpRoute("/static", new FileHandler(Path.of("public")))
  .withWebSocketRoute("/ws/echo", new EchoWebSocketHandler())
  .start(port = 8080)
```

## DevTools

WebView includes built-in DevTools for debugging and monitoring (enabled with `.withDevMode(true)`):

```scala
val server = WebViewServer()
  .withDevMode(true) // Adds /__devtools route
  .withWebViewRoute("/app", new MyWebView())
  .start(port = 8080)

// Visit http://localhost:8080/__devtools for real-time debugging
```

### Features

- **Component Inspector**: View active WebView instances and their state
- **Timeline**: See all events, state changes, and info messages
- **Performance Metrics**: Track render times, event processing, and memory usage
- **Connection Status**: Monitor WebSocket connections and disconnections
- **State Diff**: Compare state before and after events

### DevTools Integration

DevTools automatically tracks all WebView activity:

```scala
// All of this is automatically captured:
override def mount(...) = MyState(...) // Recorded: Mount event
override def handleEvent(Increment, state) = ... // Recorded: Event + state diff
override def handleInfo(msg, state) = ... // Recorded: Info message
// Render time is automatically measured
```

## Complete Example

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

## Next Steps

- Learn about the [HTML DSL](html-dsl.md) for building UIs
- Explore [Styling](styling.md) for CSS-in-Scala
- Return to [WebView Overview](index.md)
