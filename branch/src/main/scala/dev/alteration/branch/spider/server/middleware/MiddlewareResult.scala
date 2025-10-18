package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.macaroni.typeclasses.*

/** Result type for middleware processing.
  *
  * Middleware can either:
  *   - Continue with a (possibly modified) request
  *   - Short-circuit and respond immediately
  *
  * @tparam Resp
  *   Response type (for short-circuit responses)
  * @tparam Req
  *   Request type (for continuing)
  */
sealed trait MiddlewareResult[+Resp, +Req]

/** Continue processing with the given request.
  *
  * @param request
  *   The request to pass to the next middleware or handler
  */
case class Continue[Req](request: Req) extends MiddlewareResult[Nothing, Req]

/** Short-circuit and respond immediately without calling the handler.
  *
  * @param response
  *   The response to return
  */
case class Respond[Resp](response: Resp) extends MiddlewareResult[Resp, Nothing]

object MiddlewareResult {

  /** Bifunctor instance for MiddlewareResult.
    *
    * Allows mapping over both the response (left) and request (right) sides.
    */
  given Bifunctor[MiddlewareResult] with {
    def bimap[A, B, C, D](
        fab: MiddlewareResult[A, B]
    )(f: A => C, g: B => D): MiddlewareResult[C, D] =
      fab match {
        case Continue(req) => Continue(g(req))
        case Respond(resp) => Respond(f(resp))
      }
  }

  /** Functor instance for MiddlewareResult[Resp, *].
    *
    * Maps over the request (right) side, treating response as fixed.
    */
  given [Resp]: Functor[[Req] =>> MiddlewareResult[Resp, Req]] with {
    def map[A, B](
        fa: MiddlewareResult[Resp, A]
    )(f: A => B): MiddlewareResult[Resp, B] =
      fa match {
        case Continue(req) => Continue(f(req))
        case Respond(resp) => Respond(resp)
      }
  }

  /** Monad instance for MiddlewareResult[Resp, *].
    *
    * Enables flatMap/for-comprehension support for chaining middleware
    * operations.
    */
  given [Resp]: Monad[[Req] =>> MiddlewareResult[Resp, Req]] with {
    def pure[A](a: A): MiddlewareResult[Resp, A] = Continue(a)

    def ap[A, B](
        ff: MiddlewareResult[Resp, A => B]
    )(fa: MiddlewareResult[Resp, A]): MiddlewareResult[Resp, B] =
      ff match {
        case Respond(resp) => Respond(resp)
        case Continue(f)   =>
          fa match {
            case Respond(resp) => Respond(resp)
            case Continue(a)   => Continue(f(a))
          }
      }

    def flatMap[A, B](
        fa: MiddlewareResult[Resp, A]
    )(f: A => MiddlewareResult[Resp, B]): MiddlewareResult[Resp, B] =
      fa match {
        case Continue(req) => f(req)
        case Respond(resp) => Respond(resp)
      }

    override def map[A, B](
        fa: MiddlewareResult[Resp, A]
    )(f: A => B): MiddlewareResult[Resp, B] =
      fa match {
        case Continue(req) => Continue(f(req))
        case Respond(resp) => Respond(resp)
      }
  }

  /** Semigroup instance for MiddlewareResult.
    *
    * Combines two middleware results:
    *   - If first responds, use that response
    *   - If first continues and second responds, use second's response
    *   - If both continue, use second's request
    */
  given [Resp, Req]: Semigroup[MiddlewareResult[Resp, Req]] with {
    def combine(
        a: MiddlewareResult[Resp, Req],
        b: MiddlewareResult[Resp, Req]
    ): MiddlewareResult[Resp, Req] =
      a match {
        case Respond(resp) => Respond(resp) // Short-circuit wins
        case Continue(_)   => b             // Continue to next result
      }
  }

  // Extension methods for ergonomic usage
  extension [Resp, Req](result: MiddlewareResult[Resp, Req]) {

    /** Check if this result is a Continue. */
    def isContinue: Boolean = result match {
      case Continue(_) => true
      case _           => false
    }

    /** Check if this result is a Respond. */
    def isRespond: Boolean = result match {
      case Respond(_) => true
      case _          => false
    }

    /** Get the request if this is a Continue, None otherwise. */
    def getRequest: Option[Req] = result match {
      case Continue(req) => Some(req)
      case _             => None
    }

    /** Get the response if this is a Respond, None otherwise. */
    def getResponse: Option[Resp] = result match {
      case Respond(resp) => Some(resp)
      case _             => None
    }

    /** Map over the request side (only affects Continue). */
    def mapRequest[Req2](f: Req => Req2): MiddlewareResult[Resp, Req2] =
      result match {
        case Continue(req) => Continue(f(req))
        case Respond(resp) => Respond(resp)
      }

    /** Map over the response side (only affects Respond). */
    def mapResponse[Resp2](f: Resp => Resp2): MiddlewareResult[Resp2, Req] =
      result match {
        case Continue(req) => Continue(req)
        case Respond(resp) => Respond(f(resp))
      }
  }
}
