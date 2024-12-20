package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.*
import dev.wishingtree.branch.lzy.Lazy
import dev.wishingtree.branch.lzy.abstractions.Semigroup
import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import java.time.{Duration, Instant}
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

/** A base trait for handling requests, rooted at a specific path.
  */
trait ContextHandler(val path: String) {

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
    (HttpMethod, Path),
    RequestHandler[?, ?]
  ]

  private[spider] inline def httpHandler: HttpHandler = {
    (exchange: HttpExchange) =>
      {
        Lazy
          .fn {
            HttpMethod
              .fromString(exchange.getRequestMethod.toUpperCase)
              .map(v => v -> Path.of(exchange.getRequestURI.getPath).relativeTo("/"))
              .filter(contextRouter.isDefinedAt)
              .map(contextRouter)
              .getOrElse(RequestHandler.unimplementedHandler)
          }
          .flatMap(_.lzyRun(exchange))
          .recover { e =>
            Lazy.fn {
              val msg = e.getMessage.getBytes()
              exchange.sendResponseHeaders(500, msg.length)
              exchange.getResponseBody.write(msg)
              exchange.getResponseBody.close()
            }
          }
          .runSync()

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
          : PartialFunction[(HttpMethod, Path), RequestHandler[?, ?]] =
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
