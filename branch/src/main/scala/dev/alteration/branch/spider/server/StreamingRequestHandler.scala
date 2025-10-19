package dev.alteration.branch.spider.server

import java.io.InputStream
import scala.util.Try

/** A handler for requests with streaming bodies.
  *
  * Unlike RequestHandler which buffers the entire request body in memory,
  * StreamingRequestHandler provides access to the raw input stream for
  * processing large request bodies efficiently.
  *
  * Example usage:
  * {{{
  * val uploadHandler = new StreamingRequestHandler[String] {
  *   override def handle(request: Request[StreamingRequest]): Response[String] = {
  *     var totalBytes = 0L
  *
  *     request.body.readChunks { chunk =>
  *       totalBytes += chunk.length
  *       // Process chunk (e.g., write to file, database, etc.)
  *     }
  *
  *     Response(200, s"Uploaded $totalBytes bytes")
  *   }
  * }
  * }}}
  *
  * @tparam O The type of the response body (must have a `Conversion[O, Array[Byte]]` in scope)
  */
trait StreamingRequestHandler[O](using
    responseEncoder: Conversion[O, Array[Byte]]
) {

  /** Handle a request with a streaming body and return a response.
    *
    * @param request The request with StreamingRequest body
    * @return The response
    */
  def handle(request: Request[StreamingRequest]): Response[O]

  /** Internal method used by the server to create a StreamingRequest from the
    * parsed HTTP headers and input stream.
    *
    * This is called by SpiderServer before invoking handle().
    */
  private[spider] final def createStreamingRequest(
      input: InputStream,
      headers: Map[String, List[String]],
      config: ServerConfig
  ): StreamingRequest = {
    // Extract Content-Length
    val contentLength = headers
      .find { case (k, _) => k.equalsIgnoreCase("Content-Length") }
      .flatMap(_._2.headOption)
      .flatMap(s => Try(s.toLong).toOption)

    // Check for Transfer-Encoding: chunked
    val isChunked = headers
      .find { case (k, _) => k.equalsIgnoreCase("Transfer-Encoding") }
      .flatMap(_._2.headOption)
      .exists(_.toLowerCase.contains("chunked"))

    new StreamingRequest(
      input,
      contentLength,
      isChunked,
      config.maxRequestBodySize
    )
  }

}

object StreamingRequestHandler {

  given Conversion[String, Array[Byte]] = _.getBytes()
  given Conversion[Array[Byte], Array[Byte]] = identity

}
