package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.blammo.JsonConsoleLogger
import dev.alteration.branch.friday.JsonCodec
import dev.alteration.branch.spider.server.{Request, Response}

import java.util.logging.Level

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
class LoggingMiddleware[I, O] extends Middleware[I, O] with JsonConsoleLogger {

  case class MiddlewareLog(
      direction: String,
      headers: String,
      status: Option[Int]
  ) derives JsonCodec

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    logger.info(
      MiddlewareLog(
        direction = s"→ ${request.uri.getPath}",
        headers = request.headers.keys.mkString(", "),
        status = None
      ).toJsonString
    )
    Continue(request)
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    logger.info(
      MiddlewareLog(
        direction = s"← ${request.uri.getPath}",
        headers = response.headers.keys.mkString(", "),
        status = Some(response.statusCode)
      ).toJsonString
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
class TimingMiddleware[I, O] extends Middleware[I, O] with JsonConsoleLogger {

  case class TimingLog(
      direction: String,
      statusCode: Option[Int] = None,
      timing: Option[Double] = None
  ) derives JsonCodec

  // Thread-local storage for request start times
  private val startTimes = ThreadLocal.withInitial[Long](() => 0L)

  override def preProcess(
      request: Request[I]
  ): MiddlewareResult[Response[O], Request[I]] = {
    startTimes.set(System.nanoTime())
    logger.info(
      TimingLog(
        direction = s"→ ${request.uri}"
      ).toJsonString
    )
    Continue(request)
  }

  override def postProcess(
      request: Request[I],
      response: Response[O]
  ): Response[O] = {
    val elapsedMs = (System.nanoTime() - startTimes.get()) / 1_000_000.0
    logger.info(
      TimingLog(
        direction = s"← ${request.uri.getPath}",
        statusCode = Some(response.statusCode),
        timing = Some(elapsedMs)
      ).toJsonString
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
