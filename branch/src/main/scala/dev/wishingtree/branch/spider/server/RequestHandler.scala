package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.HttpExchange
import dev.wishingtree.branch.lzy.Lazy

import scala.jdk.CollectionConverters.*

/** A base trait to extend for handling a Request and returning a Response.
  * @tparam I
  *   The type of the request body (must have a `Conversion[Array[Byte], I]` in
  *   scope)
  * @tparam O
  *   The type of the response body (must have a `Conversion[O, Array[Byte]]` in
  *   scope)
  */
trait RequestHandler[I, O](using
    requestDecoder: Conversion[Array[Byte], I],
    responseEncoder: Conversion[O, Array[Byte]]
) {

  /** Handle a request and return a response.
    * @param request
    * @return
    */
  def handle(request: Request[I]): Response[O]

  /** Decode the Request from the HttpExchange.
    */
  private final def decodeRequest(
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

  /** Send the Response to the HttpExchange.
    */
  private final def sendResponse(response: Response[O])(
      exchange: HttpExchange
  ): Lazy[Unit] = {
    for {
      rawResponse <- Lazy.fn(responseEncoder(response.body))
      _           <- Lazy.fn {
                       response.headers.foreach { (k, v) =>
                         exchange.getResponseHeaders.set(k, v.mkString(","))
                       }
                     }
      _           <- Lazy.fn(exchange.sendResponseHeaders(200, rawResponse.length))
      _           <- Lazy.fn(exchange.getResponseBody.write(rawResponse))
    } yield ()
  }

  private[spider] inline final def lzyRun(
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
