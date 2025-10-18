package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.server.SignedCookie.*

import java.net.URI

class SignedCookieSpec extends munit.FunSuite {

  val secret = "my-secret-key-for-testing"

  test("SignedCookie.sign creates signed value") {
    val value  = "abc123"
    val signed = SignedCookie.sign(value, secret)

    assert(signed.contains("."))
    assert(signed.startsWith("abc123."))
  }

  test("SignedCookie.unsign validates correct signature") {
    val value  = "abc123"
    val signed = SignedCookie.sign(value, secret)
    val result = SignedCookie.verify(signed, secret)

    assertEquals(result, Some("abc123"))
  }

  test("SignedCookie.unsign rejects tampered value") {
    val value  = "abc123"
    val signed = SignedCookie.sign(value, secret)
    // Tamper with the value
    val tampered = signed.replace("abc123", "xyz789")
    val result   = SignedCookie.verify(tampered, secret)

    assertEquals(result, None)
  }

  test("SignedCookie.unsign rejects wrong secret") {
    val value       = "abc123"
    val signed      = SignedCookie.sign(value, secret)
    val wrongSecret = "wrong-secret"
    val result      = SignedCookie.verify(signed, wrongSecret)

    assertEquals(result, None)
  }

  test("SignedCookie.unsign rejects malformed value") {
    val result1 = SignedCookie.verify("no-separator", secret)
    assertEquals(result1, None)

    val result2 = SignedCookie.verify("too.many.separators", secret)
    assertEquals(result2, None)
  }

  test("SignedCookie.create makes signed cookie") {
    val cookie = SignedCookie.create("session", "abc123", secret)

    assertEquals(cookie.name, "session")
    assert(cookie.value.contains("."))
    assert(cookie.value.startsWith("abc123."))

    // Verify we can unsign it
    val unsigned = SignedCookie.verify(cookie.value, secret)
    assertEquals(unsigned, Some("abc123"))
  }

  test("Cookie.signed extension method") {
    val cookie = Cookie("session", "abc123")
      .withSecure
      .withHttpOnly
      .signed(secret)

    assert(cookie.value.contains("."))
    assert(cookie.value.startsWith("abc123."))
    assertEquals(cookie.secure, true)
    assertEquals(cookie.httpOnly, true)

    // Verify we can unsign it
    val unsigned = SignedCookie.verify(cookie.value, secret)
    assertEquals(unsigned, Some("abc123"))
  }

  test("Map[String, String].signedCookie extension retrieves and validates") {
    val signed  = SignedCookie.sign("abc123", secret)
    val cookies = Map("session" -> signed, "user" -> "john")

    val result = cookies.signedCookie("session", secret)
    assertEquals(result, Some("abc123"))
  }

  test("Map[String, String].signedCookie returns None for missing cookie") {
    val cookies = Map("user" -> "john")
    val result  = cookies.signedCookie("session", secret)
    assertEquals(result, None)
  }

  test("Map[String, String].signedCookie returns None for tampered cookie") {
    val signed   = SignedCookie.sign("abc123", secret)
    val tampered = signed.replace("abc123", "xyz789")
    val cookies  = Map("session" -> tampered)

    val result = cookies.signedCookie("session", secret)
    assertEquals(result, None)
  }

  test("Request.signedCookie extension method") {
    val signed  = SignedCookie.sign("abc123", secret)
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List(s"session=$signed; user=john")),
      body = ""
    )

    val result = request.signedCookie("session", secret)
    assertEquals(result, Some("abc123"))
  }

  test("Request.signedCookie returns None for unsigned cookie") {
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List("session=abc123")),
      body = ""
    )

    val result = request.signedCookie("session", secret)
    assertEquals(result, None)
  }

  test("Response.withSignedCookie using Cookie model") {
    val response = Response(200, "OK")
    val cookie   = Cookie("session", "abc123").withSecure
    val updated  = response.withSignedCookie(cookie, secret)

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 1)
    assert(setCookieHeaders.head.contains("session=abc123."))
    assert(setCookieHeaders.head.contains("Secure"))

    // Verify the signature is valid
    val cookieValue = setCookieHeaders.head.split("=")(1).split(";")(0)
    val unsigned    = SignedCookie.verify(cookieValue, secret)
    assertEquals(unsigned, Some("abc123"))
  }

  test("Response.withSignedCookie using name and value") {
    val response = Response(200, "OK")
    val updated  = response.withSignedCookie("session", "abc123", secret)

    val setCookieHeaders = updated.headers.getOrElse("Set-Cookie", List.empty)
    assertEquals(setCookieHeaders.length, 1)
    assert(setCookieHeaders.head.contains("session=abc123."))

    // Verify the signature is valid
    val cookieValue = setCookieHeaders.head.split("=")(1).split(";")(0)
    val unsigned    = SignedCookie.verify(cookieValue, secret)
    assertEquals(unsigned, Some("abc123"))
  }

  test("End-to-end: sign on response, verify on request") {
    // Set a signed cookie in response
    val response = Response(200, "OK")
      .withSignedCookie("session", "user123", secret)

    // Extract the Set-Cookie header
    val setCookieHeader =
      response.headers.getOrElse("Set-Cookie", List.empty).head
    val cookieValue     = setCookieHeader.split("=")(1).split(";")(0)

    // Simulate request with this cookie
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List(s"session=$cookieValue")),
      body = ""
    )

    // Verify we can retrieve and validate it
    val result = request.signedCookie("session", secret)
    assertEquals(result, Some("user123"))
  }

  test("Signed cookies prevent value tampering") {
    // Set a signed cookie
    val response        = Response(200, "OK")
      .withSignedCookie("role", "user", secret)
    val setCookieHeader =
      response.headers.getOrElse("Set-Cookie", List.empty).head
    val signedValue     = setCookieHeader.split("=")(1).split(";")(0)

    // Attacker tries to change "user" to "admin"
    val tamperedValue = signedValue.replace("user", "admin")

    // Create request with tampered cookie
    val request = Request(
      uri = URI.create("http://example.com/"),
      headers = Map("Cookie" -> List(s"role=$tamperedValue")),
      body = ""
    )

    // Verification should fail
    val result = request.signedCookie("role", secret)
    assertEquals(result, None)
  }

}
