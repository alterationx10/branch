package dev.alteration.branch.spider.server

/** A model for an HTTP cookie with standard attributes.
  *
  * @param name
  *   The cookie name
  * @param value
  *   The cookie value
  * @param domain
  *   Optional domain attribute
  * @param path
  *   Optional path attribute (defaults to "/")
  * @param maxAge
  *   Optional Max-Age in seconds
  * @param secure
  *   Whether the Secure flag is set
  * @param httpOnly
  *   Whether the HttpOnly flag is set
  * @param sameSite
  *   Optional SameSite attribute
  */
case class Cookie(
    name: String,
    value: String,
    domain: Option[String] = None,
    path: Option[String] = Some("/"),
    maxAge: Option[Long] = None,
    secure: Boolean = false,
    httpOnly: Boolean = false,
    sameSite: Option[Cookie.SameSite] = None
)

object Cookie {

  /** SameSite attribute values */
  enum SameSite {
    case Strict, Lax, None
  }

  /** Parse a Cookie header value into a map of cookie name-value pairs.
    *
    * Example: "session=abc123; user=john" => Map("session" -> "abc123", "user"
    * -> "john")
    *
    * @param cookieHeader
    *   The Cookie header value
    * @return
    *   Map of cookie names to values
    */
  def parseCookieHeader(cookieHeader: String): Map[String, String] = {
    cookieHeader
      .split(";")
      .map(_.trim)
      .filter(_.contains("="))
      .map { pair =>
        val parts = pair.split("=", 2)
        parts(0).trim -> parts(1).trim
      }
      .toMap
  }

  /** Parse cookies from request headers.
    *
    * @param headers
    *   Request headers
    * @return
    *   Map of cookie names to values
    */
  def fromHeaders(headers: Map[String, List[String]]): Map[String, String] = {
    headers
      .get("cookie")
      .orElse(headers.get("Cookie"))
      .map(_.mkString("; "))
      .map(parseCookieHeader)
      .getOrElse(Map.empty)
  }

  extension (c: Cookie) {

    /** Convert cookie to Set-Cookie header value */
    def toSetCookieHeader: String = {
      val parts = List(
        Some(s"${c.name}=${c.value}"),
        c.domain.map(d => s"Domain=$d"),
        c.path.map(p => s"Path=$p"),
        c.maxAge.map(ma => s"Max-Age=$ma"),
        if (c.secure) Some("Secure") else None,
        if (c.httpOnly) Some("HttpOnly") else None,
        c.sameSite.map(ss => s"SameSite=$ss")
      ).flatten

      parts.mkString("; ")
    }

    /** Set the Domain attribute */
    def withDomain(domain: String): Cookie =
      c.copy(domain = Some(domain))

    /** Set the Path attribute */
    def withPath(path: String): Cookie =
      c.copy(path = Some(path))

    /** Set Max-Age in seconds */
    def withMaxAge(seconds: Long): Cookie =
      c.copy(maxAge = Some(seconds))

    /** Set the Secure flag */
    def withSecure: Cookie =
      c.copy(secure = true)

    /** Set the HttpOnly flag */
    def withHttpOnly: Cookie =
      c.copy(httpOnly = true)

    /** Set the SameSite attribute */
    def withSameSite(sameSite: SameSite): Cookie =
      c.copy(sameSite = Some(sameSite))
  }

}
