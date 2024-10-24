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

  val getHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val headHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val optionsHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val traceHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val putHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val deleteHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val postHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val patchHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  val connectHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  private[spider] inline def httpHandler: HttpHandler = {
    (exchange: HttpExchange) =>
      {
        val lzyHandle: Lazy[Unit] =
          HttpVerb.fromString(exchange.getRequestMethod.toUpperCase) match {
            case Some(HttpVerb.GET)     => getHandler.lzyRun(exchange)
            case Some(HttpVerb.HEAD)    => headHandler.lzyRun(exchange)
            case Some(HttpVerb.OPTIONS) => optionsHandler.lzyRun(exchange)
            case Some(HttpVerb.TRACE)   => traceHandler.lzyRun(exchange)
            case Some(HttpVerb.PUT)     => putHandler.lzyRun(exchange)
            case Some(HttpVerb.DELETE)  => deleteHandler.lzyRun(exchange)
            case Some(HttpVerb.POST)    => postHandler.lzyRun(exchange)
            case Some(HttpVerb.PATCH)   => patchHandler.lzyRun(exchange)
            case Some(HttpVerb.CONNECT) => connectHandler.lzyRun(exchange)
            case _                      =>
              Lazy.fail(
                new Exception(
                  s"Unknown HTTP verb: ${exchange.getRequestMethod}"
                )
              )
          }
        LazyRuntime.runSync {
          for {
            response <- lzyHandle.recover { e =>
                          Lazy.fn {
                            val msg = e.getMessage.getBytes()
                            exchange.sendResponseHeaders(500, msg.length)
                            exchange.getResponseBody.write(msg)
                            exchange.getResponseBody.close()
                          }
                        }
          } yield ()

        }
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
        s"Handled ${exchange.getRequestMethod} ${exchange.getRequestURI} in ${Duration.between(start, end).getSeconds / 1000f} ms"
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
  )(using httpServer: HttpServer) = {
    httpServer.removeContext(handler.path)
  }

}
