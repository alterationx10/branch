package dev.wishingtree.branch.http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}

import java.time.Duration

trait RouteHandler(val routeContext: String) {

  lazy val getHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val headHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val optionsHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val traceHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val putHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val deleteHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val postHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val patchHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  lazy val connectHandler: RequestHandler[?, ?] =
    RequestHandler.unimplementedHandler

  private[http] inline def httpHandler: HttpHandler = {
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
            start    <- Lazy.now
            response <- lzyHandle.recover { e =>
                          Lazy.fn {
                            val msg = e.getMessage.getBytes()
                            exchange.sendResponseHeaders(500, msg.length)
                            exchange.getResponseBody.write(msg)
                            exchange.getResponseBody.close()
                          }
                        }
            end      <- Lazy.now
            _        <-
              Lazy.println(
                s"Handled ${exchange.getRequestMethod} ${exchange.getRequestURI} in ${Duration.between(start, end).getSeconds / 1000f} ms"
              )
          } yield ()

        }
        exchange.close()
      }
  }

}

object RouteHandler {

  inline def registerHandler[H <: RouteHandler](
      handler: H
  )(using httpServer: HttpServer): Unit =
    httpServer
      .createContext(handler.routeContext, handler.httpHandler)
}
