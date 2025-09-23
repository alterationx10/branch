package dev.alteration.branch.spider.server

import com.sun.net.httpserver.HttpExchange

import scala.jdk.CollectionConverters.*
import scala.util.Try

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
  ): Try[Request[I]] =
    for {
      headers <- Try {
                   exchange.getRequestHeaders.asScala
                     .map((k, v) => k -> v.asScala.toList)
                     .toMap
                 }
      rawBody <- Try(exchange.getRequestBody.readAllBytes())
      body    <- Try(requestDecoder(rawBody))
    } yield Request(exchange.getRequestURI, headers, body)

  /** Send the Response to the HttpExchange.
    */
  private final def sendResponse(response: Response[O])(
      exchange: HttpExchange
  ): Try[Unit] = {
    for {
      responseBody <- Try(responseEncoder(response.body))
      _            <- Try {
                        response.headers.foreach { (k, v) =>
                          exchange.getResponseHeaders.set(k, v.mkString(","))
                        }
                      }
      _            <- Try {
                        exchange.sendResponseHeaders(
                          response.statusCode,
                          responseBody.length
                        )
                      }
      _            <- Try(exchange.getResponseBody.write(responseBody))
    } yield ()
  }

  private[spider] final def tryRun(
      exchange: HttpExchange
  ): Try[Unit] = {
    for {
      request  <- decodeRequest(exchange)
      response <- Try(this.handle(request))
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

  private[spider] val notFoundHandler: RequestHandler[Unit, String] =
    new RequestHandler[Unit, String] {
      override def handle(request: Request[Unit]): Response[String] =
        Response(404, "Not found").htmlContent
    }

}
