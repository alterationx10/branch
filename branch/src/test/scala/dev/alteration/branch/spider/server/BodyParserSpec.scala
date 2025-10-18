package dev.alteration.branch.spider.server

import dev.alteration.branch.friday.{JsonCodec, JsonDecoder, JsonEncoder}
import dev.alteration.branch.spider.server.BodyParser.*

import java.net.URI
import java.nio.charset.StandardCharsets

class BodyParserSpec extends munit.FunSuite {

  // Simple test case class
  case class User(name: String, age: Int) derives JsonCodec

  test("parseFormUrlEncoded parses simple form") {
    val body   = "name=John&age=30"
    val result = BodyParser.parseFormUrlEncoded(body)
    assertEquals(result, Map("name" -> "John", "age" -> "30"))
  }

  test("parseFormUrlEncoded handles URL encoding") {
    val body   = "name=John+Doe&email=john%40example.com"
    val result = BodyParser.parseFormUrlEncoded(body)
    assertEquals(
      result,
      Map("name" -> "John Doe", "email" -> "john@example.com")
    )
  }

  test("parseFormUrlEncoded handles empty values") {
    val body   = "name=&age=30"
    val result = BodyParser.parseFormUrlEncoded(body)
    assertEquals(result, Map("name" -> "", "age" -> "30"))
  }

  test("parseFormUrlEncoded handles empty body") {
    val result = BodyParser.parseFormUrlEncoded("")
    assertEquals(result, Map.empty[String, String])
  }

  test("parseFormUrlEncoded handles special characters") {
    val body   = "message=Hello%20World%21&symbol=%26"
    val result = BodyParser.parseFormUrlEncoded(body)
    assertEquals(result, Map("message" -> "Hello World!", "symbol" -> "&"))
  }

  test("parseFormUrlEncoded from bytes") {
    val body   = "name=John&age=30"
    val bytes  = body.getBytes(StandardCharsets.UTF_8)
    val result = BodyParser.parseFormUrlEncoded(bytes)
    assertEquals(result, Map("name" -> "John", "age" -> "30"))
  }

  test("parseJson parses valid JSON") {
    val json   = """{"name":"Alice","age":25}"""
    val result = BodyParser.parseJson[User](json)
    assert(result.isSuccess)
    assertEquals(result.get, User("Alice", 25))
  }

  test("parseJson handles invalid JSON") {
    val json   = """{"name":"Alice","age":}"""
    val result = BodyParser.parseJson[User](json)
    assert(result.isFailure)
  }

  test("parseJson from bytes") {
    val json   = """{"name":"Bob","age":35}"""
    val bytes  = json.getBytes(StandardCharsets.UTF_8)
    val result = BodyParser.parseJson[User](bytes)
    assert(result.isSuccess)
    assertEquals(result.get, User("Bob", 35))
  }

  test("Request.contentType extracts Content-Type") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Content-Type" -> List("application/json; charset=utf-8")),
      body = ""
    )
    assertEquals(request.contentType, Some("application/json"))
  }

  test("Request.contentType handles lowercase header") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers =
        Map("content-type" -> List("application/x-www-form-urlencoded")),
      body = ""
    )
    assertEquals(request.contentType, Some("application/x-www-form-urlencoded"))
  }

  test("Request.contentType returns None when not present") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )
    assertEquals(request.contentType, None)
  }

  test("Request.bodySizeBytes calculates size for Array[Byte]") {
    val body    = "Hello World".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = body
    )
    assertEquals(request.bodySizeBytes, 11L)
  }

  test("Request.bodySizeBytes calculates size for String") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = "Hello World"
    )
    assertEquals(request.bodySizeBytes, 11L)
  }

  test("Request.isBodyWithinLimit checks size") {
    val body    = "Test".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = body
    )
    assert(request.isBodyWithinLimit(10))
    assert(!request.isBodyWithinLimit(3))
  }

  test("Request[Array[Byte]].parseFormBody parses form data") {
    val body    = "name=Charlie&age=40".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = body
    )
    val result  = request.parseFormBody
    assertEquals(result, Map("name" -> "Charlie", "age" -> "40"))
  }

  test("Request[Array[Byte]].parseJsonBody parses JSON") {
    val json    = """{"name":"Diana","age":28}"""
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = bytes
    )
    val result  = request.parseJsonBody[User]
    assert(result.isSuccess)
    assertEquals(result.get, User("Diana", 28))
  }

  test("Request[String].parseFormBody parses form data") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = "name=Eve&age=32"
    )
    val result  = request.parseFormBody
    assertEquals(result, Map("name" -> "Eve", "age" -> "32"))
  }

  test("Request[String].parseJsonBody parses JSON") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = """{"name":"Frank","age":45}"""
    )
    val result  = request.parseJsonBody[User]
    assert(result.isSuccess)
    assertEquals(result.get, User("Frank", 45))
  }

  test("Request.parseFormBodySafe succeeds within size limit") {
    val body    = "name=Grace&age=27".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = body
    )
    val result  = request.parseFormBodySafe(ParserConfig.default)
    result match {
      case ParseSuccess(form) =>
        assertEquals(form, Map("name" -> "Grace", "age" -> "27"))
      case _                  => fail("Expected ParseSuccess")
    }
  }

  test("Request.parseFormBodySafe fails when body too large") {
    val body    = "x" * 1000000                    // 1MB
    val bytes   = body.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = bytes
    )
    val config  = ParserConfig(maxFormSize = 1000) // 1KB limit
    val result  = request.parseFormBodySafe(config)
    assertEquals(result, BodyTooLarge)
  }

  test("Request.parseJsonBodySafe succeeds within size limit") {
    val json    = """{"name":"Henry","age":38}"""
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = bytes
    )
    val result  = request.parseJsonBodySafe[User](ParserConfig.default)
    result match {
      case ParseSuccess(user) => assertEquals(user, User("Henry", 38))
      case _                  => fail("Expected ParseSuccess")
    }
  }

  test("Request.parseJsonBodySafe fails when body too large") {
    val largeData = "x" * 1000000  // 1MB of data
    val json    = s"""{"data":"$largeData"}""" // Large JSON
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = bytes
    )
    val config  = ParserConfig(maxJsonSize = 1000)  // 1KB limit
    val result  = request.parseJsonBodySafe[User](config)
    assertEquals(result, BodyTooLarge)
  }

  test("Request.parseJsonBodySafe returns ParseFailure for invalid JSON") {
    val json    = """{"name":"Ivy","age":"""
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = bytes
    )
    val result  = request.parseJsonBodySafe[User](ParserConfig.default)
    result match {
      case ParseFailure(_) => // Expected
      case _               => fail("Expected ParseFailure")
    }
  }

  test("Request.parseBodyAuto parses JSON based on Content-Type") {
    val json    = """{"name":"Jack","age":42}"""
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Content-Type" -> List("application/json")),
      body = bytes
    )
    val result  = request.parseBodyAuto[User]()
    result match {
      case ParseSuccess(Right(user)) => assertEquals(user, User("Jack", 42))
      case _                         => fail("Expected ParseSuccess with Right")
    }
  }

  test("Request.parseBodyAuto parses form based on Content-Type") {
    val body    = "name=Kate&age=29".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers =
        Map("Content-Type" -> List("application/x-www-form-urlencoded")),
      body = body
    )
    val result  = request.parseBodyAuto[User]()
    result match {
      case ParseSuccess(Left(form)) =>
        assertEquals(form, Map("name" -> "Kate", "age" -> "29"))
      case _                        => fail("Expected ParseSuccess with Left")
    }
  }

  test("Request.parseBodyAuto returns UnsupportedContentType") {
    val bytes   = "some data".getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Content-Type" -> List("text/plain")),
      body = bytes
    )
    val result  = request.parseBodyAuto[User]()
    assertEquals(result, UnsupportedContentType)
  }

  test("Request.parseBodyAuto returns BodyTooLarge for JSON") {
    val largeData = "x" * 1000000  // 1MB of data
    val json    = s"""{"data":"$largeData"}"""
    val bytes   = json.getBytes(StandardCharsets.UTF_8)
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Content-Type" -> List("application/json")),
      body = bytes
    )
    val config  = ParserConfig(maxJsonSize = 1000)
    val result  = request.parseBodyAuto[User](config)
    assertEquals(result, BodyTooLarge)
  }

  test("ParserConfig presets") {
    val default    = ParserConfig.default
    val strict     = ParserConfig.strict
    val permissive = ParserConfig.permissive

    assert(strict.maxJsonSize < default.maxJsonSize)
    assert(default.maxJsonSize < permissive.maxJsonSize)
  }

  test("badRequestResponse creates 400 response") {
    val response = BodyParser.badRequestResponse("Invalid data")
    assertEquals(response.statusCode, 400)
    assertEquals(response.body, "Invalid data")
  }

  test("payloadTooLargeResponse creates 413 response") {
    val response = BodyParser.payloadTooLargeResponse
    assertEquals(response.statusCode, 413)
  }

  test("unsupportedMediaTypeResponse creates 415 response") {
    val response = BodyParser.unsupportedMediaTypeResponse
    assertEquals(response.statusCode, 415)
  }

}
