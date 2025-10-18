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
    def deleteCookie(name: String, domain: Option[String] = None, path: Option[String] = Some("/")): Response[A] = {
      val cookie = Cookie(name, "", domain, path, maxAge = Some(0))
      withCookie(cookie)
    }
  }

}
