---
title: Spider
description: A custom HTTP/1.1 server with WebSocket support and reactive UI framework
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

Spider is a lightweight HTTP framework with a custom HTTP/1.1 server implementation built on `java.net.ServerSocket`. It provides both server and client functionality with a clean Scala API, WebSocket support, and a reactive UI framework called WebView.

## Overview

Spider consists of several major components:

- **[HTTP Server](server.md)** - Custom HTTP/1.1 server with routing, static file serving, and security features
- **[HTTP Client](client.md)** - Client API built on `java.net.http.HttpClient`
- **[WebSocket Support](websockets.md)** - Full WebSocket implementation with lifecycle management
- **[WebView Framework](webview/)** - Server-side reactive UI framework inspired by Phoenix LiveView

## Quick Start

### Simple HTTP Server

```scala
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.common.HttpMethod

case class GreeterHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(200, "Hello!")
  }
}

val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "greet" :: Nil) => GreeterHandler()
}

val server = new SpiderServer(
  port = 8080,
  router = router,
  config = ServerConfig.default
).withShutdownHook()

server.start() // Blocking call
```

### WebView Application

```scala
import dev.alteration.branch.spider.webview.*

case class CounterState(count: Int = 0)

sealed trait CounterEvent derives EventCodec
case object Increment extends CounterEvent
case object Decrement extends CounterEvent

class CounterWebView extends WebView[CounterState, CounterEvent] {
  override def mount(params: Map[String, String], session: Session): CounterState = {
    CounterState(count = 0)
  }

  override def handleEvent(event: CounterEvent, state: CounterState): CounterState = {
    event match {
      case Increment => state.copy(count = state.count + 1)
      case Decrement => state.copy(count = state.count - 1)
    }
  }

  override def render(state: CounterState): String = {
    s"""
    <div>
      <h1>Count: ${state.count}</h1>
      <button wv-click="Increment">+</button>
      <button wv-click="Decrement">-</button>
    </div>
    """
  }
}

@main def run(): Unit = {
  val server = WebViewServer()
    .withRoute("/counter", new CounterWebView())
    .withHtmlPages()
    .withDevMode(true)
    .start(port = 8080)

  println("Visit http://localhost:8080/counter")
}
```

## Documentation

### Core Server
- **[HTTP Server](server.md)** - SpiderServer, RequestHandler, routing, file serving, and security configuration
- **[HTTP Client](client.md)** - Making HTTP requests with the client API
- **[WebSocket Support](websockets.md)** - WebSocket handlers, connections, and protocol details

### Advanced Features
- **[Middleware](middleware.md)** - Request/response processing, CORS, sessions, CSRF, compression, rate limiting
- **[Cookies & Sessions](cookies-sessions.md)** - Cookie handling and session management
- **[Body Parsing](body-parsing.md)** - JSON, forms, and multipart file uploads
- **[Streaming](streaming.md)** - Streaming requests, responses, and Server-Sent Events (SSE)
- **[Advanced Routing](routing.md)** - Path parameters, query strings, and routing helpers

### WebView Framework
- **[WebView Overview](webview/)** - Introduction to the reactive UI framework
- **[HTML DSL](webview/html-dsl.md)** - Type-safe HTML construction with tags, attributes, and components
- **[Styling](webview/styling.md)** - CSS-in-Scala with StyleSheet and CSS utilities
- **[Advanced Topics](webview/advanced.md)** - Lifecycle hooks, error boundaries, actor communication, and DevTools

## Features

### HTTP Server
- Virtual threads for efficient concurrent connections
- HTTP/1.1 support (Keep-alive, chunked transfer encoding)
- Type-safe routing with path parameter extraction
- Static file serving with automatic content types
- Security hardening via configurable limits
- WebSocket upgrade handling
- Middleware for cross-cutting concerns
- Cookie and session management
- CORS and CSRF protection
- Request/response compression
- Rate limiting
- JSON, form, and multipart parsing
- Streaming requests and responses
- Server-Sent Events (SSE)

### WebView Framework
- Server-side reactive UI with automatic updates
- Type-safe event system with compile-time guarantees
- Scalatags-inspired HTML DSL with XSS protection
- Actor-based isolated component state
- Rich lifecycle hooks for side effects
- Error boundaries with graceful recovery
- CSS-in-Scala with scoped styling
- Built-in DevTools for debugging

## Other Libraries

If you like Spider, you should check out [Tapir](https://tapir.softwaremill.com/en/latest/)
