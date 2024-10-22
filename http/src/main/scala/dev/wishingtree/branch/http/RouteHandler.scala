package dev.wishingtree.branch.http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import dev.wishingtree.branch.lzy.{Lazy, LazyRuntime}

import scala.compiletime.summonInline
import scala.jdk.CollectionConverters.*

trait RequestHandler[I, O] {
  given requestDecoder: Conversion[Array[Byte], I]
  given responseEncoder: Conversion[O, Array[Byte]]

  case class Request[A](headers: Map[String, List[String]], body: A)
  case class Response[A](headers: Map[String, List[String]], body: A)

  def handle(request: Request[I]): Response[O]

  private def decodeRequest(
      exchange: HttpExchange
  ): Lazy[Request[I]] =
    for {
      headers <- Lazy
        .fn {
          exchange.getRequestHeaders.asScala
            .map((k, v) => k -> v.asScala.toList)
            .toMap
        }
      rawBody <- Lazy.fn(exchange.getRequestBody.readAllBytes())
      body <- Lazy.fn(requestDecoder(rawBody))
    } yield Request(headers, body)

  private def sendResponse(response: Response[O])(
      exchange: HttpExchange
  ): Lazy[Unit] = {
    for {
      rawResponse <- Lazy.fn(responseEncoder(response.body))
      _ <- Lazy.fn(exchange.sendResponseHeaders(200, rawResponse.length))
      _ <- Lazy.fn {
        response.headers.foreach { (k, v) =>
          exchange.getResponseHeaders.set(k, v.mkString(","))
        }
      }
      _ <- Lazy.fn(exchange.getResponseBody.write(rawResponse))
    } yield ()
  }

  private[http] inline def lzyRun(
      exchange: HttpExchange
  ): Lazy[Unit] = {
    for {
      request <- decodeRequest(exchange)
      response <- Lazy.fn(this.handle(request))
      _ <- sendResponse(response)(exchange)
    } yield ()
  }

}

trait RouteHandler(val route: String) {

  val getHandler: RequestHandler[?, ?]

  private[http] inline def httpHandler: HttpHandler = {
    new HttpHandler:
      override def handle(exchange: HttpExchange): Unit = {
        val lzyHandle: Lazy[Unit] =
          HttpVerb.fromString(exchange.getRequestMethod.toUpperCase) match {
            case Some(HttpVerb.GET) =>
              getHandler.lzyRun(exchange)
          }
        val result = LazyRuntime.runSync(lzyHandle)
        exchange.close()
      }

  }

}

object RouteHandler {

  inline def registerHandler[H <: RouteHandler](
      httpServer: HttpServer
  )(handler: H): Unit =
    httpServer
      .createContext(handler.route)
      .setHandler(handler.httpHandler)
}
