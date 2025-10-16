package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.HttpMethod

import java.io.{BufferedInputStream, InputStream}
import java.net.URI
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Try, Success, Failure}

/** Parses HTTP/1.1 requests from an InputStream into Request objects.
  */
object HttpParser {

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
    * @return
    *   Try containing either a ParseResult or an error
    */
  def parse(input: InputStream): Try[ParseResult] = {
    val buffered = new BufferedInputStream(input)

    for {
      requestLine <- parseRequestLine(buffered)
      (method, uri, version) = requestLine
      headers <- parseHeaders(buffered)
      body <- readBody(buffered, headers)
    } yield ParseResult(method, uri, version, headers, body)
  }

  /** Parse the HTTP request line: METHOD /path HTTP/1.1
    *
    * @return
    *   Try containing (HttpMethod, URI, httpVersion)
    */
  private def parseRequestLine(
      input: InputStream
  ): Try[(HttpMethod, URI, String)] = {
    readLine(input).flatMap { line =>
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
      input: InputStream
  ): Try[Map[String, List[String]]] = {
    @tailrec
    def readHeaderLines(
        acc: mutable.Map[String, mutable.ListBuffer[String]]
    ): Try[Map[String, List[String]]] = {
      readLine(input) match {
        case Success(line) if line.isEmpty =>
          // Empty line indicates end of headers
          Success(acc.view.mapValues(_.toList).toMap)

        case Success(line) =>
          // Parse "HeaderName: value"
          line.indexOf(':') match {
            case -1 =>
              Failure(new IllegalArgumentException(s"Invalid header: $line"))
            case colonIdx =>
              val name = line.substring(0, colonIdx).trim
              val value = line.substring(colonIdx + 1).trim

              // Accumulate values for this header
              if (!acc.contains(name)) {
                acc(name) = mutable.ListBuffer.empty[String]
              }
              acc(name) += value

              readHeaderLines(acc)
          }

        case Failure(e) =>
          Failure(e)
      }
    }

    readHeaderLines(mutable.Map.empty)
  }

  /** Read the request body based on the Content-Length header.
    *
    * If Content-Length is not present or is 0, returns an empty byte array.
    *
    * @param headers
    *   The parsed headers map
    * @return
    *   The request body as a byte array
    */
  private def readBody(
      input: InputStream,
      headers: Map[String, List[String]]
  ): Try[Array[Byte]] = {
    // Look for Content-Length header (case-insensitive)
    val contentLengthOpt = headers
      .find { case (k, _) => k.equalsIgnoreCase("Content-Length") }
      .flatMap(_._2.headOption)
      .flatMap(s => Try(s.toInt).toOption)

    contentLengthOpt match {
      case Some(length) if length > 0 =>
        Try {
          val buffer = new Array[Byte](length)
          var offset = 0
          var remaining = length

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

      case _ =>
        // No body or Content-Length: 0
        Success(Array.empty[Byte])
    }
  }

  /** Read a single line from the input stream, terminated by \r\n.
    *
    * This handles HTTP line endings properly (CRLF).
    *
    * @return
    *   The line without the \r\n terminator
    */
  private def readLine(input: InputStream): Try[String] = {
    Try {
      val line = new StringBuilder
      var current: Int = input.read()
      var done = false

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
