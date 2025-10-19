package dev.alteration.branch.spider.server

/** Server-Sent Events (SSE) support for real-time server-to-client streaming.
  *
  * SSE is a standard for pushing updates from server to client over HTTP.
  * It's simpler than WebSockets and perfect for one-way real-time updates.
  *
  * Example usage:
  * {{{
  * val sseHandler = new RequestHandler[Unit, StreamingResponse] {
  *   override def handle(request: Request[Unit]): Response[StreamingResponse] = {
  *     val stream = StreamingResponse.create { writer =>
  *       val sse = ServerSentEvents(writer)
  *
  *       // Send events
  *       sse.sendEvent("Hello from SSE!")
  *       sse.sendEvent("Another event", eventType = Some("custom"))
  *       sse.sendEvent("Event with ID", id = Some("123"))
  *     }
  *
  *     Response(
  *       statusCode = 200,
  *       body = stream,
  *       headers = Map(
  *         "Content-Type" -> List("text/event-stream"),
  *         "Cache-Control" -> List("no-cache"),
  *         "Connection" -> List("keep-alive")
  *       )
  *     )
  *   }
  * }
  * }}}
  */
class ServerSentEvents(writer: StreamWriter) {

  /** Send an SSE event.
    *
    * @param data The event data
    * @param eventType Optional event type (appears as "event: type" in SSE)
    * @param id Optional event ID (for client reconnection)
    * @param retry Optional retry time in milliseconds
    */
  def sendEvent(
      data: String,
      eventType: Option[String] = None,
      id: Option[String] = None,
      retry: Option[Int] = None
  ): Unit = {
    val builder = new StringBuilder()

    // Event type
    eventType.foreach { t =>
      builder.append(s"event: $t\n")
    }

    // Event ID
    id.foreach { i =>
      builder.append(s"id: $i\n")
    }

    // Retry time
    retry.foreach { r =>
      builder.append(s"retry: $r\n")
    }

    // Data (can be multi-line)
    data.split("\n").foreach { line =>
      builder.append(s"data: $line\n")
    }

    // End of event (required blank line)
    builder.append("\n")

    writer.write(builder.toString())
  }

  /** Send a comment (useful for keeping connection alive).
    */
  def sendComment(comment: String): Unit = {
    writer.write(s": $comment\n\n")
  }

  /** Send a heartbeat to keep the connection alive.
    */
  def sendHeartbeat(): Unit = {
    sendComment("heartbeat")
  }

  /** Flush the stream.
    */
  def flush(): Unit = {
    writer.flush()
  }
}

object ServerSentEvents {

  /** Helper to create a ServerSentEvents instance.
    */
  def apply(writer: StreamWriter): ServerSentEvents = {
    new ServerSentEvents(writer)
  }

}
