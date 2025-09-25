package dev.alteration.branch.spider.server

import com.sun.net.httpserver.*
import dev.alteration.branch.lzy.abstractions.Semigroup
import dev.alteration.branch.spider.HttpMethod

import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters.*
import scala.util.*

/** A base trait for handling requests, rooted at a specific path.
  */
trait ContextHandler(val path: String) {
  export dev.alteration.branch.macaroni.extensions.StringContextExtensions.ci

  /** A list of filters to apply to request/responses.
    */
  val filters: Seq[Filter] =
    Seq.empty

  /** An authenticator to use for requests.
    */
  val authenticator: Option[Authenticator] =
    Option.empty

  /** A partial function to route requests to specific handlers.
    */
  val contextRouter: PartialFunction[
    (HttpMethod, List[String]),
    RequestHandler[?, ?]
  ]

  /** A default handler for requests that are not found. Override this to
    * provide a custom 404 handler.
    */
  val notFoundRequestHandler: RequestHandler[?, ?] =
    RequestHandler.notFoundHandler

  private[spider] inline def httpHandler: HttpHandler = {
    (exchange: HttpExchange) =>
      {

        val requestHandler: RequestHandler[?, ?] =
          HttpMethod.fromString(exchange.getRequestMethod.toUpperCase) match {
            case Some(method) =>
              val path = exchange.getRequestURI.getPath
                .split("/")
                .toList
                .filter(_.nonEmpty)
              contextRouter
                .lift(method -> path)
                .getOrElse(notFoundRequestHandler)
            case None         =>
              notFoundRequestHandler
          }

        Try(requestHandler.tryRun(exchange)) match {
          case Success(value)     => ()
          case Failure(exception) =>
            Try {
              val msg = exception.getMessage.getBytes()
              exchange.sendResponseHeaders(500, msg.length)
              exchange.getResponseBody.write(msg)
              exchange.getResponseBody.close()
            }
        }

        exchange.close()
      }
  }

}

object ContextHandler {

  /** Combine two ContextHandlers into one. The combined ContextHandler will
    * have the path of the first handler, the filters of both handlers, the
    * authenticator of the first handler with the second handler as a fallback,
    * and the contextRouter of both handlers, tried in order.
    */
  given Semigroup[ContextHandler] = (a: ContextHandler, b: ContextHandler) => {
    new ContextHandler(a.path) {
      override val filters: Seq[Filter]                 = (a.filters ++ b.filters).distinct
      override val authenticator: Option[Authenticator] =
        a.authenticator.orElse(b.authenticator)
      override val contextRouter
          : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] =
        a.contextRouter orElse b.contextRouter
    }
  }

  /** A filter to print the timing of requests.
    */
  val timingFilter: Filter = new Filter {
    override def doFilter(exchange: HttpExchange, chain: Filter.Chain): Unit = {
      val start = Instant.now()
      chain.doFilter(exchange)
      val end   = Instant.now()
      println(
        f"${exchange.getResponseCode} ${exchange.getRequestMethod} ${exchange.getRequestURI} in ${Duration.between(start, end).getSeconds / 1000f}%.2f ms"
      )
    }

    override def description(): String = "Print timing for handling requests."
  }

  /** Register a handler with the HttpServer
    * @param handler
    * @param httpServer
    * @tparam H
    */
  inline def registerHandler[H <: ContextHandler](
      handler: H
  )(using httpServer: HttpServer): Unit = {
    val ctx = httpServer
      .createContext(handler.path, handler.httpHandler)
    ctx.getFilters.addAll(handler.filters.asJava)
    handler.authenticator.foreach(a => ctx.setAuthenticator(a))
  }

  inline def unregisterHandler[H <: ContextHandler](
      handler: H
  )(using httpServer: HttpServer): Unit = {
    httpServer.removeContext(handler.path)
  }

}
