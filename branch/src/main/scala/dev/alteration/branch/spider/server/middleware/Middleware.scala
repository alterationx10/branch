package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.macaroni.typeclasses.*
import dev.alteration.branch.spider.server.{Request, Response, RequestHandler}

/** Middleware for HTTP request/response processing.
  *
  * Middleware can:
  *   - Pre-process requests (modify or short-circuit before handler)
  *   - Post-process responses (modify after handler)
  *   - Combine both pre and post-processing
  *
  * @tparam I
  *   Input type (request body type)
  * @tparam O
  *   Output type (response body type)
  */
trait Middleware[I, O] {

  /** Pre-process a request before it reaches the handler.
    *
    * @param request
    *   The incoming request
    * @return
    *   Either Continue(modifiedRequest) or Respond(response) to short-circuit
    */
  def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] =
    Continue(request)

  /** Post-process a response after the handler has executed.
    *
    * @param request
    *   The original request
    * @param response
    *   The response from the handler
    * @return
    *   The modified response
    */
  def postProcess(request: Request[I], response: Response[O]): Response[O] =
    response

  /** Compose this middleware with another, creating a chain.
    *
    * The other middleware runs after this one. Short-circuits stop the chain.
    *
    * @param other
    *   The middleware to run after this one
    * @return
    *   A new middleware that runs both in sequence
    */
  def andThen(other: Middleware[I, O]): Middleware[I, O] = {
    val self = this
    new Middleware[I, O] {
      override def preProcess(
          request: Request[I]
      ): MiddlewareResult[Response[O], Request[I]] =
        self.preProcess(request) match {
          case Continue(req)  => other.preProcess(req)
          case Respond(resp)  => Respond(resp)
        }

      override def postProcess(
          request: Request[I],
          response: Response[O]
      ): Response[O] =
        other.postProcess(request, self.postProcess(request, response))
    }
  }

  /** Apply this middleware to a handler, creating a wrapped handler.
    *
    * @param handler
    *   The handler to wrap
    * @return
    *   A new handler that runs the middleware before/after the original
    */
  def apply(handler: RequestHandler[I, O])(using
      requestDecoder: Conversion[Array[Byte], I],
      responseEncoder: Conversion[O, Array[Byte]]
  ): RequestHandler[I, O] = {
    val middleware = this
    new RequestHandler[I, O] {
      override def handle(request: Request[I]): Response[O] =
        middleware.preProcess(request) match {
          case Continue(req) =>
            val response = handler.handle(req)
            middleware.postProcess(req, response)
          case Respond(resp) =>
            resp
        }
    }
  }

  /** Alias for andThen, for more natural chaining syntax. */
  def >>(other: Middleware[I, O]): Middleware[I, O] = andThen(other)
}

object Middleware {

  /** Create a middleware that only does pre-processing.
    *
    * @param f
    *   The pre-processing function
    */
  def preOnly[I, O](
      f: Request[I] => MiddlewareResult[Response[O], Request[I]]
  ): Middleware[I, O] =
    new Middleware[I, O] {
      override def preProcess(
          request: Request[I]
      ): MiddlewareResult[Response[O], Request[I]] =
        f(request)
    }

  /** Create a middleware that only does post-processing.
    *
    * @param f
    *   The post-processing function
    */
  def postOnly[I, O](
      f: (Request[I], Response[O]) => Response[O]
  ): Middleware[I, O] =
    new Middleware[I, O] {
      override def postProcess(
          request: Request[I],
          response: Response[O]
      ): Response[O] =
        f(request, response)
    }

  /** Create a middleware from both pre and post-processing functions.
    *
    * @param pre
    *   The pre-processing function
    * @param post
    *   The post-processing function
    */
  def apply[I, O](
      pre: Request[I] => MiddlewareResult[Response[O], Request[I]],
      post: (Request[I], Response[O]) => Response[O]
  ): Middleware[I, O] =
    new Middleware[I, O] {
      override def preProcess(
          request: Request[I]
      ): MiddlewareResult[Response[O], Request[I]] =
        pre(request)

      override def postProcess(
          request: Request[I],
          response: Response[O]
      ): Response[O] =
        post(request, response)
    }

  /** Identity middleware that does nothing. */
  def identity[I, O]: Middleware[I, O] = new Middleware[I, O] {}

  /** Monoid instance for Middleware composition.
    *
    * Enables combining middleware using |+| operator.
    */
  given [I, O]: Monoid[Middleware[I, O]] with {
    def empty: Middleware[I, O] = identity[I, O]

    def combine(
        a: Middleware[I, O],
        b: Middleware[I, O]
    ): Middleware[I, O] =
      a.andThen(b)
  }

  /** Combine multiple middleware into a single middleware chain.
    *
    * @param middlewares
    *   The middleware to chain together
    * @return
    *   A single middleware that runs all of them in order
    */
  def chain[I, O](middlewares: Middleware[I, O]*): Middleware[I, O] =
    middlewares.foldLeft(identity[I, O])(_.andThen(_))

  /** Extension methods for RequestHandler to apply middleware. */
  extension [I, O](handler: RequestHandler[I, O]) {

    /** Apply a middleware to this handler.
      *
      * @param middleware
      *   The middleware to apply
      * @return
      *   A new handler wrapped with the middleware
      */
    def withMiddleware(middleware: Middleware[I, O])(using
        requestDecoder: Conversion[Array[Byte], I],
        responseEncoder: Conversion[O, Array[Byte]]
    ): RequestHandler[I, O] =
      middleware(handler)

    /** Apply multiple middleware to this handler.
      *
      * @param middlewares
      *   The middleware to apply (in order)
      * @return
      *   A new handler wrapped with all middleware
      */
    def withMiddlewares(
        middlewares: Middleware[I, O]*
    )(using
        requestDecoder: Conversion[Array[Byte], I],
        responseEncoder: Conversion[O, Array[Byte]]
    ): RequestHandler[I, O] =
      chain(middlewares*)(handler)
  }
}
