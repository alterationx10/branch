package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.blammo.BaseLogger
import dev.alteration.branch.spider.server.{Request, Response}

import java.util.logging.{ConsoleHandler, Level}

/** Middleware for logging HTTP requests and responses.
  *
  * Logs:
  *   - Request method, URI, and headers (on entry)
  *   - Response status code, timing, and headers (on exit)
  *
  * Example usage:
  * {{{
  *   val handler = myHandler.withMiddleware(LoggingMiddleware())
  * }}}
  */
class LoggingMiddleware[I, O] extends Middleware[I, O] with BaseLogger {

  val handler = new ConsoleHandler()

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    logger.info(
      s"→ ${request.uri.getPath} | Headers: ${request.headers.keys.mkString(", ")}"
    )
    Continue(request)
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    logger.info(
      s"← ${request.uri.getPath} | Status: ${response.statusCode}"
    )
    response
  }
}

object LoggingMiddleware {
  def apply[I, O](): LoggingMiddleware[I, O] = new LoggingMiddleware[I, O]()
}

/** Middleware for logging HTTP requests with timing information.
  *
  * Logs:
  *   - Request details on entry
  *   - Response details and elapsed time on exit
  *
  * Example usage:
  * {{{
  *   val handler = myHandler.withMiddleware(TimingMiddleware())
  * }}}
  */
class TimingMiddleware[I, O] extends Middleware[I, O] with BaseLogger {

  val handler = new ConsoleHandler()

  // Thread-local storage for request start times
  private val startTimes = ThreadLocal.withInitial[Long](() => 0L)

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    startTimes.set(System.nanoTime())
    logger.info(s"→ ${request.uri}")
    Continue(request)
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    val elapsedMs = (System.nanoTime() - startTimes.get()) / 1_000_000.0
    logger.info(
      s"← ${request.uri.getPath} | ${response.statusCode} | ${elapsedMs}ms"
    )
    startTimes.remove() // Clean up thread-local storage
    response
  }
}

object TimingMiddleware {
  def apply[I, O](): TimingMiddleware[I, O] = new TimingMiddleware[I, O]()

  /** Create a timing middleware with custom log level.
    *
    * @param level
    *   The log level to use
    */
  def apply[I, O](level: Level): TimingMiddleware[I, O] =
    new TimingMiddleware[I, O] {
      override val logLevel: Level = level
    }
}
