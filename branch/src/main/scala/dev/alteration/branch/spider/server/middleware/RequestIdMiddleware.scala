package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, Response}

import java.util.UUID

/** Middleware for injecting request IDs.
  *
  * Adds a unique request ID to:
  *   - Request headers (for use by handlers)
  *   - Response headers (for client correlation)
  *
  * If the request already has an X-Request-ID header, it will be reused.
  * Otherwise, a new UUID will be generated.
  *
  * Example usage:
  * {{{
  *   val handler = myHandler.withMiddleware(RequestIdMiddleware())
  * }}}
  */
class RequestIdMiddleware[I, O](
    val requestHeaderName: String = "X-Request-ID",
    val responseHeaderName: String = "X-Request-ID"
) extends Middleware[I, O] {

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    val requestId = request.headers
      .get(requestHeaderName)
      .flatMap(_.headOption)
      .getOrElse(UUID.randomUUID().toString)

    val updatedRequest = request.copy(
      headers = request.headers + (requestHeaderName -> List(requestId))
    )

    Continue(updatedRequest)
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    request.headers
      .get(requestHeaderName)
      .flatMap(_.headOption) match {
      case Some(requestId) =>
        response.copy(
          headers =
            response.headers + (responseHeaderName -> List(requestId))
        )
      case None =>
        response // Should never happen if preProcess ran
    }
  }
}

object RequestIdMiddleware {

  /** Create a RequestIdMiddleware with default header names. */
  def apply[I, O](): RequestIdMiddleware[I, O] =
    new RequestIdMiddleware[I, O]()

  /** Create a RequestIdMiddleware with custom header names.
    *
    * @param requestHeaderName
    *   The header name for request ID in requests
    * @param responseHeaderName
    *   The header name for request ID in responses
    */
  def apply[I, O](
      requestHeaderName: String,
      responseHeaderName: String
  ): RequestIdMiddleware[I, O] =
    new RequestIdMiddleware[I, O](requestHeaderName, responseHeaderName)
}

/** Extension methods for accessing request IDs. */
extension [I](request: Request[I]) {

  /** Get the request ID from the request headers, if present.
    *
    * @return
    *   Some(requestId) if present, None otherwise
    */
  def requestId: Option[String] =
    request.headers.get("X-Request-ID").flatMap(_.headOption)
}
