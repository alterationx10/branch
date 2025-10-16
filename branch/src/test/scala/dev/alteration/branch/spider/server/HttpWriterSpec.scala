package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.server.RequestHandler.given

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class HttpWriterSpec extends munit.FunSuite {

  /** Helper to capture output from HttpWriter. */
  def captureOutput[A](response: Response[A])(using
      encoder: Conversion[A, Array[Byte]]
  ): String = {
    val output = new ByteArrayOutputStream()
    HttpWriter.write(response, output)
    new String(output.toByteArray, StandardCharsets.UTF_8)
  }

  test("write simple 200 OK response") {
    val response = Response(
      statusCode = 200,
      body = "Hello, World!",
      headers = Map(ContentType.txt.toHeader)
    )

    val output = captureOutput(response)

    assert(output.startsWith("HTTP/1.1 200 OK\r\n"))
    assert(output.contains("Content-Type: text/plain\r\n"))
    assert(output.contains("\r\n\r\n"))
    assert(output.endsWith("Hello, World!"))
  }

  test("write 404 Not Found response") {
    val response = Response(
      statusCode = 404,
      body = "Not found",
      headers = Map(ContentType.html.toHeader)
    )

    val output = captureOutput(response)

    assert(output.startsWith("HTTP/1.1 404 Not Found\r\n"))
    assert(output.contains("Content-Type: text/html\r\n"))
    assert(output.endsWith("Not found"))
  }

  test("write response with multiple headers") {
    val response = Response(
      statusCode = 200,
      body = "Test",
      headers = Map(
        "Content-Type" -> List("text/plain"),
        "Cache-Control" -> List("no-cache"),
        "X-Custom" -> List("value1", "value2")
      )
    )

    val output = captureOutput(response)

    assert(output.contains("Content-Type: text/plain\r\n"))
    assert(output.contains("Cache-Control: no-cache\r\n"))
    assert(output.contains("X-Custom: value1\r\n"))
    assert(output.contains("X-Custom: value2\r\n"))
  }

  test("write response with empty body") {
    val response = Response(
      statusCode = 204,
      body = "",
      headers = Map.empty
    )

    val output = captureOutput(response)

    assert(output.startsWith("HTTP/1.1 204 No Content\r\n"))
    assert(output.endsWith("\r\n\r\n"))
  }

  test("write JSON response") {
    val jsonBody = """{"message":"Hello"}"""
    val response = Response(
      statusCode = 200,
      body = jsonBody,
      headers = Map(ContentType.json.toHeader)
    )

    val output = captureOutput(response)

    assert(output.contains("HTTP/1.1 200 OK\r\n"))
    assert(output.contains("Content-Type: application/json\r\n"))
    assert(output.endsWith(jsonBody))
  }

  test("write response with byte array body") {
    val bodyBytes = "Binary data".getBytes(StandardCharsets.UTF_8)
    val response = Response(
      statusCode = 200,
      body = bodyBytes,
      headers = Map(ContentType.bin.toHeader)
    )

    val output = captureOutput(response)

    assert(output.contains("HTTP/1.1 200 OK\r\n"))
    assert(output.contains("Content-Type: application/octet-stream\r\n"))
    assert(output.endsWith("Binary data"))
  }

  test("write 500 Internal Server Error response") {
    val response = Response(
      statusCode = 500,
      body = "Server error",
      headers = Map(ContentType.txt.toHeader)
    )

    val output = captureOutput(response)

    assert(output.startsWith("HTTP/1.1 500 Internal Server Error\r\n"))
  }

  test("write response with unknown status code") {
    val response = Response(
      statusCode = 999,
      body = "Custom",
      headers = Map.empty
    )

    val output = captureOutput(response)

    assert(output.startsWith("HTTP/1.1 999 Unknown\r\n"))
  }

  test("proper CRLF line endings throughout") {
    val response = Response(
      statusCode = 200,
      body = "Test",
      headers = Map("X-Test" -> List("value"))
    )

    val output = captureOutput(response)
    val lines = output.split("\r\n", -1)

    // Should have: status line, header line, blank line, body
    assert(lines.length >= 4)
    assertEquals(lines(0), "HTTP/1.1 200 OK")
    assert(lines(1).startsWith("X-Test: value"))
    assertEquals(lines(2), "")
    assertEquals(lines(3), "Test")
  }

  test("handle response with no headers") {
    val response = Response(
      statusCode = 200,
      body = "No headers",
      headers = Map.empty
    )

    val output = captureOutput(response)

    // Should have status line, blank line, body
    assert(output.contains("HTTP/1.1 200 OK\r\n\r\n"))
    assert(output.endsWith("No headers"))
  }
}
