package dev.wishingtree.branch.http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}

trait RouteHandler(val route: String) {

  lazy val getHandler: RequestHandler[?, ?]     = throw new Exception(
    s"GET for $route not implemented"
  )
  lazy val headHandler: RequestHandler[?, ?]    = throw new Exception(
    s"HEAD for $route not implemented"
  )
  lazy val optionsHandler: RequestHandler[?, ?] = throw new Exception(
    s"OPTIONS for $route not implemented"
  )
  lazy val traceHandler: RequestHandler[?, ?]   = throw new Exception(
    s"TRACE for $route not implemented"
  )
  lazy val putHandler: RequestHandler[?, ?]     = throw new Exception(
    s"PUT for $route not implemented"
  )
  lazy val deleteHandler: RequestHandler[?, ?]  = throw new Exception(
    s"DELETE for $route not implemented"
  )
  lazy val postHandler: RequestHandler[?, ?]    = throw new Exception(
    s"POST for $route not implemented"
  )
  lazy val patchHandler: RequestHandler[?, ?]   = throw new Exception(
    s"PATCH for $route not implemented"
  )
  lazy val connectHandler: RequestHandler[?, ?] = throw new Exception(
    s"CONNECT for $route not implemented"
  )

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
              throw new Exception(
                s"Unknown HTTP verb: ${exchange.getRequestMethod}"
              )
          }
        LazyRuntime.runSync(lzyHandle)
        exchange.close()
      }
  }

}

object RouteHandler {

  inline def registerHandler[H <: RouteHandler](
      handler: H
  )(using httpServer: HttpServer): Unit =
    httpServer
      .createContext(handler.route)
      .setHandler(handler.httpHandler)
}
