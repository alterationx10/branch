package dev.alteration.branch.spider.server

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import scala.util.Try

/** Writes HTTP/1.1 responses to an OutputStream.
  */
object HttpWriter {

  /** Map of common HTTP status codes to their reason phrases. */
  private val statusMessages = Map(
    200 -> "OK",
    201 -> "Created",
    204 -> "No Content",
    301 -> "Moved Permanently",
    302 -> "Found",
    304 -> "Not Modified",
    400 -> "Bad Request",
    401 -> "Unauthorized",
    403 -> "Forbidden",
    404 -> "Not Found",
    405 -> "Method Not Allowed",
    500 -> "Internal Server Error",
    501 -> "Not Implemented",
    502 -> "Bad Gateway",
    503 -> "Service Unavailable"
  )

  /** Get the reason phrase for a status code, or a generic message.
    *
    * @param statusCode
    *   The HTTP status code
    * @return
    *   The reason phrase (e.g., "OK", "Not Found")
    */
  private def reasonPhrase(statusCode: Int): String = {
    statusMessages.getOrElse(statusCode, "Unknown")
  }

  /** Write an HTTP response to an output stream.
    *
    * This method writes:
    *   - Status line: HTTP/1.1 200 OK
    *   - Headers with CRLF line endings
    *   - Blank line
    *   - Response body
    *
    * @param response
    *   The Response to write
    * @param output
    *   The OutputStream to write to (typically from a Socket)
    * @param encoder
    *   Conversion to convert response body to bytes
    * @return
    *   Try indicating success or failure
    */
  def write[A](response: Response[A], output: OutputStream)(using
      encoder: Conversion[A, Array[Byte]]
  ): Try[Unit] = {
    Try {
      val writer = new BufferedWriter(output)

      // Encode body first to get length
      val bodyBytes = encoder(response.body)

      // Add Content-Length header if not already present
      val hasContentLength = response.headers.exists { case (k, _) =>
        k.equalsIgnoreCase("Content-Length")
      }

      val headers = if (!hasContentLength) {
        response.headers + ("Content-Length" -> List(bodyBytes.length.toString))
      } else {
        response.headers
      }

      // Write status line: HTTP/1.1 200 OK\r\n
      writer.writeLine(
        s"HTTP/1.1 ${response.statusCode} ${reasonPhrase(response.statusCode)}"
      )

      // Write headers (including Content-Length)
      headers.foreach { case (name, values) =>
        values.foreach { value =>
          writer.writeLine(s"$name: $value")
        }
      }

      // Blank line to separate headers from body
      writer.writeLine("")

      // Write body
      if (bodyBytes.nonEmpty) {
        writer.writeBytes(bodyBytes)
      }

      // Flush to ensure all data is sent
      writer.flush()
    }
  }

  /** A simple buffered writer for writing HTTP responses with proper CRLF
    * line endings.
    *
    * @param output
    *   The underlying OutputStream
    */
  private class BufferedWriter(output: OutputStream) {

    /** Write a line with CRLF terminator.
      *
      * @param line
      *   The line to write (without CRLF)
      */
    def writeLine(line: String): Unit = {
      val bytes = (line + "\r\n").getBytes(StandardCharsets.UTF_8)
      output.write(bytes)
    }

    /** Write raw bytes to the output stream.
      *
      * @param bytes
      *   The bytes to write
      */
    def writeBytes(bytes: Array[Byte]): Unit = {
      output.write(bytes)
    }

    /** Flush the underlying output stream.
      */
    def flush(): Unit = {
      output.flush()
    }
  }
}
