package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.Cookie

/** Configuration for CSRF (Cross-Site Request Forgery) protection.
  *
  * @param tokenLength
  *   Length of the generated CSRF token (default: 32 bytes)
  * @param cookieName
  *   Name of the cookie that stores the CSRF token
  * @param headerName
  *   Name of the request header that should contain the CSRF token
  * @param fieldName
  *   Name of the form field for CSRF token (for POST forms)
  * @param exemptMethods
  *   HTTP methods that don't require CSRF validation (typically safe methods)
  * @param exemptPaths
  *   Paths that are exempt from CSRF validation (e.g., public APIs)
  * @param cookieSecure
  *   Whether the CSRF cookie should have the Secure flag
  * @param cookieHttpOnly
  *   Whether the CSRF cookie should have the HttpOnly flag
  * @param cookieSameSite
  *   SameSite attribute for the CSRF cookie
  */
case class CsrfConfig(
    tokenLength: Int = 32,
    cookieName: String = "XSRF-TOKEN",
    headerName: String = "X-XSRF-TOKEN",
    fieldName: String = "csrf_token",
    exemptMethods: Set[HttpMethod] =
      Set(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
    exemptPaths: Set[String] = Set.empty,
    cookieSecure: Boolean = true,
    cookieHttpOnly: Boolean = false, // Must be false so JS can read it
    cookieSameSite: Option[Cookie.SameSite] = Some(Cookie.SameSite.Strict)
)

object CsrfConfig {

  /** Default CSRF configuration suitable for most web applications. */
  val default: CsrfConfig = CsrfConfig()

  /** Strict CSRF configuration with stronger security settings. */
  val strict: CsrfConfig = CsrfConfig(
    tokenLength = 64,
    cookieSecure = true,
    cookieHttpOnly = false,
    cookieSameSite = Some(Cookie.SameSite.Strict)
  )

  /** Development CSRF configuration with relaxed settings. */
  val development: CsrfConfig = CsrfConfig(
    cookieSecure = false,
    cookieSameSite = Some(Cookie.SameSite.Lax)
  )

  extension (config: CsrfConfig) {

    /** Set the token length in bytes */
    def withTokenLength(length: Int): CsrfConfig =
      config.copy(tokenLength = length)

    /** Set the cookie name */
    def withCookieName(name: String): CsrfConfig =
      config.copy(cookieName = name)

    /** Set the header name */
    def withHeaderName(name: String): CsrfConfig =
      config.copy(headerName = name)

    /** Set the form field name */
    def withFieldName(name: String): CsrfConfig =
      config.copy(fieldName = name)

    /** Set exempt HTTP methods */
    def withExemptMethods(methods: HttpMethod*): CsrfConfig =
      config.copy(exemptMethods = methods.toSet)

    /** Add paths that are exempt from CSRF validation */
    def withExemptPaths(paths: String*): CsrfConfig =
      config.copy(exemptPaths = config.exemptPaths ++ paths)

    /** Set all exempt paths (replaces existing) */
    def withExemptPathsOnly(paths: String*): CsrfConfig =
      config.copy(exemptPaths = paths.toSet)

    /** Enable secure cookies */
    def withSecureCookie: CsrfConfig =
      config.copy(cookieSecure = true)

    /** Disable secure cookies (for development) */
    def withoutSecureCookie: CsrfConfig =
      config.copy(cookieSecure = false)

    /** Set SameSite attribute for the cookie */
    def withSameSite(sameSite: Cookie.SameSite): CsrfConfig =
      config.copy(cookieSameSite = Some(sameSite))

    /** Remove SameSite attribute */
    def withoutSameSite: CsrfConfig =
      config.copy(cookieSameSite = None)

    /** Check if a path is exempt from CSRF validation */
    def isPathExempt(path: String): Boolean =
      config.exemptPaths.contains(path) || config.exemptPaths.exists(pattern =>
        matchesPattern(path, pattern)
      )

    /** Check if a method is exempt from CSRF validation */
    def isMethodExempt(method: HttpMethod): Boolean =
      config.exemptMethods.contains(method)

    /** Simple pattern matching for paths (supports * wildcard) */
    private def matchesPattern(path: String, pattern: String): Boolean = {
      if (pattern.contains("*")) {
        val regex = pattern.replace("*", ".*").r
        regex.matches(path)
      } else {
        path == pattern
      }
    }
  }
}
