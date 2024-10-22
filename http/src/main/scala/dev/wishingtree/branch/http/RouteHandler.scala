package dev.wishingtree.branch.http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}

trait RouteHandler(val route: String) {

  val getHandler: RequestHandler[?, ?]

  private[http] inline def httpHandler: HttpHandler = {
    (exchange: HttpExchange) =>
      {
        val lzyHandle: Lazy[Unit] =
          HttpVerb.fromString(exchange.getRequestMethod.toUpperCase) match {
            case Some(HttpVerb.GET) =>
              getHandler.lzyRun(exchange)
          }
        val result                = LazyRuntime.runSync(lzyHandle)
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
