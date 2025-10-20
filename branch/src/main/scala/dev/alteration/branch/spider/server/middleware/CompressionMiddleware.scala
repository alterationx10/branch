package dev.alteration.branch.spider.server.middleware

import dev.alteration.branch.spider.server.{Request, Response}
import java.io.ByteArrayOutputStream
import java.util.zip.{Deflater, DeflaterOutputStream, GZIPOutputStream}

/** Configuration for compression middleware.
  *
  * @param level
  *   Compression level (0-9, where 0=no compression, 9=best compression)
  * @param minSize
  *   Minimum response size (in bytes) to compress
  * @param excludeContentTypes
  *   Content types to exclude from compression (already compressed)
  */
case class CompressionConfig(
    level: Int = 6,
    minSize: Int = 1024,
    excludeContentTypes: Set[String] = Set(
      "image/jpeg",
      "image/png",
      "image/gif",
      "image/webp",
      "video/mp4",
      "video/webm",
      "application/zip",
      "application/gzip",
      "application/x-gzip",
      "application/x-bzip2",
      "application/x-compress",
      "application/x-compressed"
    )
)

/** Compression middleware that compresses HTTP responses based on
  * Accept-Encoding.
  *
  * Supports:
  *   - gzip encoding
  *   - deflate encoding
  *   - Accept-Encoding negotiation
  *   - Configurable compression level
  *   - Excludes already-compressed content types
  *   - Minimum size threshold
  *
  * Example:
  * {{{
  * val compression = CompressionMiddleware(
  *   CompressionConfig(level = 6, minSize = 1024)
  * )
  * val handler = compression(myHandler)
  * }}}
  */
case class CompressionMiddleware(
    config: CompressionConfig = CompressionConfig()
) extends Middleware[Array[Byte], Array[Byte]] {

  override def postProcess(
      request: Request[Array[Byte]],
      response: Response[Array[Byte]]
  ): Response[Array[Byte]] = {
    // Check if response should be compressed
    if (!shouldCompress(request, response)) {
      return response
    }

    // Get accepted encodings from request
    val acceptEncoding = request.headers
      .get("Accept-Encoding")
      .flatMap(_.headOption)
      .getOrElse("")
      .toLowerCase

    // Determine which encoding to use (prefer gzip)
    val encoding = if (acceptEncoding.contains("gzip")) {
      Some("gzip")
    } else Some("deflate").filter(acceptEncoding.contains)

    encoding match {
      case Some("gzip") =>
        val compressed = compressGzip(response.body)
        response
          .copy(body = compressed)
          .withHeader("Content-Encoding", "gzip")
          .withHeader("Content-Length", compressed.length.toString)
          .withHeader("Vary", "Accept-Encoding")

      case Some("deflate") =>
        val compressed = compressDeflate(response.body)
        response
          .copy(body = compressed)
          .withHeader("Content-Encoding", "deflate")
          .withHeader("Content-Length", compressed.length.toString)
          .withHeader("Vary", "Accept-Encoding")

      case _ =>
        response
    }
  }

  /** Check if response should be compressed. */
  private def shouldCompress(
      request: Request[Array[Byte]],
      response: Response[Array[Byte]]
  ): Boolean = {
    // Check if response is already compressed
    val contentEncoding = response.headers.get("Content-Encoding")
    if (contentEncoding.exists(_.exists(_.nonEmpty))) {
      return false
    }

    // Check response size
    if (response.body.length < config.minSize) {
      return false
    }

    // Check content type
    val contentType = response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse("")

    if (
      config.excludeContentTypes
        .exists(excluded => contentType.toLowerCase.startsWith(excluded))
    ) {
      return false
    }

    // Check if client accepts compression
    val acceptEncoding = request.headers
      .get("Accept-Encoding")
      .flatMap(_.headOption)
      .getOrElse("")

    acceptEncoding.contains("gzip") || acceptEncoding.contains("deflate")
  }

  /** Compress data using gzip. */
  private def compressGzip(data: Array[Byte]): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos) {
      // Set compression level
      this.`def`.setLevel(config.level)
    }
    gzos.write(data)
    gzos.close()
    baos.toByteArray
  }

  /** Compress data using deflate. */
  private def compressDeflate(data: Array[Byte]): Array[Byte] = {
    val baos     = new ByteArrayOutputStream()
    val deflater = new Deflater(config.level)
    val dos      = new DeflaterOutputStream(baos, deflater)
    dos.write(data)
    dos.close()
    baos.toByteArray
  }
}

object CompressionMiddleware {

  /** Create a compression middleware with default configuration. */
  def apply(): CompressionMiddleware =
    CompressionMiddleware(CompressionConfig())

  /** Create a compression middleware with custom level. */
  def withLevel(level: Int): CompressionMiddleware =
    CompressionMiddleware(CompressionConfig(level = level))

  /** Create a compression middleware with custom minimum size. */
  def withMinSize(minSize: Int): CompressionMiddleware =
    CompressionMiddleware(CompressionConfig(minSize = minSize))
}
