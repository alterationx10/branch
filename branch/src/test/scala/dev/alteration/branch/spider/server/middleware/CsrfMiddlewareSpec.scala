package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.{Request, Response}

import java.net.URI

class CsrfMiddlewareSpec extends munit.FunSuite {

  test("CsrfMiddleware generates token for new requests") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
    assert(setCookieHeaders.get.exists(_.contains("XSRF-TOKEN=")))
  }

  test("CsrfMiddleware preserves existing token") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddleware[String, String](config)

    val existingToken = "existing-token-123"
    val request       = Request(
      uri = URI.create("http://localhost/"),
      headers = Map("Cookie" -> List(s"XSRF-TOKEN=$existingToken")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
    assert(
      setCookieHeaders.get.exists(_.contains(s"XSRF-TOKEN=$existingToken"))
    )
  }

  test("CsrfMiddleware validates matching tokens") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddleware[String, String](config)

    val token   = "valid-token-123"
    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map(
        "Cookie"       -> List(s"XSRF-TOKEN=$token"),
        "X-XSRF-TOKEN" -> List(token)
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue)
  }

  test("CsrfMiddleware rejects mismatched tokens") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map(
        "Cookie"       -> List("XSRF-TOKEN=token1"),
        "X-XSRF-TOKEN" -> List("token2")
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond)

    val response = result.getResponse.get
    assertEquals(response.statusCode, 403)
  }

  test("CsrfMiddleware rejects missing token") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map.empty,
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond)

    val response = result.getResponse.get
    assertEquals(response.statusCode, 403)
  }

  test("CsrfMiddlewareWithMethod exempts safe methods") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.GET) // Always return GET
    )

    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map.empty, // No CSRF token
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue) // Should continue without validation
  }

  test("CsrfMiddlewareWithMethod validates non-safe methods") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.POST) // Always return POST
    )

    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map.empty, // No CSRF token
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond) // Should reject

    val response = result.getResponse.get
    assertEquals(response.statusCode, 403)
  }

  test("CsrfMiddlewareWithMethod exempts configured paths") {
    val config     = CsrfConfig.development.withExemptPaths("/api/webhook/*")
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.POST) // POST (normally requires CSRF)
    )

    val request = Request(
      uri = URI.create("http://localhost/api/webhook/github"),
      headers = Map.empty, // No CSRF token
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue) // Should continue without validation
  }

  test("CsrfMiddlewareWithMethod validates non-exempt paths") {
    val config     = CsrfConfig.development.withExemptPaths("/api/public")
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.POST)
    )

    val request = Request(
      uri = URI.create("http://localhost/api/private"), // Not exempt
      headers = Map.empty,
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond) // Should reject
  }

  test("CsrfMiddleware.default factory method") {
    val middleware = CsrfMiddleware.default[String, String]

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)
  }

  test("CsrfMiddleware.strict factory method") {
    val middleware = CsrfMiddleware.strict[String, String]

    val request = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val setCookieHeaders = result.headers.get("Set-Cookie")
    assert(setCookieHeaders.isDefined)

    // Strict config should have Strict SameSite
    val cookieHeader = setCookieHeaders.get.head
    assert(cookieHeader.contains("SameSite=Strict"))
  }

  test("CsrfMiddleware allows valid token with correct header") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.POST)
    )

    val token   = "valid-token-abc123"
    val request = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map(
        "Cookie"       -> List(s"XSRF-TOKEN=$token"),
        "X-XSRF-TOKEN" -> List(token)
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue)
  }

  test("End-to-end: generate token, validate request") {
    val config     = CsrfConfig.development
    val middleware = CsrfMiddlewareWithMethod[String, String](
      config,
      _ => Some(HttpMethod.POST)
    )

    // Step 1: Generate token for initial response
    val initialRequest    = Request(
      uri = URI.create("http://localhost/"),
      headers = Map.empty,
      body = ""
    )
    val initialResponse   = Response(200, "OK")
    val responseWithToken =
      middleware.postProcess(initialRequest, initialResponse)

    // Extract the generated token from the Set-Cookie header
    val setCookieHeader = responseWithToken.headers.get("Set-Cookie").get.head
    val tokenMatch      =
      """XSRF-TOKEN=([^;]+)""".r.findFirstMatchIn(setCookieHeader)
    val generatedToken  = tokenMatch.get.group(1)

    // Step 2: Make a subsequent request with the token
    val subsequentRequest = Request(
      uri = URI.create("http://localhost/api/data"),
      headers = Map(
        "Cookie"       -> List(s"XSRF-TOKEN=$generatedToken"),
        "X-XSRF-TOKEN" -> List(generatedToken)
      ),
      body = ""
    )

    val validationResult = middleware.preProcess(subsequentRequest)
    assert(validationResult.isContinue) // Should be valid
  }

}
