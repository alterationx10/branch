package dev.alteration.branch.spider.server

import java.io.OutputStream

/** A streaming response that writes data incrementally to the output stream.
  *
  * Unlike regular Response[A] which buffers the entire body, StreamingResponse
  * allows you to write data to the response stream as it becomes available.
  *
  * This is useful for:
  * - Large file downloads
  * - Server-Sent Events (SSE)
  * - Streaming data processing
  * - Real-time data feeds
  *
  * Example usage:
  * {{{
  * val streamHandler = new RequestHandler[Unit, StreamingResponse] {
  *   override def handle(request: Request[Unit]): Response[StreamingResponse] = {
  *     val stream = StreamingResponse.create { writer =>
  *       writer.write("Hello, ")
  *       writer.write("streaming ")
  *       writer.write("world!")
  *     }
  *     Response(200, stream, Map("Content-Type" -> List("text/plain")))
  *   }
  * }
  * }}}
  */
trait StreamingResponse {

  /** Write the stream to the output stream.
    *
    * This method is called by the server to write the streaming data.
    * The headers have already been sent at this point.
    *
    * @param output The output stream to write to
    */
  def writeTo(output: OutputStream): Unit

}

object StreamingResponse {

  /** Create a StreamingResponse from a function.
    *
    * @param fn A function that writes data using a StreamWriter
    * @return A StreamingResponse that executes the function
    */
  def create(fn: StreamWriter => Unit): StreamingResponse = {
    new StreamingResponse {
      override def writeTo(output: OutputStream): Unit = {
        val writer = new StreamWriter(output)
        fn(writer)
        writer.flush()
      }
    }
  }

  /** Convert StreamingResponse to Array[Byte] - not directly supported.
    *
    * This is required for the RequestHandler's encoder, but streaming responses
    * are handled specially by the server and this conversion is never actually used.
    */
  given Conversion[StreamingResponse, Array[Byte]] = { _ =>
    // This should never be called - streaming responses are handled specially
    throw new UnsupportedOperationException(
      "StreamingResponse cannot be converted to Array[Byte] - this indicates a server bug"
    )
  }

}

/** A writer for streaming responses.
  *
  * Provides convenient methods for writing data to the output stream.
  */
class StreamWriter(output: OutputStream) {

  /** Write a string to the stream.
    */
  def write(data: String): Unit = {
    output.write(data.getBytes())
    output.flush()
  }

  /** Write bytes to the stream.
    */
  def write(data: Array[Byte]): Unit = {
    output.write(data)
    output.flush()
  }

  /** Write a line (with newline) to the stream.
    */
  def writeLine(data: String): Unit = {
    write(data + "\n")
  }

  /** Get direct access to the underlying OutputStream for advanced usage.
    */
  def outputStream: OutputStream = output

  /** Flush the stream.
    */
  def flush(): Unit = {
    output.flush()
  }

}
