package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.Cookie

/** Configuration for session middleware.
  *
  * @param cookieName
  *   Name of the session ID cookie
  * @param maxAge
  *   Session duration in seconds
  * @param secure
  *   Whether to set the Secure flag on the cookie
  * @param httpOnly
  *   Whether to set the HttpOnly flag on the cookie
  * @param sameSite
  *   SameSite attribute for the cookie
  * @param path
  *   Path attribute for the cookie
  * @param domain
  *   Optional domain attribute for the cookie
  * @param rotateOnAuth
  *   Whether to rotate session ID on authentication events
  * @param slidingExpiration
  *   Whether to extend expiration on each request
  */
case class SessionConfig(
    cookieName: String = "SESSION_ID",
    maxAge: Long = 3600, // 1 hour default
    secure: Boolean = true,
    httpOnly: Boolean = true,
    sameSite: Cookie.SameSite = Cookie.SameSite.Strict,
    path: String = "/",
    domain: Option[String] = None,
    rotateOnAuth: Boolean = true,
    slidingExpiration: Boolean = true
)

object SessionConfig {

  /** Default session configuration with secure defaults. */
  def default: SessionConfig = SessionConfig()

  /** Development configuration with relaxed security for easier testing. */
  def development: SessionConfig = SessionConfig(
    secure = false,
    sameSite = Cookie.SameSite.Lax,
    maxAge = 7200 // 2 hours
  )

  /** Strict configuration with maximum security. */
  def strict: SessionConfig = SessionConfig(
    secure = true,
    httpOnly = true,
    sameSite = Cookie.SameSite.Strict,
    rotateOnAuth = true,
    slidingExpiration = false,
    maxAge = 1800 // 30 minutes
  )

  /** Long-lived session configuration (e.g., "remember me" functionality). */
  def longLived: SessionConfig = SessionConfig(
    maxAge = 2592000, // 30 days
    slidingExpiration = true
  )
}
