---
title: Spider HTTP Client
description: Making HTTP requests with the client API
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - client
---

# HTTP Client

Spider includes a client API built on `java.net.http.HttpClient` for making HTTP requests.

## Basic Usage

```scala
import dev.alteration.branch.spider.client.*

val client = Client.build()

val request = ClientRequest.build(
  uri"http://localhost:8080/api/hello",
  _.GET()
)

val response = client.send(request, HttpResponse.BodyHandlers.ofString())
println(response.body())
```

## URI String Interpolation

Spider provides a convenient `uri` string interpolator:

```scala
val endpoint = "users"
val id = 123

val request = ClientRequest.build(
  uri"https://api.example.com/$endpoint/$id",
  _.GET()
)
```

## HTTP Methods

All standard HTTP methods are supported:

```scala
// GET request
val getRequest = ClientRequest.build(
  uri"https://api.example.com/users",
  _.GET()
)

// POST request with body
val postRequest = ClientRequest.build(
  uri"https://api.example.com/users",
  _.POST(HttpRequest.BodyPublishers.ofString("""{"name": "Alice"}"""))
)

// PUT request
val putRequest = ClientRequest.build(
  uri"https://api.example.com/users/123",
  _.PUT(HttpRequest.BodyPublishers.ofString("""{"name": "Bob"}"""))
)

// DELETE request
val deleteRequest = ClientRequest.build(
  uri"https://api.example.com/users/123",
  _.DELETE()
)

// PATCH request
val patchRequest = ClientRequest.build(
  uri"https://api.example.com/users/123",
  _.method("PATCH", HttpRequest.BodyPublishers.ofString("""{"age": 30}"""))
)
```

## Content Type Helpers

Spider provides helpers for setting content types:

```scala
val request = ClientRequest.build(
  uri"https://api.example.com/users",
  _.POST(HttpRequest.BodyPublishers.ofString("""{"name": "Alice"}"""))
    .header("Content-Type", ContentType.json.value)
)
```

## Custom Headers

Add custom headers to requests:

```scala
val request = ClientRequest.build(
  uri"https://api.example.com/users",
  _.GET()
    .header("Authorization", "Bearer token123")
    .header("X-Custom-Header", "value")
)
```

## Response Body Handlers

Use different body handlers based on your needs:

```scala
import java.net.http.HttpResponse

// String response
val stringResponse = client.send(
  request,
  HttpResponse.BodyHandlers.ofString()
)

// Byte array response
val bytesResponse = client.send(
  request,
  HttpResponse.BodyHandlers.ofByteArray()
)

// Save to file
val fileResponse = client.send(
  request,
  HttpResponse.BodyHandlers.ofFile(Path.of("output.json"))
)

// Discard response body
val discardResponse = client.send(
  request,
  HttpResponse.BodyHandlers.discarding()
)
```

## Example: Complete Request Flow

```scala
import dev.alteration.branch.spider.client.*
import java.net.http.HttpResponse

val client = Client.build()

// POST JSON data
val jsonData = """{"username": "alice", "email": "alice@example.com"}"""
val postRequest = ClientRequest.build(
  uri"https://api.example.com/users",
  _.POST(HttpRequest.BodyPublishers.ofString(jsonData))
    .header("Content-Type", ContentType.json.value)
    .header("Accept", ContentType.json.value)
)

val response = client.send(postRequest, HttpResponse.BodyHandlers.ofString())

println(s"Status: ${response.statusCode()}")
println(s"Body: ${response.body()}")

// GET request with query parameters
val userId = 123
val getRequest = ClientRequest.build(
  uri"https://api.example.com/users/$userId",
  _.GET()
    .header("Authorization", "Bearer token123")
)

val getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString())
println(getResponse.body())
```

## Next Steps

- Build web applications with the [HTTP Server](/spider/server)
- Add real-time features with [WebSocket Support](/spider/websockets)
- Create reactive UIs with [WebView](/spider/webview/)
