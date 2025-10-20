package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, Response}
import dev.alteration.branch.spider.server.middleware.CsrfToken.*

import java.net.URI

class CsrfTokenSpec extends munit.FunSuite {

  test("CsrfToken.generate creates non-empty token") {
    val token = CsrfToken.generate()
    assert(token.nonEmpty)
  }

  test("CsrfToken.generate creates tokens of specified length") {
    val token16 = CsrfToken.generate(16)
    val token32 = CsrfToken.generate(32)
    val token64 = CsrfToken.generate(64)

    // Base64 encoding increases size, so we check that they're different lengths
    assert(token16.length < token32.length)
    assert(token32.length < token64.length)
  }

  test("CsrfToken.generate creates unique tokens") {
    val token1 = CsrfToken.generate()
    val token2 = CsrfToken.generate()
    assert(token1 != token2)
  }

  test("CsrfToken.validate accepts matching tokens") {
    val token = "test-token-12345"
    assert(CsrfToken.validate(token, token))
  }

  test("CsrfToken.validate rejects different tokens") {
    val token1 = "test-token-12345"
    val token2 = "test-token-67890"
    assert(!CsrfToken.validate(token1, token2))
  }

  test("CsrfToken.validate rejects empty tokens") {
    assert(!CsrfToken.validate("", ""))
    assert(!CsrfToken.validate("token", ""))
    assert(!CsrfToken.validate("", "token"))
  }

  test("CsrfToken.fromCookie extracts token from request") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List("XSRF-TOKEN=test-token-123")),
      body = ""
    )

    val token = CsrfToken.fromCookie(request, "XSRF-TOKEN")
    assertEquals(token, Some("test-token-123"))
  }

  test("CsrfToken.fromCookie returns None when cookie not found") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    val token = CsrfToken.fromCookie(request, "XSRF-TOKEN")
    assertEquals(token, None)
  }

  test("CsrfToken.fromHeader extracts token from request") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("X-XSRF-TOKEN" -> List("test-token-123")),
      body = ""
    )

    val token = CsrfToken.fromHeader(request, "X-XSRF-TOKEN")
    assertEquals(token, Some("test-token-123"))
  }

  test("CsrfToken.fromHeader returns None when header not found") {
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    val token = CsrfToken.fromHeader(request, "X-XSRF-TOKEN")
    assertEquals(token, None)
  }

  test("Response.withCsrfToken adds CSRF cookie") {
    val config   = CsrfConfig.default
    val response = Response(200, "OK")
    val token    = "test-token-123"
    val result   = response.withCsrfToken(token, config)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
    assert(setCookieHeaders.get.exists(_.contains("XSRF-TOKEN=test-token-123")))
  }

  test("Response.withNewCsrfToken generates and adds token") {
    val config                   = CsrfConfig.default
    val response                 = Response(200, "OK")
    val (result, generatedToken) = response.withNewCsrfToken(config)

    assert(generatedToken.nonEmpty)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
    assert(
      setCookieHeaders.get.exists(_.contains(s"XSRF-TOKEN=$generatedToken"))
    )
  }

  test("Request.csrfTokenFromCookie extracts token") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List("XSRF-TOKEN=test-token-123")),
      body = ""
    )

    val token = request.csrfTokenFromCookie(config)
    assertEquals(token, Some("test-token-123"))
  }

  test("Request.csrfTokenFromHeader extracts token") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("X-XSRF-TOKEN" -> List("test-token-123")),
      body = ""
    )

    val token = request.csrfTokenFromHeader(config)
    assertEquals(token, Some("test-token-123"))
  }

  test("Request.validateCsrfToken validates matching tokens") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map(
        "Cookie"       -> List("XSRF-TOKEN=test-token-123"),
        "X-XSRF-TOKEN" -> List("test-token-123")
      ),
      body = ""
    )

    assert(request.validateCsrfToken(config))
  }

  test("Request.validateCsrfToken rejects mismatched tokens") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map(
        "Cookie"       -> List("XSRF-TOKEN=test-token-123"),
        "X-XSRF-TOKEN" -> List("different-token")
      ),
      body = ""
    )

    assert(!request.validateCsrfToken(config))
  }

  test("Request.validateCsrfToken rejects when cookie missing") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("X-XSRF-TOKEN" -> List("test-token-123")),
      body = ""
    )

    assert(!request.validateCsrfToken(config))
  }

  test("Request.validateCsrfToken rejects when header missing") {
    val config  = CsrfConfig.default
    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List("XSRF-TOKEN=test-token-123")),
      body = ""
    )

    assert(!request.validateCsrfToken(config))
  }

  test("CsrfToken.hiddenField generates HTML input") {
    val html = CsrfToken.hiddenField("test-token-123")
    assert(html.contains("type=\"hidden\""))
    assert(html.contains("name=\"csrf_token\""))
    assert(html.contains("value=\"test-token-123\""))
  }

  test("CsrfToken.hiddenField accepts custom field name") {
    val html = CsrfToken.hiddenField("test-token-123", "custom_field")
    assert(html.contains("name=\"custom_field\""))
  }

  test("CsrfToken.metaTag generates HTML meta tag") {
    val html = CsrfToken.metaTag("test-token-123")
    assert(html.contains("<meta"))
    assert(html.contains("name=\"csrf-token\""))
    assert(html.contains("content=\"test-token-123\""))
  }

  test("CSRF cookie has correct attributes") {
    val config   = CsrfConfig.default
    val response = Response(200, "OK")
    val result   = response.withCsrfToken("test-token", config)

    val setCookieHeaders = result.headers.get("Set-Cookie").get
    val cookieHeader     = setCookieHeaders.head

    assert(cookieHeader.contains("XSRF-TOKEN=test-token"))
    assert(cookieHeader.contains("Path=/"))
    assert(cookieHeader.contains("SameSite=Strict"))
  }

}
