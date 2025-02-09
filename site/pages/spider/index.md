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
---

# Spider

*Oh, what a tangled web we weave when first we practice http*

Spider is a lightweight HTTP framework built on top of Java's built-in `HttpServer`. It provides both server and client functionality with a clean Scala API.

## Server Components

### RequestHandler

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
    case HttpMethod.GET -> >> / "greet" => GreeterHandler()
    case HttpMethod.GET -> >> / "echo" / msg => EchoHandler(msg)
  }
}
```

ContextHandlers support:
- Path-based routing with pattern matching
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
      case HttpMethod.GET -> >> / "hello" => GreeterHandler()
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

Spider includes a comprehensive `ContentType` enum covering common MIME types. The content type is automatically set based on file extensions when using `FileContextHandler`.

## Examples

See the test files for more examples of:
- Route handling
- File serving
- Client usage
- JSON responses
- Authentication
- Filters

## Other Libraries

If you like Spider, you should check out [Tapir](https://tapir.softwaremill.com/en/latest/)