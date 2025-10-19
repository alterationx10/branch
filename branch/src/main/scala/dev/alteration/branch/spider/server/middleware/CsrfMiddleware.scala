package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.{Request, Response}
import dev.alteration.branch.spider.server.middleware.CsrfToken.*

/** Middleware for CSRF (Cross-Site Request Forgery) protection.
  *
  * This middleware:
  *   - Generates CSRF tokens for new sessions
  *   - Validates CSRF tokens on non-safe HTTP methods
  *   - Allows exempting specific paths and methods
  *   - Uses double-submit cookie pattern for token validation
  *
  * @param config
  *   The CSRF configuration to use
  */
class CsrfMiddleware[I, O](config: CsrfConfig) extends Middleware[I, O] {

  /** Validate CSRF token for non-exempt requests. */
  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    // Check if this request needs CSRF validation
    val needsValidation = shouldValidate(request)

    if (needsValidation) {
      // Validate the CSRF token
      if (request.validateCsrfToken(config)) {
        // Token is valid - continue
        Continue(request)
      } else {
        // Token is invalid or missing - reject with 403 Forbidden
        val response = Response[O](
          statusCode = 403,
          body = null.asInstanceOf[O],
          headers = Map.empty
        )
        Respond(response)
      }
    } else {
      // No validation needed - continue
      Continue(request)
    }
  }

  /** Ensure response has a CSRF token cookie. */
  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    // Check if the request already has a CSRF token
    val existingToken = request.csrfTokenFromCookie(config)

    existingToken match {
      case Some(token) =>
        // Token exists - ensure it's in the response cookie
        response.withCsrfToken(token, config)
      case None =>
        // No token - generate a new one
        val (updatedResponse, _) = response.withNewCsrfToken(config)
        updatedResponse
    }
  }

  /** Determine if a request should be validated for CSRF. */
  private def shouldValidate(request: Request[I]): Boolean = {
    // Parse the HTTP method from headers or use a default
    // In a real implementation, you'd get this from the parsed request
    val path = request.uri.getPath

    // Check if path is exempt
    val pathExempt = config.isPathExempt(path)

    // We need to check the method, but it's not directly available in Request[I]
    // For now, we'll check based on whether the request has a CSRF header/cookie
    // In practice, you'd want to pass the HTTP method through or store it in Request

    // If path is exempt, no validation needed
    if (pathExempt) {
      false
    } else {
      // Need validation if not a safe method (this is a simplified check)
      // In real use, you'd check the actual HTTP method from the request context
      true
    }
  }
}

object CsrfMiddleware {

  /** Create a CSRF middleware with the given configuration. */
  def apply[I, O](config: CsrfConfig): CsrfMiddleware[I, O] =
    new CsrfMiddleware[I, O](config)

  /** Create a CSRF middleware with default configuration. */
  def default[I, O]: CsrfMiddleware[I, O] =
    new CsrfMiddleware[I, O](CsrfConfig.default)

  /** Create a CSRF middleware with strict configuration. */
  def strict[I, O]: CsrfMiddleware[I, O] =
    new CsrfMiddleware[I, O](CsrfConfig.strict)

  /** Create a CSRF middleware with development configuration. */
  def development[I, O]: CsrfMiddleware[I, O] =
    new CsrfMiddleware[I, O](CsrfConfig.development)
}

/** Enhanced CSRF middleware that has access to HTTP method information.
  *
  * This version provides better method-based validation by including the HTTP
  * method in the request processing.
  */
class CsrfMiddlewareWithMethod[I, O](
    config: CsrfConfig,
    methodExtractor: Request[I] => Option[HttpMethod]
) extends Middleware[I, O] {

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    val path   = request.uri.getPath
    val method = methodExtractor(request)

    // Determine if validation is needed
    val needsValidation = method match {
      case Some(m) if config.isMethodExempt(m) => false
      case _ if config.isPathExempt(path)      => false
      case _                                   => true
    }

    if (needsValidation) {
      if (request.validateCsrfToken(config)) {
        Continue(request)
      } else {
        val response = Response[O](
          statusCode = 403,
          body = null.asInstanceOf[O],
          headers = Map.empty
        )
        Respond(response)
      }
    } else {
      Continue(request)
    }
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    val existingToken = request.csrfTokenFromCookie(config)

    existingToken match {
      case Some(token) =>
        response.withCsrfToken(token, config)
      case None =>
        val (updatedResponse, _) = response.withNewCsrfToken(config)
        updatedResponse
    }
  }
}

object CsrfMiddlewareWithMethod {

  /** Create a CSRF middleware with method-aware validation.
    *
    * @param config
    *   CSRF configuration
    * @param methodExtractor
    *   Function to extract HTTP method from request
    */
  def apply[I, O](
      config: CsrfConfig,
      methodExtractor: Request[I] => Option[HttpMethod]
  ): CsrfMiddlewareWithMethod[I, O] =
    new CsrfMiddlewareWithMethod[I, O](config, methodExtractor)

  /** Create with default config and method extractor. */
  def default[I, O](
      methodExtractor: Request[I] => Option[HttpMethod]
  ): CsrfMiddlewareWithMethod[I, O] =
    new CsrfMiddlewareWithMethod[I, O](CsrfConfig.default, methodExtractor)
}
