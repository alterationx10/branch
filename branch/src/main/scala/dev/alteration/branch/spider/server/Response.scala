package dev.alteration.branch.spider.server

import dev.alteration.branch.spider.common.ContentType

/** A response model for an HTTP request.
  */
case class Response[A](
    statusCode: Int,
    body: A,
    headers: Map[String, List[String]] = Map(
      ContentType.bin.toHeader
    )
)

object Response {

  // Redirect helpers

  /** Creates a 301 Moved Permanently redirect response.
    */
  def movedPermanently(location: String): Response[String] =
    Response(301, "", Map("Location" -> List(location)))

  /** Creates a 302 Found (temporary redirect) response.
    */
  def found(location: String): Response[String] =
    Response(302, "", Map("Location" -> List(location)))

  /** Creates a 307 Temporary Redirect response (preserves HTTP method).
    */
  def temporaryRedirect(location: String): Response[String] =
    Response(307, "", Map("Location" -> List(location)))

  /** Creates a 308 Permanent Redirect response (preserves HTTP method).
    */
  def permanentRedirect(location: String): Response[String] =
    Response(308, "", Map("Location" -> List(location)))

  // Error response builders

  /** Creates a 400 Bad Request response.
    */
  def badRequest(message: String = "Bad Request"): Response[String] =
    Response(400, message, Map(ContentType.txt.toHeader))

  /** Creates a 401 Unauthorized response.
    */
  def unauthorized(message: String = "Unauthorized"): Response[String] =
    Response(401, message, Map(ContentType.txt.toHeader))

  /** Creates a 403 Forbidden response.
    */
  def forbidden(message: String = "Forbidden"): Response[String] =
    Response(403, message, Map(ContentType.txt.toHeader))

  /** Creates a 404 Not Found response.
    */
  def notFound(message: String = "Not Found"): Response[String] =
    Response(404, message, Map(ContentType.txt.toHeader))

  /** Creates a 409 Conflict response.
    */
  def conflict(message: String = "Conflict"): Response[String] =
    Response(409, message, Map(ContentType.txt.toHeader))

  /** Creates a 422 Unprocessable Entity response.
    */
  def unprocessableEntity(
      message: String = "Unprocessable Entity"
  ): Response[String] =
    Response(422, message, Map(ContentType.txt.toHeader))

  /** Creates a 429 Too Many Requests response.
    */
  def tooManyRequests(
      message: String = "Too Many Requests",
      retryAfter: Option[Int] = None
  ): Response[String] = {
    val headers = retryAfter match {
      case Some(seconds) =>
        Map(ContentType.txt.toHeader, "Retry-After" -> List(seconds.toString))
      case None          => Map(ContentType.txt.toHeader)
    }
    Response(429, message, headers)
  }

  /** Creates a 500 Internal Server Error response.
    */
  def internalServerError(
      message: String = "Internal Server Error"
  ): Response[String] =
    Response(500, message, Map(ContentType.txt.toHeader))

  /** Creates a 501 Not Implemented response.
    */
  def notImplemented(message: String = "Not Implemented"): Response[String] =
    Response(501, message, Map(ContentType.txt.toHeader))

  /** Creates a 502 Bad Gateway response.
    */
  def badGateway(message: String = "Bad Gateway"): Response[String] =
    Response(502, message, Map(ContentType.txt.toHeader))

  /** Creates a 503 Service Unavailable response.
    */
  def serviceUnavailable(
      message: String = "Service Unavailable",
      retryAfter: Option[Int] = None
  ): Response[String] = {
    val headers = retryAfter match {
      case Some(seconds) =>
        Map(ContentType.txt.toHeader, "Retry-After" -> List(seconds.toString))
      case None          => Map(ContentType.txt.toHeader)
    }
    Response(503, message, headers)
  }

  /** Creates a 504 Gateway Timeout response.
    */
  def gatewayTimeout(message: String = "Gateway Timeout"): Response[String] =
    Response(504, message, Map(ContentType.txt.toHeader))

  extension (sc: StringContext) {

    /** A string interpolator for creating a text/html response.
      *
      * {{{
      *   html"""
      *   <h1>Hello, $name!</h1>
      *   """
      * }}}
      */
    def html(args: Any*): Response[String] = {
      Response(200, sc.s(args*).trim, Map(ContentType.html.toHeader))
    }

    /** A string interpolator for creating an application/json response.
      *
      * {{{
      *   json"""
      *   {
      *    "message": "Hello, $name!"
      *   }
      *   """
      * }}}
      */
    def json(args: Any*): Response[String] =
      Response(200, sc.s(args*).strip(), Map(ContentType.json.toHeader))
  }

  extension [A](r: Response[A]) {

    /** Adds a header to the response.
      */
    def withHeader(header: (String, String)) =
      r.copy(headers = r.headers + (header._1 -> List(header._2)))

    /** Adds a content type header to the response.
      */
    def withContentType(contentType: ContentType): Response[A] =
      r.copy(headers = r.headers + contentType.toHeader)

    /** Adds a content type header to the response.
      */
    def withContentType(contentType: String): Response[A] =
      withContentType(ContentType(contentType))

    /** Sets the response content type to [[ContentType.txt]].
      */
    def textContent: Response[A] =
      r.withContentType(ContentType.txt)

    /** Sets the response content type to [[ContentType.html]].
      */
    def htmlContent: Response[A] =
      r.withContentType(ContentType.html)

    /** Try to automatically set the contentType via [[ContentType.contentPF]]
      */
    def autoContent(ext: String): Response[A] = {
      val sanitized   = ext.split("\\.").toList.last
      val contentType = ContentType.contentPF(sanitized)
      r.withContentType(contentType)
    }

    /** Set a cookie on the response using a Cookie model */
    def withCookie(cookie: Cookie): Response[A] = {
      val cookieHeader = cookie.toSetCookieHeader
      val existing     = r.headers.getOrElse("Set-Cookie", List.empty)
      r.copy(headers = r.headers + ("Set-Cookie" -> (existing :+ cookieHeader)))
    }

    /** Set a simple cookie with just name and value */
    def withCookie(name: String, value: String): Response[A] =
      withCookie(Cookie(name, value))

    /** Delete a cookie by setting Max-Age to 0 */
    def deleteCookie(
        name: String,
        domain: Option[String] = None,
        path: Option[String] = Some("/")
    ): Response[A] = {
      val cookie = Cookie(name, "", domain, path, maxAge = Some(0))
      withCookie(cookie)
    }
  }

}
