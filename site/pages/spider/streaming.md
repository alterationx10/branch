---
title: Spider Streaming
description: Streaming requests, responses, and Server-Sent Events
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags:
  - http
  - streaming
  - sse
---

# Streaming

Spider supports streaming for both requests and responses, enabling efficient handling of large payloads and real-time data streams.

## Streaming Requests

For handling large request bodies without loading them entirely into memory, use `StreamingRequestHandler`:

```scala
import dev.alteration.branch.spider.server.*
import java.nio.file.{Files, Paths}

case class FileUploadHandler() extends StreamingRequestHandler[String] {
  override def handle(request: Request[StreamingRequest]): Response[String] = {
    val output = Files.newOutputStream(Paths.get("/uploads/file.bin"))
    var totalBytes = 0L

    try {
      // Read request body in chunks
      request.body.readChunks { chunk =>
        output.write(chunk)
        totalBytes += chunk.length
        // Process chunk (calculate hash, scan for viruses, etc.)
      }

      Response(200, s"Uploaded $totalBytes bytes")
    } finally {
      output.close()
    }
  }
}
```

### StreamingRequest API

```scala
class StreamingRequest {
  // Read chunks with a callback
  def readChunks(chunkSize: Int = 8192)(process: Array[Byte] => Unit): Long

  // Read with a stream reader for more control
  def withReader[A](f: StreamReader => A): A

  // Get content length if available
  def contentLength: Option[Long]

  // Check if using chunked transfer encoding
  def isChunked: Boolean

  // Get the underlying input stream (advanced)
  def inputStream: InputStream
}
```

### StreamReader

For more control, use the `StreamReader`:

```scala
case class CustomUploadHandler() extends StreamingRequestHandler[String] {
  override def handle(request: Request[StreamingRequest]): Response[String] = {
    request.body.withReader { reader =>
      // Read fixed-size chunks
      val chunk1 = reader.read(1024)  // Read up to 1KB
      val chunk2 = reader.read(2048)  // Read up to 2KB

      // Read all remaining data
      val remaining = reader.readRemaining()

      // Skip bytes
      reader.skip(100)

      // Check bytes read
      val totalRead = reader.bytesRead

      Response(200, s"Read $totalRead bytes")
    }
  }
}
```

### Streaming Multipart Uploads

Parse multipart uploads without loading everything into memory:

```scala
import dev.alteration.branch.spider.server.*
import java.nio.file.{Files, Path}

case class StreamingUploadHandler() extends StreamingRequestHandler[String] {
  override def handle(request: Request[StreamingRequest]): Response[String] = {
    val boundary = request.multipartBoundary.getOrElse {
      return Response(400, "Missing boundary")
    }

    val config = BodyParser.ParserConfig.default
    val tempDir = Some(Path.of("/tmp/uploads"))

    MultipartParser.parseMultipartStreaming(
      request.body,
      boundary,
      config,
      tempDir
    ) match {
      case Success(multipart) =>
        // Files are saved to temp location
        val fileCount = multipart.files.values.flatten.size

        // Process each file
        multipart.files.values.flatten.foreach { upload =>
          upload.contentLength.foreach { size =>
            println(s"File: ${upload.filename.getOrElse("unknown")}, size: $size")
          }

          // Read file content as stream
          upload.withInputStream { stream =>
            // Process stream (copy to permanent storage, etc.)
            Files.copy(stream, Path.of(s"/permanent/${upload.filename.get}"))
          }
        }

        Response(200, s"Uploaded $fileCount file(s)")

      case Failure(error) =>
        Response(400, s"Upload failed: ${error.getMessage}")
    }
  }
}
```

## Streaming Responses

Send responses incrementally instead of buffering the entire response:

```scala
import dev.alteration.branch.spider.server.*

case class StreamingDownloadHandler() extends RequestHandler[Unit, StreamingResponse] {
  override def handle(request: Request[Unit]): Response[StreamingResponse] = {
    val stream = StreamingResponse.create { writer =>
      // Write data incrementally
      writer.write("Chunk 1\n".getBytes())
      writer.flush()

      Thread.sleep(1000)

      writer.write("Chunk 2\n".getBytes())
      writer.flush()

      Thread.sleep(1000)

      writer.write("Chunk 3\n".getBytes())
      writer.flush()
    }

    Response(
      statusCode = 200,
      body = stream,
      headers = Map(
        "Content-Type" -> List("text/plain"),
        "Transfer-Encoding" -> List("chunked")
      )
    )
  }
}
```

### Streaming Files

Stream large files efficiently:

```scala
import java.nio.file.{Files, Paths}

case class FileDownloadHandler() extends RequestHandler[Unit, StreamingResponse] {
  override def handle(request: Request[Unit]): Response[StreamingResponse] = {
    val filePath = Paths.get("/data/large-file.zip")

    if (!Files.exists(filePath)) {
      return Response(404, StreamingResponse.empty)
    }

    val stream = StreamingResponse.fromFile(filePath)

    Response(
      statusCode = 200,
      body = stream,
      headers = Map(
        "Content-Type" -> List("application/zip"),
        "Content-Disposition" -> List("attachment; filename=\"large-file.zip\""),
        "Content-Length" -> List(Files.size(filePath).toString)
      )
    )
  }
}
```

### StreamWriter API

```scala
trait StreamWriter {
  // Write bytes
  def write(data: Array[Byte]): Unit
  def write(data: String): Unit

  // Flush the stream
  def flush(): Unit

  // Write and flush
  def writeFlush(data: Array[Byte]): Unit
  def writeFlush(data: String): Unit
}
```

## Server-Sent Events (SSE)

Server-Sent Events enable real-time server-to-client streaming:

```scala
import dev.alteration.branch.spider.server.*

case class SSEHandler() extends RequestHandler[Unit, StreamingResponse] {
  override def handle(request: Request[Unit]): Response[StreamingResponse] = {
    val stream = StreamingResponse.create { writer =>
      val sse = ServerSentEvents(writer)

      // Send events
      sse.sendEvent("Hello from server!")
      sse.sendEvent("Another update", eventType = Some("message"))
      sse.sendEvent("Event with ID", id = Some("1"))

      // Send periodic updates
      for (i <- 1 to 10) {
        Thread.sleep(1000)
        sse.sendEvent(s"Update $i", eventType = Some("update"))
      }
    }

    Response(
      statusCode = 200,
      body = stream,
      headers = Map(
        "Content-Type" -> List("text/event-stream"),
        "Cache-Control" -> List("no-cache"),
        "Connection" -> List("keep-alive")
      )
    )
  }
}
```

### SSE API

```scala
class ServerSentEvents {
  // Send an event
  def sendEvent(
    data: String,
    eventType: Option[String] = None,
    id: Option[String] = None,
    retry: Option[Int] = None
  ): Unit

  // Send a comment (keeps connection alive)
  def sendComment(comment: String): Unit

  // Send a heartbeat
  def sendHeartbeat(): Unit

  // Flush the stream
  def flush(): Unit
}
```

### SSE Event Format

```scala
// Simple event
sse.sendEvent("Hello!")
// Outputs:
// data: Hello!
//

// Event with type
sse.sendEvent("Stock price: $150", eventType = Some("stock-update"))
// Outputs:
// event: stock-update
// data: Stock price: $150
//

// Event with ID (for reconnection)
sse.sendEvent("Update 1", id = Some("1"))
// Outputs:
// id: 1
// data: Update 1
//

// Multi-line data
sse.sendEvent("Line 1\nLine 2\nLine 3")
// Outputs:
// data: Line 1
// data: Line 2
// data: Line 3
//
```

### SSE with Heartbeats

Keep connections alive with periodic heartbeats:

```scala
case class SSEWithHeartbeatHandler() extends RequestHandler[Unit, StreamingResponse] {
  override def handle(request: Request[Unit]): Response[StreamingResponse] = {
    val stream = StreamingResponse.create { writer =>
      val sse = ServerSentEvents(writer)

      // Send updates with heartbeats
      for (i <- 1 to 100) {
        if (i % 10 == 0) {
          // Send heartbeat every 10 iterations
          sse.sendHeartbeat()
        } else {
          sse.sendEvent(s"Update $i")
        }
        Thread.sleep(1000)
      }
    }

    Response(
      statusCode = 200,
      body = stream,
      headers = Map(
        "Content-Type" -> List("text/event-stream"),
        "Cache-Control" -> List("no-cache"),
        "Connection" -> List("keep-alive"),
        "X-Accel-Buffering" -> List("no") // Disable nginx buffering
      )
    )
  }
}
```

### Client-Side SSE

JavaScript client example:

```javascript
const eventSource = new EventSource('/sse/updates');

// Handle messages
eventSource.onmessage = (event) => {
  console.log('Received:', event.data);
};

// Handle custom event types
eventSource.addEventListener('stock-update', (event) => {
  console.log('Stock update:', event.data);
});

// Handle errors
eventSource.onerror = (error) => {
  console.error('SSE error:', error);
  eventSource.close();
};

// Close connection
eventSource.close();
```

### SSE Best Practices

1. **Set proper headers**: Always include `Content-Type: text/event-stream`
2. **Disable caching**: Use `Cache-Control: no-cache`
3. **Use heartbeats**: Keep connections alive with periodic comments
4. **Handle disconnections**: Clients will automatically reconnect
5. **Use event IDs**: Enable clients to resume from last event
6. **Set retry**: Suggest reconnection delay with `retry` field

## Complete SSE Example

```scala
import dev.alteration.branch.spider.server.*
import scala.concurrent.duration.*

case class StockTickerHandler(symbol: String) extends RequestHandler[Unit, StreamingResponse] {
  override def handle(request: Request[Unit]): Response[StreamingResponse] = {
    val stream = StreamingResponse.create { writer =>
      val sse = ServerSentEvents(writer)
      var eventId = 0

      try {
        // Send initial connection confirmation
        sse.sendEvent(
          s"Connected to $symbol ticker",
          eventType = Some("connection"),
          retry = Some(5000) // Reconnect after 5 seconds
        )

        // Send stock updates
        while (true) {
          eventId += 1

          // Simulate stock price
          val price = getStockPrice(symbol)
          val change = calculateChange(price)

          // Send update
          sse.sendEvent(
            s"""{"symbol":"$symbol","price":$price,"change":$change}""",
            eventType = Some("price-update"),
            id = Some(eventId.toString)
          )

          // Heartbeat every 10 events
          if (eventId % 10 == 0) {
            sse.sendHeartbeat()
          }

          Thread.sleep(1000) // Update every second
        }
      } catch {
        case _: InterruptedException =>
          // Client disconnected
          println(s"Client disconnected from $symbol ticker")
      }
    }

    Response(
      statusCode = 200,
      body = stream,
      headers = Map(
        "Content-Type" -> List("text/event-stream"),
        "Cache-Control" -> List("no-cache"),
        "Connection" -> List("keep-alive"),
        "X-Accel-Buffering" -> List("no")
      )
    )
  }
}
```

## Performance Considerations

1. **Memory efficiency**: Streaming avoids loading entire payloads into memory
2. **Chunked transfer**: Use chunked encoding for unknown content lengths
3. **Buffer management**: Flush regularly to prevent buffering delays
4. **Connection limits**: Monitor open SSE connections
5. **Cleanup**: Ensure resources are closed properly
6. **Backpressure**: Handle slow clients appropriately

## Next Steps

- Learn about [Body Parsing](body-parsing.md) for standard request parsing
- Explore [Middleware](middleware.md) for request processing
- Return to [HTTP Server](server.md)
