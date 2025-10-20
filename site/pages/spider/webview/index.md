---
title: WebView - Server-Side Reactive UI Framework
description: Build dynamic web applications with server-side state
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - webview
  - reactive
  - ui
---

# WebView - Server-Side Reactive UI Framework

WebView is a server-side reactive UI framework that brings a Phoenix LiveView-inspired approach to Scala. It enables you to build dynamic, real-time web applications where the UI state lives on the server and updates are pushed to clients over WebSocket.

This is new and ambitious, so there are probably a few bugs and likely to change a bit in the early iterations.

## Key Features

- **Server-Side State**: All application state lives on the server in type-safe Scala code
- **Automatic Updates**: UI updates are pushed to clients in real-time via WebSocket
- **Type-Safe Events**: Strongly-typed event system with compile-time guarantees
- **HTML DSL**: Scalatags-inspired type-safe HTML construction with automatic XSS protection
- **Actor-Based**: Built on Keanu actors for concurrent, isolated component state
- **Lifecycle Hooks**: Rich lifecycle hooks for side effects, pub/sub, and actor integration
- **Error Boundaries**: Graceful error recovery with customizable error handling
- **CSS-in-Scala**: Scoped styling with StyleSheet for collision-free CSS
- **DevTools**: Built-in debugging and monitoring tools for development
- **Basic Components**: Built-in form inputs, buttons, and layout helpers

## Quick Start

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

## Core Concepts

### WebView Trait

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

  // Lifecycle hooks (see Advanced Topics)
  def afterMount(state: State, context: WebViewContext): Unit = {}
  def beforeUpdate(event: Event, state: State, context: WebViewContext): Unit = {}
  def afterUpdate(event: Event, oldState: State, newState: State, context: WebViewContext): Unit = {}
  def beforeRender(state: State): State = state

  // Error boundaries (see Advanced Topics)
  def onError(error: Throwable, state: State, phase: ErrorPhase): Option[State] = None
  def renderError(error: Throwable, phase: ErrorPhase): String = { /* default error UI */ }
}
```

### State Management

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

### Event System

Events are strongly-typed using sealed traits and the `EventCodec` type class, which provides automatic JSON encoding/decoding:

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

## WebView Server

The WebViewServer provides a fluent builder API for configuring WebView applications:

```scala
val server = WebViewServer()
  .withWebViewRoute("/counter", new CounterWebView())
  .withWebViewRoute("/todos", new TodoWebView())
  .withDevMode(true) // Enable DevTools
  .start(port = 8080)

// Visit http://localhost:8080/counter or http://localhost:8080/todos
// DevTools at http://localhost:8080/__devtools
```

### Key Methods

- `.withWebViewRoute(path, webView)` - Add a WebView with automatic page serving
- `.withWebViewRouteFactory(path, factory)` - Use factory for per-connection instances
- `.withHttpRoute(path, handler)` - Add custom HTTP endpoints
- `.withWebSocketRoute(path, handler)` - Add custom WebSocket endpoints (non-WebView)
- `.withDevMode(enabled)` - Enable DevTools at `/__devtools`
- `.start(port, host)` - Start the server

## Documentation

- **[HTML DSL](/spider/webview/html-dsl)** - Type-safe HTML construction with tags, attributes, and components
- **[Styling](/spider/webview/styling)** - CSS-in-Scala with StyleSheet and CSS utilities
- **[Advanced Topics](/spider/webview/advanced)** - Lifecycle hooks, error boundaries, actor communication, and DevTools

## Next Steps

- Learn about the [HTML DSL](/spider/webview/html-dsl) for building UIs
- Explore [Styling](/spider/webview/styling) for CSS-in-Scala
- Dive into [Advanced Topics](/spider/webview/advanced) for lifecycle hooks and actor integration
