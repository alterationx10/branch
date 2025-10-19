package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.macaroni.crypto.Crypto
import dev.alteration.branch.spider.server.{Cookie, Request, Response}

import java.security.SecureRandom

/** Utilities for generating and validating CSRF tokens. */
object CsrfToken {

  private val secureRandom = new SecureRandom()

  /** Generate a cryptographically secure CSRF token.
    *
    * @param length
    *   Length of the token in bytes (will be base64 encoded)
    * @return
    *   Base64-encoded token string
    */
  def generate(length: Int = 32): String = {
    val bytes = new Array[Byte](length)
    secureRandom.nextBytes(bytes)
    Crypto.base64Encode(bytes)
  }

  /** Validate a CSRF token using constant-time comparison.
    *
    * @param expected
    *   The expected token value
    * @param actual
    *   The actual token value from the request
    * @return
    *   true if tokens match
    */
  def validate(expected: String, actual: String): Boolean = {
    if (expected.isEmpty || actual.isEmpty) {
      false
    } else {
      Crypto.slowEquals(expected.getBytes("UTF-8"), actual.getBytes("UTF-8"))
    }
  }

  /** Extract CSRF token from request cookies.
    *
    * @param request
    *   The request to extract from
    * @param cookieName
    *   Name of the CSRF cookie
    * @return
    *   The token if found
    */
  def fromCookie[I](request: Request[I], cookieName: String): Option[String] = {
    request.cookie(cookieName).filter(_.nonEmpty)
  }

  /** Extract CSRF token from request header.
    *
    * @param request
    *   The request to extract from
    * @param headerName
    *   Name of the CSRF header
    * @return
    *   The token if found
    */
  def fromHeader[I](request: Request[I], headerName: String): Option[String] = {
    request.headers
      .get(headerName)
      .flatMap(_.headOption)
      .filter(_.nonEmpty)
  }

  /** Extract CSRF token from request (tries header first, then form field).
    *
    * Note: For form field extraction, the request body must be parsed. This
    * method only checks the header. For form field support, parse the body
    * first.
    *
    * @param request
    *   The request to extract from
    * @param headerName
    *   Name of the CSRF header
    * @return
    *   The token if found
    */
  def fromRequest[I](
      request: Request[I],
      headerName: String
  ): Option[String] = {
    fromHeader(request, headerName)
  }

  extension [O](response: Response[O]) {

    /** Add a CSRF token to the response as a cookie.
      *
      * @param token
      *   The CSRF token to set
      * @param config
      *   CSRF configuration
      * @return
      *   Response with CSRF cookie set
      */
    def withCsrfToken(token: String, config: CsrfConfig): Response[O] = {
      val cookie = Cookie(config.cookieName, token)
        .withPath("/")
        .withHttpOnly
        .copy(
          secure = config.cookieSecure,
          httpOnly = config.cookieHttpOnly,
          sameSite = config.cookieSameSite
        )
      response.withCookie(cookie)
    }

    /** Generate and add a new CSRF token to the response.
      *
      * @param config
      *   CSRF configuration
      * @return
      *   Tuple of (Response with CSRF cookie, generated token)
      */
    def withNewCsrfToken(
        config: CsrfConfig
    ): (Response[O], String) = {
      val token            = generate(config.tokenLength)
      val updatedResponse  = response.withCsrfToken(token, config)
      (updatedResponse, token)
    }
  }

  extension [I](request: Request[I]) {

    /** Get the CSRF token from this request's cookies.
      *
      * @param config
      *   CSRF configuration
      * @return
      *   The token if found
      */
    def csrfTokenFromCookie(config: CsrfConfig): Option[String] = {
      fromCookie(request, config.cookieName)
    }

    /** Get the CSRF token from this request's headers.
      *
      * @param config
      *   CSRF configuration
      * @return
      *   The token if found
      */
    def csrfTokenFromHeader(config: CsrfConfig): Option[String] = {
      fromHeader(request, config.headerName)
    }

    /** Validate the CSRF token in this request.
      *
      * Compares the token from the cookie (expected) with the token from the
      * header (actual).
      *
      * @param config
      *   CSRF configuration
      * @return
      *   true if the token is valid
      */
    def validateCsrfToken(config: CsrfConfig): Boolean = {
      (
        csrfTokenFromCookie(config),
        csrfTokenFromHeader(config)
      ) match {
        case (Some(expected), Some(actual)) => validate(expected, actual)
        case _                              => false
      }
    }
  }

  /** Create an HTML hidden input field for embedding CSRF token in forms.
    *
    * @param token
    *   The CSRF token
    * @param fieldName
    *   Name of the form field
    * @return
    *   HTML string for the hidden input
    */
  def hiddenField(token: String, fieldName: String = "csrf_token"): String = {
    s"""<input type="hidden" name="$fieldName" value="$token">"""
  }

  /** Create a meta tag for embedding CSRF token in HTML head.
    *
    * Useful for JavaScript frameworks to read the token.
    *
    * @param token
    *   The CSRF token
    * @return
    *   HTML string for the meta tag
    */
  def metaTag(token: String): String = {
    s"""<meta name="csrf-token" content="$token">"""
  }
}
