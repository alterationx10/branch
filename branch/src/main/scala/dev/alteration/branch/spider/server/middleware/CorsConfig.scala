package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod

/** Configuration for CORS (Cross-Origin Resource Sharing).
  *
  * @param allowedOrigins
  *   Set of allowed origins. Use Set("*") for wildcard (all origins). Specific
  *   origins should include the full origin (e.g., "https://example.com")
  * @param allowedMethods
  *   Set of allowed HTTP methods for cross-origin requests
  * @param allowedHeaders
  *   Set of allowed request headers. Use Set("*") for wildcard
  * @param exposedHeaders
  *   Set of headers that browsers are allowed to access
  * @param allowCredentials
  *   Whether to allow credentials (cookies, authorization headers)
  * @param maxAge
  *   How long (in seconds) preflight results can be cached. None means no
  *   caching
  */
case class CorsConfig(
    allowedOrigins: Set[String] = Set("*"),
    allowedMethods: Set[HttpMethod] =
      Set(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE),
    allowedHeaders: Set[String] = Set("*"),
    exposedHeaders: Set[String] = Set.empty,
    allowCredentials: Boolean = false,
    maxAge: Option[Long] = Some(3600)
)

object CorsConfig {

  /** Permissive CORS configuration that allows all origins, methods, and
    * headers. Useful for development but not recommended for production.
    */
  val permissive: CorsConfig = CorsConfig(
    allowedOrigins = Set("*"),
    allowedMethods = HttpMethod.values.toSet,
    allowedHeaders = Set("*"),
    allowCredentials = false,
    maxAge = Some(3600)
  )

  /** Restrictive CORS configuration that only allows GET requests from specific
    * origins. Use `.withOrigins()` to specify allowed origins.
    */
  val restrictive: CorsConfig = CorsConfig(
    allowedOrigins = Set.empty,
    allowedMethods = Set(HttpMethod.GET),
    allowedHeaders = Set("Content-Type"),
    allowCredentials = false,
    maxAge = Some(1800)
  )

  extension (config: CorsConfig) {

    /** Set allowed origins */
    def withOrigins(origins: String*): CorsConfig =
      config.copy(allowedOrigins = origins.toSet)

    /** Set allowed methods */
    def withMethods(methods: HttpMethod*): CorsConfig =
      config.copy(allowedMethods = methods.toSet)

    /** Set allowed headers */
    def withHeaders(headers: String*): CorsConfig =
      config.copy(allowedHeaders = headers.toSet)

    /** Set exposed headers */
    def withExposedHeaders(headers: String*): CorsConfig =
      config.copy(exposedHeaders = headers.toSet)

    /** Allow credentials (cookies, auth headers) */
    def withCredentials: CorsConfig =
      config.copy(allowCredentials = true)

    /** Disallow credentials */
    def withoutCredentials: CorsConfig =
      config.copy(allowCredentials = false)

    /** Set preflight cache max age in seconds */
    def withMaxAge(seconds: Long): CorsConfig =
      config.copy(maxAge = Some(seconds))

    /** Disable preflight caching */
    def withoutMaxAge: CorsConfig =
      config.copy(maxAge = None)

    /** Check if a specific origin is allowed.
      *
      * @param origin
      *   The origin to check
      * @return
      *   true if the origin is allowed
      */
    def isOriginAllowed(origin: String): Boolean =
      config.allowedOrigins.contains("*") || config.allowedOrigins.contains(
        origin
      )

    /** Check if a specific method is allowed.
      *
      * @param method
      *   The HTTP method to check
      * @return
      *   true if the method is allowed
      */
    def isMethodAllowed(method: HttpMethod): Boolean =
      config.allowedMethods.contains(method)

    /** Check if specific headers are allowed.
      *
      * @param headers
      *   The headers to check (comma-separated or list)
      * @return
      *   true if all headers are allowed
      */
    def areHeadersAllowed(headers: List[String]): Boolean =
      config.allowedHeaders.contains("*") || headers.forall(h =>
        config.allowedHeaders.contains(h.trim.toLowerCase)
      )
  }
}
