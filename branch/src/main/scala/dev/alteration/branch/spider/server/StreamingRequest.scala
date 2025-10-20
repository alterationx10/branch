package dev.alteration.branch.spider.server

import java.io.InputStream

/** A streaming request body that allows reading chunks incrementally.
  *
  * Unlike regular Request[Array[Byte]] which buffers the entire body in memory,
  * StreamingRequest provides access to the raw input stream to read data as it
  * arrives. This is useful for:
  *   - Large file uploads
  *   - Chunked transfer encoding processing
  *   - Streaming data ingestion
  *   - Memory-efficient request handling
  *
  * Example usage:
  * {{{
  * val uploadHandler = new StreamingRequestHandler[String] {
  *   override def handle(request: Request[StreamingRequest]): Response[String] = {
  *     val stream = request.body
  *     var totalBytes = 0L
  *
  *     // Process chunks as they arrive
  *     stream.readChunks { chunk =>
  *       totalBytes += chunk.length
  *       // Process chunk (e.g., write to file, forward to another service)
  *     }
  *
  *     Response(200, s"Uploaded $totalBytes bytes")
  *   }
  * }
  * }}}
  *
  * @param input
  *   The input stream to read from
  * @param contentLength
  *   Optional content length from headers
  * @param isChunked
  *   Whether the request uses chunked transfer encoding
  * @param maxBodySize
  *   Optional maximum body size limit
  */
class StreamingRequest(
    private val input: InputStream,
    val contentLength: Option[Long],
    val isChunked: Boolean,
    private val maxBodySize: Option[Long] = None
) {

  /** Read the entire body into memory as a byte array.
    *
    * Warning: This defeats the purpose of streaming and should only be used
    * when you know the body is reasonably sized.
    */
  def readAll(): Array[Byte] = {
    if (isChunked) {
      readChunkedBody()
    } else {
      contentLength match {
        case Some(length) =>
          val buffer    = new Array[Byte](length.toInt)
          var offset    = 0
          var remaining = length.toInt

          while (remaining > 0) {
            val bytesRead = input.read(buffer, offset, remaining)
            if (bytesRead == -1) {
              throw new IllegalStateException("Unexpected end of stream")
            }
            offset += bytesRead
            remaining -= bytesRead
          }
          buffer

        case None =>
          Array.empty[Byte]
      }
    }
  }

  /** Read and process chunks as they arrive.
    *
    * This method calls the provided function for each chunk of data. For
    * chunked encoding, each chunk is the actual HTTP chunk. For Content-Length,
    * data is read in reasonable buffer sizes.
    *
    * @param chunkSize
    *   The buffer size to use when reading (default 8KB)
    * @param fn
    *   Function to call for each chunk
    */
  def readChunks(chunkSize: Int = 8192)(fn: Array[Byte] => Unit): Unit = {
    if (isChunked) {
      readChunksFromChunkedBody(fn)
    } else {
      readChunksFromContentLength(chunkSize, fn)
    }
  }

  /** Process the stream with access to a StreamReader.
    *
    * This provides a more convenient API for reading data.
    *
    * @param fn
    *   Function that processes the stream using StreamReader
    */
  def withReader[A](fn: StreamReader => A): A = {
    val reader = new StreamReader(input, contentLength, isChunked, maxBodySize)
    fn(reader)
  }

  /** Read chunks from a request with Content-Length.
    */
  private def readChunksFromContentLength(
      chunkSize: Int,
      fn: Array[Byte] => Unit
  ): Unit = {
    contentLength match {
      case Some(length) =>
        var remaining = length
        var totalRead = 0L

        while (remaining > 0) {
          // Check max body size
          maxBodySize.foreach { maxSize =>
            if (totalRead >= maxSize) {
              throw new IllegalStateException(
                s"Request body exceeds maximum size: $maxSize bytes"
              )
            }
          }

          val bufferSize = Math.min(chunkSize.toLong, remaining).toInt
          val buffer     = new Array[Byte](bufferSize)
          val bytesRead  = input.read(buffer, 0, bufferSize)

          if (bytesRead == -1) {
            throw new IllegalStateException("Unexpected end of stream")
          }

          totalRead += bytesRead
          remaining -= bytesRead

          // Only pass the actual bytes read (might be less than buffer size)
          if (bytesRead < buffer.length) {
            fn(buffer.take(bytesRead))
          } else {
            fn(buffer)
          }
        }

      case None =>
        // No content length, nothing to read
        ()
    }
  }

  /** Read chunks from a chunked transfer encoded request.
    */
  private def readChunksFromChunkedBody(fn: Array[Byte] => Unit): Unit = {
    var done      = false
    var totalSize = 0L

    while (!done) {
      // Read chunk size line
      val sizeLine  = readLine(input)
      val chunkSize = Integer.parseInt(sizeLine.split(";")(0).trim, 16)

      if (chunkSize == 0) {
        // Last chunk, read trailing headers
        var trailingDone = false
        while (!trailingDone) {
          val line = readLine(input)
          if (line.isEmpty) trailingDone = true
        }
        done = true
      } else {
        // Check max body size
        totalSize += chunkSize
        maxBodySize.foreach { maxSize =>
          if (totalSize > maxSize) {
            throw new IllegalStateException(
              s"Request body exceeds maximum size: $maxSize bytes"
            )
          }
        }

        // Read chunk data
        val chunk     = new Array[Byte](chunkSize)
        var offset    = 0
        var remaining = chunkSize

        while (remaining > 0) {
          val bytesRead = input.read(chunk, offset, remaining)
          if (bytesRead == -1) {
            throw new IllegalStateException("Unexpected end of stream")
          }
          offset += bytesRead
          remaining -= bytesRead
        }

        fn(chunk)

        // Read trailing CRLF
        val crlf = new Array[Byte](2)
        if (input.read(crlf) != 2 || crlf(0) != '\r' || crlf(1) != '\n') {
          throw new IllegalArgumentException("Expected CRLF after chunk")
        }
      }
    }
  }

  /** Read a chunked body into a single array (for readAll).
    */
  private def readChunkedBody(): Array[Byte] = {
    val chunks = scala.collection.mutable.ArrayBuffer.empty[Array[Byte]]
    readChunksFromChunkedBody(chunk => chunks += chunk)
    chunks.flatten.toArray
  }

  /** Read a line from the input stream (CRLF terminated).
    */
  private def readLine(input: InputStream): String = {
    val line    = new StringBuilder
    var current = input.read()

    if (current == -1) {
      throw new IllegalStateException("Unexpected end of stream")
    }

    var done = false
    while (current != -1 && !done) {
      val char = current.toChar

      if (char == '\r') {
        input.mark(1)
        val next = input.read()
        if (next == '\n') {
          done = true
        } else {
          input.reset()
          line.append(char)
          current = input.read()
        }
      } else {
        line.append(char)
        current = input.read()
      }
    }

    line.toString
  }

  /** Get direct access to the underlying InputStream for advanced usage.
    *
    * Warning: Using this directly bypasses chunk parsing and size limits. Only
    * use if you know what you're doing.
    */
  def inputStream: InputStream = input
}

object StreamingRequest {

  /** Convert StreamingRequest from Array[Byte] - not meaningful.
    *
    * This is required for the RequestHandler's decoder, but streaming requests
    * are created directly from the input stream, not from buffered bytes.
    */
  given Conversion[Array[Byte], StreamingRequest] = { _ =>
    throw new UnsupportedOperationException(
      "Cannot convert Array[Byte] to StreamingRequest - this indicates a server bug"
    )
  }

}

/** A reader for streaming request bodies.
  *
  * Provides convenient methods for reading data from the stream.
  */
class StreamReader(
    private val input: InputStream,
    val contentLength: Option[Long],
    val isChunked: Boolean,
    private val maxBodySize: Option[Long]
) {

  private var totalBytesRead = 0L

  /** Read a chunk of data up to the specified size.
    *
    * @param maxBytes
    *   Maximum bytes to read
    * @return
    *   The chunk of data, or empty array if EOF
    */
  def read(maxBytes: Int): Array[Byte] = {
    // Check size limit
    maxBodySize.foreach { maxSize =>
      if (totalBytesRead >= maxSize) {
        throw new IllegalStateException(
          s"Request body exceeds maximum size: $maxSize bytes"
        )
      }
    }

    val buffer    = new Array[Byte](maxBytes)
    val bytesRead = input.read(buffer, 0, maxBytes)

    if (bytesRead == -1) {
      Array.empty[Byte]
    } else {
      totalBytesRead += bytesRead
      if (bytesRead < buffer.length) {
        buffer.take(bytesRead)
      } else {
        buffer
      }
    }
  }

  /** Read data into a provided buffer.
    *
    * @param buffer
    *   The buffer to read into
    * @param offset
    *   Offset in the buffer to start writing
    * @param length
    *   Maximum number of bytes to read
    * @return
    *   Number of bytes actually read, or -1 if EOF
    */
  def read(buffer: Array[Byte], offset: Int, length: Int): Int = {
    // Check size limit
    maxBodySize.foreach { maxSize =>
      if (totalBytesRead >= maxSize) {
        throw new IllegalStateException(
          s"Request body exceeds maximum size: $maxSize bytes"
        )
      }
    }

    val bytesRead = input.read(buffer, offset, length)
    if (bytesRead > 0) {
      totalBytesRead += bytesRead
    }
    bytesRead
  }

  /** Get total bytes read so far.
    */
  def bytesRead: Long = totalBytesRead

  /** Get direct access to the underlying InputStream.
    */
  def inputStream: InputStream = input
}
