package dev.wishingtree.branch.spider

import com.sun.net.httpserver.HttpExchange
import dev.wishingtree.branch.lzy.Lazy

import java.net.URI
import scala.jdk.CollectionConverters.*

trait RequestHandler[I, O](using
    requestDecoder: Conversion[Array[Byte], I],
    responseEncoder: Conversion[O, Array[Byte]]
) {

  private def parseQueryParams(qpStr: String): Map[String, String] = {
    qpStr
      .split("&")
      .map { case s"$key=$value" =>
        key -> value
      }
      .toMap
  }

  case class Request[A](uri: URI, headers: Map[String, List[String]], body: A) {
    final lazy val queryParams = parseQueryParams(uri.getQuery)
  }
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
      body    <- Lazy.fn(requestDecoder(rawBody))
    } yield Request(exchange.getRequestURI, headers, body)

  private def sendResponse(response: Response[O])(
      exchange: HttpExchange
  ): Lazy[Unit] = {
    for {
      rawResponse <- Lazy.fn(responseEncoder(response.body))
      _           <- Lazy.fn(exchange.sendResponseHeaders(200, rawResponse.length))
      _           <- Lazy.fn {
                       response.headers.foreach { (k, v) =>
                         exchange.getResponseHeaders.set(k, v.mkString(","))
                       }
                     }
      _           <- Lazy.fn(exchange.getResponseBody.write(rawResponse))
    } yield ()
  }

  private[spider] inline def lzyRun(
      exchange: HttpExchange
  ): Lazy[Unit] = {
    for {
      request  <- decodeRequest(exchange)
      response <- Lazy.fn(this.handle(request))
      _        <- sendResponse(response)(exchange)
    } yield ()
  }

}

object RequestHandler {

  given Conversion[Array[Byte], Array[Byte]] = identity

  given Conversion[Array[Byte], Unit] = _ => ()
  given Conversion[Unit, Array[Byte]] = _ => Array.empty

  given Conversion[Array[Byte], String] = ba => new String(ba)
  given Conversion[String, Array[Byte]] = _.getBytes()

  private[spider] val unimplementedHandler: RequestHandler[Unit, Unit] =
    new RequestHandler[Unit, Unit] {

      override def handle(request: Request[Unit]): Response[Unit] =
        throw new Exception("Not implemented")
    }

}
