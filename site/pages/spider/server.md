---
title: Spider HTTP Server
description: Custom HTTP/1.1 server with routing and security features
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - server
---

# HTTP Server

Spider includes a custom HTTP/1.1 server implementation built on `java.net.ServerSocket` with virtual threads for lightweight concurrency.

## RequestHandler

The core building block is the `RequestHandler[I,O]` trait which handles converting HTTP requests into your input model `I` and converting your output model `O` back into HTTP responses:

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

### Example Handler

```scala
case class GreeterHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(200, "Hello!")
  }
}

case class EchoHandler(message: String) extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(200, s"Echo: $message")
  }
}
```

## SpiderServer

The `SpiderServer` is the core HTTP/1.1 server implementation. It uses `java.net.ServerSocket` with virtual threads for lightweight concurrency:

```scala
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.common.HttpMethod

val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, "greet" :: Nil) => GreeterHandler()
  case (HttpMethod.GET, "echo" :: msg :: Nil) => EchoHandler(msg)
}

val server = new SpiderServer(
  port = 8080,
  router = router,
  config = ServerConfig.default
).withShutdownHook()

server.start() // Blocking call
```

### Key Features

- **Virtual threads**: Uses Project Loom for efficient concurrent connections
- **HTTP/1.1 support**: Keep-alive, chunked transfer encoding, proper CRLF handling
- **WebSocket support**: Integrated WebSocket upgrade handling via `webSocketRouter` parameter
- **Type-safe routing**: PartialFunction-based routing with pattern matching
- **Security hardening**: Configurable limits via `ServerConfig`

## ServerConfig

Spider provides built-in DoS protection and security hardening through `ServerConfig`:

```scala
case class ServerConfig(
                         maxRequestLineLength: Int = 8192, // 8KB
                         maxHeaderCount: Int = 100,
                         maxHeaderSize: Int = 8192, // 8KB per header
                         maxTotalHeadersSize: Int = 65536, // 64KB combined
                         maxRequestBodySize: Option[Long] = Some(10L * 1024 * 1024), // 10MB
                         socketTimeout: Int = 30000 // 30 seconds
                       )
```

### Pre-configured Profiles

```scala
// Development: Relaxed limits for debugging
ServerConfig.development

// Default: Balanced for general use
ServerConfig.default

// Strict: Tight security for production
ServerConfig.strict
```

## FileHandler

For serving static files, Spider provides `FileHandler`:

```scala
import dev.alteration.branch.spider.server.*
import java.nio.file.Path

val fileHandler = new FileHandler(Path.of("public"))

val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
  case (HttpMethod.GET, path) => fileHandler
}
```

This will automatically:

- Serve files from the specified root directory
- Set appropriate content types based on file extensions
- Return 404 for missing files
- Support subdirectories

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

Spider includes a comprehensive `ContentType` enum covering common MIME types. The content type is automatically set based on file extensions when using `FileHandler`.

## Advanced Features

Spider provides many advanced features for building production-ready web applications:

### Middleware
Add cross-cutting concerns like logging, authentication, CORS, and more. See [Middleware](middleware.md).

### Sessions & Cookies
Manage user sessions and cookies with built-in support for signed cookies and session stores. See [Cookies & Sessions](cookies-sessions.md).

### Request Parsing
Parse JSON, forms, and multipart file uploads with size limits and validation. See [Body Parsing](body-parsing.md).

### Streaming
Handle large uploads and downloads efficiently, plus Server-Sent Events for real-time updates. See [Streaming](streaming.md).

### Advanced Routing
Extract path parameters, parse query strings, and organize routes with helpers. See [Advanced Routing](routing.md).

## Next Steps

- Learn about [Middleware](middleware.md) for request/response processing
- Explore [Advanced Routing](routing.md) for path parameters and query strings
- Read about [Body Parsing](body-parsing.md) for handling JSON, forms, and file uploads
- Discover [Streaming](streaming.md) for large payloads and SSE
- Learn about the [HTTP Client](client.md) for making requests
- Explore [WebSocket Support](websockets.md) for real-time communication
- Build reactive UIs with [WebView](webview/)
