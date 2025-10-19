package dev.alteration.branch.spider.server

import dev.alteration.branch.macaroni.crypto.Crypto

/** Utilities for creating and validating signed cookies using HMAC.
  *
  * Signed cookies provide tamper protection by appending an HMAC signature to
  * the cookie value. This prevents clients from modifying cookie values
  * without detection.
  *
  * The signed cookie format is: `value.signature` where the signature is an
  * HMAC-SHA256 hash of the value.
  */
object SignedCookie {

  private val SEPARATOR = "."

  /** Sign a cookie value using HMAC-SHA256
    *
    * @param value
    *   The cookie value to sign
    * @param secret
    *   The secret key for HMAC signing
    * @return
    *   The signed value in format: value.signature
    */
  def sign(value: String, secret: String): String = {
    val signature = Crypto.hmac256(value, secret)
    s"$value$SEPARATOR$signature"
  }

  /** Verify a signed cookie value
    *
    * @param signedValue
    *   The signed cookie value (value.signature)
    * @param secret
    *   The secret key for HMAC validation
    * @return
    *   Some(value) if valid, None if invalid or tampered
    */
  def verify(signedValue: String, secret: String): Option[String] = {
    // Find the last separator to split value from signature
    val lastDotIndex = signedValue.lastIndexOf(SEPARATOR)
    if (lastDotIndex == -1) {
      None
    } else {
      val value     = signedValue.substring(0, lastDotIndex)
      val signature = signedValue.substring(lastDotIndex + 1)
      if (Crypto.validateHmac256(value, secret, signature)) {
        Some(value)
      } else {
        None
      }
    }
  }

  /** Create a signed cookie
    *
    * @param name
    *   The cookie name
    * @param value
    *   The cookie value to sign
    * @param secret
    *   The secret key for HMAC signing
    * @return
    *   A Cookie with a signed value
    */
  def create(name: String, value: String, secret: String): Cookie = {
    Cookie(name, sign(value, secret))
  }

  extension (c: Cookie) {

    /** Sign this cookie's value
      *
      * @param secret
      *   The secret key for HMAC signing
      * @return
      *   A new Cookie with signed value
      */
    def signed(secret: String): Cookie = {
      c.copy(value = sign(c.value, secret))
    }
  }

  extension (cookies: Map[String, String]) {

    /** Get and validate a signed cookie value
      *
      * @param name
      *   The cookie name
      * @param secret
      *   The secret key for HMAC validation
      * @return
      *   Some(value) if cookie exists and is valid, None otherwise
      */
    def signedCookie(name: String, secret: String): Option[String] = {
      cookies.get(name).flatMap(verify(_, secret))
    }
  }

  extension [A](r: Request[A]) {

    /** Get and validate a signed cookie from the request
      *
      * @param name
      *   The cookie name
      * @param secret
      *   The secret key for HMAC validation
      * @return
      *   Some(value) if cookie exists and is valid, None otherwise
      */
    def signedCookie(name: String, secret: String): Option[String] = {
      r.cookies.signedCookie(name, secret)
    }
  }

  extension [A](r: Response[A]) {

    /** Set a signed cookie on the response
      *
      * @param cookie
      *   The cookie to sign and set
      * @param secret
      *   The secret key for HMAC signing
      * @return
      *   Response with the signed cookie set
      */
    def withSignedCookie(cookie: Cookie, secret: String): Response[A] = {
      r.withCookie(cookie.signed(secret))
    }

    /** Set a simple signed cookie with just name and value
      *
      * @param name
      *   The cookie name
      * @param value
      *   The cookie value
      * @param secret
      *   The secret key for HMAC signing
      * @return
      *   Response with the signed cookie set
      */
    def withSignedCookie(name: String, value: String, secret: String): Response[A] = {
      r.withCookie(create(name, value, secret))
    }
  }

}
