package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.common.HttpMethod

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class HttpParserSpec extends munit.FunSuite {

  /** Helper to create an InputStream from a string. */
  def makeInputStream(content: String): ByteArrayInputStream =
    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))

  test("parse simple GET request") {
    val request =
      "GET /hello HTTP/1.1\r\n" +
        "Host: localhost:9000\r\n" +
        "User-Agent: curl/7.79.1\r\n" +
        "Accept: */*\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assertEquals(parsed.method, HttpMethod.GET)
    assertEquals(parsed.uri.getPath, "/hello")
    assertEquals(parsed.httpVersion, "HTTP/1.1")
    assertEquals(parsed.headers("Host"), List("localhost:9000"))
    assertEquals(parsed.headers("User-Agent"), List("curl/7.79.1"))
    assertEquals(parsed.headers("Accept"), List("*/*"))
    assert(parsed.body.isEmpty)
  }

  test("parse GET request with query parameters") {
    val request =
      "GET /search?q=scala&limit=10 HTTP/1.1\r\n" +
        "Host: example.com\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assertEquals(parsed.method, HttpMethod.GET)
    assertEquals(parsed.uri.getPath, "/search")
    assertEquals(parsed.uri.getQuery, "q=scala&limit=10")
  }

  test("parse POST request with body") {
    val body    = "name=John&age=30"
    val request =
      s"POST /submit HTTP/1.1\r\n" +
        s"Host: localhost:9000\r\n" +
        s"Content-Type: application/x-www-form-urlencoded\r\n" +
        s"Content-Length: ${body.length}\r\n" +
        s"\r\n" +
        s"$body"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assertEquals(parsed.method, HttpMethod.POST)
    assertEquals(parsed.uri.getPath, "/submit")
    assertEquals(
      parsed.headers("Content-Type"),
      List("application/x-www-form-urlencoded")
    )
    assertEquals(parsed.headers("Content-Length"), List(body.length.toString))
    assertEquals(new String(parsed.body, StandardCharsets.UTF_8), body)
  }

  test("parse request with multiple values for same header") {
    val request =
      "GET /test HTTP/1.1\r\n" +
        "Host: localhost\r\n" +
        "Accept: text/html\r\n" +
        "Accept: application/json\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assertEquals(
      parsed.headers("Accept"),
      List("text/html", "application/json")
    )
  }

  test("parse request with no body (Content-Length: 0)") {
    val request =
      "POST /test HTTP/1.1\r\n" +
        "Host: localhost\r\n" +
        "Content-Length: 0\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assert(parsed.body.isEmpty)
  }

  test("convert ParseResult to Request model") {
    val request =
      "GET /hello HTTP/1.1\r\n" +
        "Host: localhost:9000\r\n" +
        "\r\n"

    val parseResult = HttpParser.parse(makeInputStream(request)).get
    val req         = HttpParser.toRequest(parseResult)

    assertEquals(req.uri.getPath, "/hello")
    assertEquals(req.headers("Host"), List("localhost:9000"))
    assert(req.body.isEmpty)
  }

  test("handle empty headers correctly") {
    val request =
      "GET / HTTP/1.1\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isSuccess)
    val parsed = result.get

    assert(parsed.headers.isEmpty)
  }

  test("fail on invalid request line") {
    val request = "INVALID REQUEST\r\n\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isFailure)
  }

  test("fail on invalid HTTP method") {
    val request =
      "INVALID /path HTTP/1.1\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isFailure)
  }

  test("fail on malformed header") {
    val request =
      "GET /test HTTP/1.1\r\n" +
        "InvalidHeaderWithoutColon\r\n" +
        "\r\n"

    val result = HttpParser.parse(makeInputStream(request))

    assert(result.isFailure)
  }
}
