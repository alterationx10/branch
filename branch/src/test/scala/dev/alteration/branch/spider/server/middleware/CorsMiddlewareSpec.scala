package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.{Request, Response}
import dev.alteration.branch.spider.server.middleware.CorsConfig.*

import java.net.URI

class CorsMiddlewareSpec extends munit.FunSuite {

  test("CorsMiddleware allows requests from allowed origins") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue)
  }

  test("CorsMiddleware continues for non-CORS requests (no Origin header)") {
    val middleware = CorsMiddleware.permissive[String, String]

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map.empty,
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isContinue)
  }

  test("CorsMiddleware adds Access-Control-Allow-Origin header") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("https://example.com")))
  }

  test(
    "CorsMiddleware adds wildcard for permissive config without credentials"
  ) {
    val middleware = CorsMiddleware.permissive[String, String]

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("*")))
  }

  test("CorsMiddleware does not add wildcard when credentials enabled") {
    val config     =
      CorsConfig.permissive.withCredentials.withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("https://example.com")))

    val allowCreds = result.headers.get("Access-Control-Allow-Credentials")
    assertEquals(allowCreds, Some(List("true")))
  }

  test("CorsMiddleware adds Vary header for specific origins") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val vary = result.headers.get("Vary")
    assertEquals(vary, Some(List("Origin")))
  }

  test("CorsMiddleware adds exposed headers") {
    val config     =
      CorsConfig().withOrigins("*").withExposedHeaders("X-Custom-Header")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://example.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val exposed = result.headers.get("Access-Control-Expose-Headers")
    assertEquals(exposed, Some(List("X-Custom-Header")))
  }

  test("CorsMiddleware handles preflight OPTIONS request") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map(
        "Origin"                         -> List("https://example.com"),
        "Access-Control-Request-Method"  -> List("POST"),
        "Access-Control-Request-Headers" -> List("Content-Type")
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond)

    val response = result.getResponse.get
    assertEquals(response.statusCode, 204)

    val allowOrigin = response.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("https://example.com")))

    val allowMethods = response.headers.get("Access-Control-Allow-Methods")
    assert(allowMethods.isDefined)
    assert(allowMethods.get.head.contains("POST"))

    val allowHeaders = response.headers.get("Access-Control-Allow-Headers")
    assert(allowHeaders.isDefined)
  }

  test("CorsMiddleware rejects preflight for disallowed method") {
    val config = CorsConfig()
      .withOrigins("https://example.com")
      .withMethods(HttpMethod.GET) // Only allow GET
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map(
        "Origin"                        -> List("https://example.com"),
        "Access-Control-Request-Method" -> List("DELETE") // Request DELETE
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond)

    val response = result.getResponse.get
    assertEquals(response.statusCode, 403)
  }

  test("CorsMiddleware rejects preflight for disallowed headers") {
    val config = CorsConfig()
      .withOrigins("https://example.com")
      .withHeaders("Content-Type") // Only allow Content-Type
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map(
        "Origin"                         -> List("https://example.com"),
        "Access-Control-Request-Method"  -> List("POST"),
        "Access-Control-Request-Headers" -> List(
          "X-Custom-Header"
        ) // Disallowed
      ),
      body = ""
    )

    val result = middleware.preProcess(request)
    assert(result.isRespond)

    val response = result.getResponse.get
    assertEquals(response.statusCode, 403)
  }

  test("CorsMiddleware adds max age to preflight response") {
    val config     =
      CorsConfig().withOrigins("https://example.com").withMaxAge(7200)
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map(
        "Origin"                        -> List("https://example.com"),
        "Access-Control-Request-Method" -> List("POST")
      ),
      body = ""
    )

    val result   = middleware.preProcess(request)
    val response = result.getResponse.get

    val maxAge = response.headers.get("Access-Control-Max-Age")
    assertEquals(maxAge, Some(List("7200")))
  }

  test("CorsMiddleware does not add CORS headers for non-allowed origin") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://evil.com")), // Not allowed
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, None)
  }

  test("CorsMiddleware handles case-insensitive headers") {
    val config     = CorsConfig().withOrigins("https://example.com")
    val middleware = CorsMiddleware[String, String](config)

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("origin" -> List("https://example.com")), // lowercase
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("https://example.com")))
  }

  test("CorsMiddleware.permissive factory method") {
    val middleware = CorsMiddleware.permissive[String, String]

    val request = Request(
      uri = URI.create("http://localhost/api"),
      headers = Map("Origin" -> List("https://anything.com")),
      body = ""
    )

    val response = Response(200, "OK")
    val result   = middleware.postProcess(request, response)

    val allowOrigin = result.headers.get("Access-Control-Allow-Origin")
    assertEquals(allowOrigin, Some(List("*")))
  }

}
