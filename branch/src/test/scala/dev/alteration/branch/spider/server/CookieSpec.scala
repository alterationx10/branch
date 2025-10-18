package dev.alteration.branch.spider.server

import java.net.URI

class CookieSpec extends munit.FunSuite {

  test("Cookie model with basic name and value") {
    val cookie = Cookie("session", "abc123")
    assertEquals(cookie.name, "session")
    assertEquals(cookie.value, "abc123")
    assertEquals(cookie.path, Some("/"))
  }

  test("Cookie.toSetCookieHeader with all attributes") {
    val cookie = Cookie(
      name = "session",
      value = "abc123",
      domain = Some("example.com"),
      path = Some("/admin"),
      maxAge = Some(3600L),
      secure = true,
      httpOnly = true,
      sameSite = Some(Cookie.SameSite.Strict)
    )

    val header = cookie.toSetCookieHeader
    assert(header.contains("session=abc123"))
    assert(header.contains("Domain=example.com"))
    assert(header.contains("Path=/admin"))
    assert(header.contains("Max-Age=3600"))
    assert(header.contains("Secure"))
    assert(header.contains("HttpOnly"))
    assert(header.contains("SameSite=Strict"))
  }

  test("Cookie builder methods") {
    val cookie = Cookie("test", "value")
      .withDomain("example.com")
      .withPath("/api")
      .withMaxAge(7200)
      .withSecure
      .withHttpOnly
      .withSameSite(Cookie.SameSite.Lax)

    assertEquals(cookie.domain, Some("example.com"))
    assertEquals(cookie.path, Some("/api"))
    assertEquals(cookie.maxAge, Some(7200L))
    assertEquals(cookie.secure, true)
    assertEquals(cookie.httpOnly, true)
    assertEquals(cookie.sameSite, Some(Cookie.SameSite.Lax))
  }

  test("Cookie.parseCookieHeader with single cookie") {
    val header  = "session=abc123"
    val cookies = Cookie.parseCookieHeader(header)
    assertEquals(cookies, Map("session" -> "abc123"))
  }

  test("Cookie.parseCookieHeader with multiple cookies") {
    val header  = "session=abc123; user=john; theme=dark"
    val cookies = Cookie.parseCookieHeader(header)
    assertEquals(
      cookies,
      Map("session" -> "abc123", "user" -> "john", "theme" -> "dark")
    )
  }

  test("Cookie.parseCookieHeader with spaces") {
    val header  = "session = abc123 ; user = john"
    val cookies = Cookie.parseCookieHeader(header)
    assertEquals(cookies, Map("session" -> "abc123", "user" -> "john"))
  }

  test("Cookie.fromHeaders extracts cookies from Cookie header") {
    val headers = Map("Cookie" -> List("session=abc123; user=john"))
    val cookies = Cookie.fromHeaders(headers)
    assertEquals(cookies, Map("session" -> "abc123", "user" -> "john"))
  }

  test("Cookie.fromHeaders handles lowercase cookie header") {
    val headers = Map("cookie" -> List("session=abc123"))
    val cookies = Cookie.fromHeaders(headers)
    assertEquals(cookies, Map("session" -> "abc123"))
  }

  test("Cookie.fromHeaders returns empty map when no cookies") {
    val headers = Map("Content-Type" -> List("text/html"))
    val cookies = Cookie.fromHeaders(headers)
    assertEquals(cookies, Map.empty[String, String])
  }

  test("Request.cookies extension method") {
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List("session=abc123; user=john")),
      body = ""
    )

    val cookies = request.cookies
    assertEquals(cookies, Map("session" -> "abc123", "user" -> "john"))
  }

  test("Request.cookie extension method") {
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List("session=abc123; user=john")),
      body = ""
    )

    assertEquals(request.cookie("session"), Some("abc123"))
    assertEquals(request.cookie("user"), Some("john"))
    assertEquals(request.cookie("missing"), None)
  }

  test("Response.withCookie using Cookie model") {
    val response = Response(200, "OK")
    val cookie   = Cookie("session", "abc123").withSecure.withHttpOnly
    val updated  = response.withCookie(cookie)

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 1)
    assert(setCookieHeaders.head.contains("session=abc123"))
    assert(setCookieHeaders.head.contains("Secure"))
    assert(setCookieHeaders.head.contains("HttpOnly"))
  }

  test("Response.withCookie using name and value") {
    val response = Response(200, "OK")
    val updated  = response.withCookie("session", "abc123")

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 1)
    assert(setCookieHeaders.head.contains("session=abc123"))
  }

  test("Response.withCookie can set multiple cookies") {
    val response = Response(200, "OK")
      .withCookie("session", "abc123")
      .withCookie("user", "john")

    val setCookieHeaders = response.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 2)
    assert(setCookieHeaders.exists(_.contains("session=abc123")))
    assert(setCookieHeaders.exists(_.contains("user=john")))
  }

  test("Response.deleteCookie sets Max-Age to 0") {
    val response = Response(200, "OK")
    val updated  = response.deleteCookie("session")

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 1)
    assert(setCookieHeaders.head.contains("session="))
    assert(setCookieHeaders.head.contains("Max-Age=0"))
  }

  test("Response.deleteCookie with domain and path") {
    val response = Response(200, "OK")
    val updated  = response.deleteCookie(
      "session",
      domain = Some("example.com"),
      path = Some("/admin")
    )

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assert(setCookieHeaders.head.contains("Domain=example.com"))
    assert(setCookieHeaders.head.contains("Path=/admin"))
    assert(setCookieHeaders.head.contains("Max-Age=0"))
  }

}
