package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.common.HttpMethod

import java.io.{BufferedInputStream, InputStream}
import java.net.URI
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Try, Success, Failure}

/** Parses HTTP/1.1 requests from an InputStream into Request objects.
  */
object HttpParser {

  /** Exception thrown when the connection is closed gracefully (EOF at start). */
  class ConnectionClosedException extends Exception("Connection closed")

  /** Exception thrown when a request exceeds configured limits. */
  sealed abstract class LimitExceededException(message: String)
      extends IllegalArgumentException(message)

  class RequestLineTooLongException(length: Int, limit: Int)
      extends LimitExceededException(
        s"Request line too long: $length bytes (limit: $limit)"
      )

  class TooManyHeadersException(count: Int, limit: Int)
      extends LimitExceededException(
        s"Too many headers: $count (limit: $limit)"
      )

  class HeaderTooLargeException(name: String, size: Int, limit: Int)
      extends LimitExceededException(
        s"Header '$name' too large: $size bytes (limit: $limit)"
      )

  class HeadersSizeTooLargeException(size: Int, limit: Int)
      extends LimitExceededException(
        s"Total headers size too large: $size bytes (limit: $limit)"
      )

  class RequestBodyTooLargeException(size: Long, limit: Long)
      extends LimitExceededException(
        s"Request body too large: $size bytes (limit: $limit)"
      )

  /** Result of parsing an HTTP request. */
  case class ParseResult(
      method: HttpMethod,
      uri: URI,
      httpVersion: String,
      headers: Map[String, List[String]],
      body: Array[Byte]
  )

  /** Parse an HTTP request from an input stream.
    *
    * @param input
    *   The input stream to read from (typically from a Socket)
    * @param config
    *   Server configuration with limits (optional, uses defaults if not
    *   provided)
    * @return
    *   Try containing either a ParseResult or an error
    */
  def parse(
      input: InputStream,
      config: ServerConfig = ServerConfig.default
  ): Try[ParseResult] = {
    val buffered = new BufferedInputStream(input)

    for {
      requestLine <- parseRequestLine(buffered, config)
      (method, uri, version) = requestLine
      headers <- parseHeaders(buffered, config)
      body <- readBody(buffered, headers, config)
    } yield ParseResult(method, uri, version, headers, body)
  }

  /** Parse the HTTP request line: METHOD /path HTTP/1.1
    *
    * @return
    *   Try containing (HttpMethod, URI, httpVersion)
    */
  private def parseRequestLine(
      input: InputStream,
      config: ServerConfig
  ): Try[(HttpMethod, URI, String)] = {
    readLine(input, config.maxRequestLineLength).flatMap { line =>
      line.split(" ") match {
        case Array(method, path, version) =>
          for {
            httpMethod <- HttpMethod
                            .fromString(method)
                            .toRight(new IllegalArgumentException(
                              s"Invalid HTTP method: $method"
                            ))
                            .toTry
            uri        <- Try(new URI(path))
          } yield (httpMethod, uri, version)

        case _ =>
          Failure(
            new IllegalArgumentException(s"Invalid request line: $line")
          )
      }
    }
  }

  /** Parse HTTP headers until we hit a blank line (\r\n\r\n).
    *
    * Headers are stored in a map with case-preserved keys. Multiple values for
    * the same header are collected into a list.
    *
    * @return
    *   Map of header names to list of values
    */
  private def parseHeaders(
      input: InputStream,
      config: ServerConfig
  ): Try[Map[String, List[String]]] = {
    @tailrec
    def readHeaderLines(
        acc: mutable.Map[String, mutable.ListBuffer[String]],
        totalHeadersSize: Int
    ): Try[Map[String, List[String]]] = {
      // Check header count limit
      if (acc.size >= config.maxHeaderCount) {
        return Failure(
          new TooManyHeadersException(acc.size + 1, config.maxHeaderCount)
        )
      }

      readLine(input, config.maxHeaderSize) match {
        case Success(line) if line.isEmpty =>
          // Empty line indicates end of headers
          Success(acc.view.mapValues(_.toList).toMap)

        case Success(line) =>
          // Check total headers size
          val newTotalSize = totalHeadersSize + line.length + 2 // +2 for \r\n
          if (newTotalSize > config.maxTotalHeadersSize) {
            return Failure(
              new HeadersSizeTooLargeException(
                newTotalSize,
                config.maxTotalHeadersSize
              )
            )
          }

          // Parse "HeaderName: value"
          line.indexOf(':') match {
            case -1 =>
              Failure(new IllegalArgumentException(s"Invalid header: $line"))
            case colonIdx =>
              val name = line.substring(0, colonIdx).trim
              val value = line.substring(colonIdx + 1).trim

              // Check individual header value size
              if (value.length > config.maxHeaderSize) {
                return Failure(
                  new HeaderTooLargeException(
                    name,
                    value.length,
                    config.maxHeaderSize
                  )
                )
              }

              // Accumulate values for this header
              if (!acc.contains(name)) {
                acc(name) = mutable.ListBuffer.empty[String]
              }
              acc(name) += value

              readHeaderLines(acc, newTotalSize)
          }

        case Failure(e) =>
          Failure(e)
      }
    }

    readHeaderLines(mutable.Map.empty, 0)
  }

  /** Read the request body based on the Content-Length or Transfer-Encoding
    * header.
    *
    * Supports both Content-Length and chunked transfer encoding. If
    * Content-Length is not present or is 0, returns an empty byte array.
    *
    * @param headers
    *   The parsed headers map
    * @param config
    *   Server configuration with body size limit
    * @return
    *   The request body as a byte array
    */
  private def readBody(
      input: InputStream,
      headers: Map[String, List[String]],
      config: ServerConfig
  ): Try[Array[Byte]] = {
    // Check for Transfer-Encoding: chunked
    val isChunked = headers
      .find { case (k, _) => k.equalsIgnoreCase("Transfer-Encoding") }
      .flatMap(_._2.headOption)
      .exists(_.toLowerCase.contains("chunked"))

    if (isChunked && config.enableChunkedEncoding) {
      // Read chunked body
      readChunkedBody(input, config)
    } else {
      // Read body with Content-Length
      readContentLengthBody(input, headers, config)
    }
  }

  /** Read a request body with Content-Length header.
    */
  private def readContentLengthBody(
      input: InputStream,
      headers: Map[String, List[String]],
      config: ServerConfig
  ): Try[Array[Byte]] = {
    // Look for Content-Length header (case-insensitive)
    val contentLengthOpt = headers
      .find { case (k, _) => k.equalsIgnoreCase("Content-Length") }
      .flatMap(_._2.headOption)
      .flatMap(s => Try(s.toLong).toOption)

    contentLengthOpt match {
      case Some(length) if length > 0 =>
        // Check body size limit
        config.maxRequestBodySize match {
          case Some(maxSize) if length > maxSize =>
            Failure(new RequestBodyTooLargeException(length, maxSize))

          case _ =>
            // Body size is within limits, read it
            Try {
              // Check if length fits in an Int (Array size limit)
              if (length > Int.MaxValue) {
                throw new RequestBodyTooLargeException(
                  length,
                  Int.MaxValue.toLong
                )
              }

              val buffer = new Array[Byte](length.toInt)
              var offset = 0
              var remaining = length.toInt

              // Read until we have the full content-length
              while (remaining > 0) {
                val bytesRead = input.read(buffer, offset, remaining)
                if (bytesRead == -1) {
                  throw new IllegalStateException(
                    "Unexpected end of stream while reading body"
                  )
                }
                offset += bytesRead
                remaining -= bytesRead
              }

              buffer
            }
        }

      case _ =>
        // No body or Content-Length: 0
        Success(Array.empty[Byte])
    }
  }

  /** Read a chunked request body (Transfer-Encoding: chunked).
    *
    * Chunked format: size-in-hex\r\n data\r\n ... 0\r\n\r\n
    */
  private def readChunkedBody(
      input: InputStream,
      config: ServerConfig
  ): Try[Array[Byte]] = {
    Try {
      val chunks = mutable.ArrayBuffer.empty[Array[Byte]]
      var totalSize = 0L
      var done = false

      while (!done) {
        // Read chunk size line (hex number followed by \r\n)
        val sizeLine = readLine(input, 100) match {
          case Success(line) => line
          case Failure(e)    => throw e
        }

        // Parse chunk size (hex)
        val chunkSize = try {
          // Handle chunk extensions (e.g., "1a; name=value")
          val sizeStr = sizeLine.split(";")(0).trim
          Integer.parseInt(sizeStr, 16)
        } catch {
          case e: NumberFormatException =>
            throw new IllegalArgumentException(
              s"Invalid chunk size: $sizeLine",
              e
            )
        }

        if (chunkSize == 0) {
          // Last chunk, read trailing headers (if any) and final \r\n
          var trailingDone = false
          while (!trailingDone) {
            readLine(input, 8192) match {
              case Success(line) if line.isEmpty => trailingDone = true
              case Success(_) => // Ignore trailing headers
              case Failure(e) => throw e
            }
          }
          done = true
        } else {
          // Check total size limit
          totalSize += chunkSize
          config.maxRequestBodySize match {
            case Some(maxSize) if totalSize > maxSize =>
              throw new RequestBodyTooLargeException(totalSize, maxSize)
            case _ => // OK
          }

          // Read chunk data
          val chunk = new Array[Byte](chunkSize)
          var offset = 0
          var remaining = chunkSize

          while (remaining > 0) {
            val bytesRead = input.read(chunk, offset, remaining)
            if (bytesRead == -1) {
              throw new IllegalStateException(
                "Unexpected end of stream while reading chunk"
              )
            }
            offset += bytesRead
            remaining -= bytesRead
          }

          chunks += chunk

          // Read trailing \r\n after chunk data
          val crlf = new Array[Byte](2)
          if (input.read(crlf) != 2 || crlf(0) != '\r' || crlf(1) != '\n') {
            throw new IllegalArgumentException(
              "Expected CRLF after chunk data"
            )
          }
        }
      }

      // Combine all chunks
      chunks.flatten.toArray
    }
  }

  /** Read a single line from the input stream, terminated by \r\n.
    *
    * This handles HTTP line endings properly (CRLF).
    *
    * @param maxLength
    *   Maximum line length in bytes. Throws RequestLineTooLongException if
    *   exceeded.
    * @return
    *   The line without the \r\n terminator
    */
  private def readLine(
      input: InputStream,
      maxLength: Int = Int.MaxValue
  ): Try[String] = {
    Try {
      val line = new StringBuilder
      var current: Int = input.read()
      var done = false

      // If we hit EOF immediately (no bytes read), connection was closed
      if (current == -1) {
        throw new ConnectionClosedException
      }

      while (current != -1 && !done) {
        val char = current.toChar

        if (char == '\r') {
          // Check if next character is \n
          input.mark(1)
          val next = input.read()
          if (next == '\n') {
            // Found CRLF, we're done
            done = true
          } else {
            // Not CRLF, reset and append \r
            input.reset()
            line.append(char)
            current = input.read()
          }
        } else {
          line.append(char)
          current = input.read()
        }

        // Check line length limit
        if (line.length > maxLength) {
          throw new RequestLineTooLongException(line.length, maxLength)
        }
      }

      // Check if we have a line
      if (!done && line.isEmpty) {
        throw new IllegalStateException("Unexpected end of stream")
      }

      line.toString
    }
  }

  /** Convert a ParseResult to a Request[Array[Byte]].
    *
    * @param result
    *   The parse result
    * @return
    *   A Request object
    */
  def toRequest(result: ParseResult): Request[Array[Byte]] = {
    Request(
      uri = result.uri,
      headers = result.headers,
      body = result.body
    )
  }
}
