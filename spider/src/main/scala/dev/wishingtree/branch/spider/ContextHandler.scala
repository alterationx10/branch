package dev.wishingtree.branch.spider

import com.sun.net.httpserver.{
  Filter,
  HttpExchange,
  HttpHandler,
  HttpServer,
  Authenticator
}
import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}

import scala.jdk.CollectionConverters.*
import java.time.{Duration, Instant}

trait ContextHandler(val path: String) {

  val filters: Seq[Filter] =
    Seq.empty

  val authenticator: Option[Authenticator] =
    Option.empty

  val contextRouter: PartialFunction[(HttpVerb, String), RequestHandler[?, ?]]

  private[spider] inline def httpHandler: HttpHandler = {
    (exchange: HttpExchange) =>
      {
        Lazy
          .fn {
            HttpVerb
              .fromString(exchange.getRequestMethod.toUpperCase)
              .map(v => v -> exchange.getRequestURI.getPath.toLowerCase)
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

  val timingFilter: Filter = new Filter {
    override def doFilter(exchange: HttpExchange, chain: Filter.Chain): Unit = {
      val start = Instant.now()
      chain.doFilter(exchange)
      val end   = Instant.now()
      println(
        f"Handled ${exchange.getRequestMethod} ${exchange.getRequestURI} in ${Duration.between(start, end).getSeconds / 1000f}%.2f ms"
      )
    }

    override def description(): String = "Print timing for handling requests."
  }

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
