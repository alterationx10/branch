package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.server.{Request, Response}
import dev.alteration.branch.spider.server.middleware.CorsConfig.*

/** Middleware for handling CORS (Cross-Origin Resource Sharing).
  *
  * This middleware:
  *   - Handles preflight OPTIONS requests
  *   - Adds appropriate CORS headers to responses
  *   - Validates origins, methods, and headers against the configuration
  *
  * @param config
  *   The CORS configuration to use
  */
class CorsMiddleware[I, O](config: CorsConfig) extends Middleware[I, O] {

  /** Handle preflight OPTIONS requests or validate CORS for regular requests.
    */
  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    // Get the origin from the request headers
    val origin = request.headers
      .get("Origin")
      .orElse(request.headers.get("origin"))
      .flatMap(_.headOption)

    origin match {
      case Some(originValue) if config.isOriginAllowed(originValue) =>
        // Check if this is a preflight request (OPTIONS with specific headers)
        val method = request.headers
          .get("Access-Control-Request-Method")
          .orElse(request.headers.get("access-control-request-method"))
          .flatMap(_.headOption)
          .flatMap(HttpMethod.fromString)

        val isPreflightRequest = method.isDefined

        if (isPreflightRequest) {
          // Handle preflight request - respond immediately
          handlePreflightRequest(originValue, method.get, request)
        } else {
          // Regular CORS request - continue processing
          Continue(request)
        }

      case Some(_) =>
        // Origin not allowed - continue but don't add CORS headers
        Continue(request)

      case None =>
        // No origin header - not a CORS request
        Continue(request)
    }
  }

  /** Add CORS headers to the response. */
  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    val origin = request.headers
      .get("Origin")
      .orElse(request.headers.get("origin"))
      .flatMap(_.headOption)

    origin match {
      case Some(originValue) if config.isOriginAllowed(originValue) =>
        addCorsHeaders(response, originValue)
      case _                                                        =>
        // No origin or origin not allowed - don't add CORS headers
        response
    }
  }

  /** Handle a preflight OPTIONS request by returning appropriate CORS headers.
    */
  private def handlePreflightRequest(
      origin: String,
      requestedMethod: HttpMethod,
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    // Get requested headers
    val requestedHeaders = request.headers
      .get("Access-Control-Request-Headers")
      .orElse(request.headers.get("access-control-request-headers"))
      .flatMap(_.headOption)
      .map(_.split(",").map(_.trim).toList)
      .getOrElse(List.empty)

    // Validate the requested method and headers
    val methodAllowed  = config.isMethodAllowed(requestedMethod)
    val headersAllowed = config.areHeadersAllowed(requestedHeaders)

    if (methodAllowed && headersAllowed) {
      // Create preflight response with CORS headers
      val response = Response[O](
        statusCode = 204, // No Content
        body = null.asInstanceOf[O],
        headers = Map.empty
      )

      val corsResponse = addPreflightHeaders(response, origin, requestedHeaders)
      Respond(corsResponse)
    } else {
      // Preflight validation failed - respond with 403 Forbidden
      val response = Response[O](
        statusCode = 403,
        body = null.asInstanceOf[O],
        headers = Map.empty
      )
      Respond(response)
    }
  }

  /** Add CORS headers to a regular response. */
  private def addCorsHeaders(
      response: Response[O],
      origin: String
  ): Response[O] = {
    val allowOrigin =
      if (config.allowedOrigins.contains("*") && !config.allowCredentials) {
        "*"
      } else {
        origin
      }

    var updatedResponse = response
      .withHeader("Access-Control-Allow-Origin" -> allowOrigin)

    // Add Vary header to indicate that the response varies by Origin
    if (allowOrigin != "*") {
      updatedResponse = updatedResponse.withHeader("Vary" -> "Origin")
    }

    // Add credentials header if enabled
    if (config.allowCredentials) {
      updatedResponse =
        updatedResponse.withHeader("Access-Control-Allow-Credentials" -> "true")
    }

    // Add exposed headers if specified
    if (config.exposedHeaders.nonEmpty) {
      val exposedHeadersStr = config.exposedHeaders.mkString(", ")
      updatedResponse = updatedResponse.withHeader(
        "Access-Control-Expose-Headers" -> exposedHeadersStr
      )
    }

    updatedResponse
  }

  /** Add preflight-specific CORS headers. */
  private def addPreflightHeaders(
      response: Response[O],
      origin: String,
      requestedHeaders: List[String]
  ): Response[O] = {
    val allowOrigin =
      if (config.allowedOrigins.contains("*") && !config.allowCredentials) {
        "*"
      } else {
        origin
      }

    var updatedResponse = response
      .withHeader("Access-Control-Allow-Origin" -> allowOrigin)

    // Add allowed methods
    val methodsStr = config.allowedMethods.map(_.toString).mkString(", ")
    updatedResponse =
      updatedResponse.withHeader("Access-Control-Allow-Methods" -> methodsStr)

    // Add allowed headers
    val headersStr =
      if (config.allowedHeaders.contains("*")) {
        requestedHeaders.mkString(", ")
      } else {
        config.allowedHeaders.mkString(", ")
      }
    if (headersStr.nonEmpty) {
      updatedResponse =
        updatedResponse.withHeader("Access-Control-Allow-Headers" -> headersStr)
    }

    // Add credentials header if enabled
    if (config.allowCredentials) {
      updatedResponse =
        updatedResponse.withHeader("Access-Control-Allow-Credentials" -> "true")
    }

    // Add max age if specified
    config.maxAge.foreach { age =>
      updatedResponse =
        updatedResponse.withHeader("Access-Control-Max-Age" -> age.toString)
    }

    // Add Vary header
    if (allowOrigin != "*") {
      updatedResponse = updatedResponse.withHeader("Vary" -> "Origin")
    }

    updatedResponse
  }
}

object CorsMiddleware {

  /** Create a CORS middleware with the given configuration. */
  def apply[I, O](config: CorsConfig): CorsMiddleware[I, O] =
    new CorsMiddleware[I, O](config)

  /** Create a permissive CORS middleware (allows all origins). Useful for
    * development.
    */
  def permissive[I, O]: CorsMiddleware[I, O] =
    new CorsMiddleware[I, O](CorsConfig.permissive)

  /** Create a restrictive CORS middleware. Use `.withOrigins()` to specify
    * allowed origins.
    */
  def restrictive[I, O]: CorsMiddleware[I, O] =
    new CorsMiddleware[I, O](CorsConfig.restrictive)
}
