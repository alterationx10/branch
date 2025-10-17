package dev.alteration.branch.spider.webview.http

import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.common.ContentType
import scala.io.Source
import scala.util.{Try, Using}

/** An HTTP handler that serves static files from the classpath.
  *
  * This is useful for serving JavaScript, CSS, images, and other static assets
  * bundled in your resources directory.
  *
  * Example:
  * {{{
  * // Serve files from src/main/resources/static/
  * val resourceServer = ResourceServer("/static")
  *
  * // When client requests "/js/app.js", it will load:
  * // - src/main/resources/static/js/app.js
  * }}}
  *
  * @param resourceBasePath
  *   The base path in the resources directory (without leading slash in
  *   resources)
  * @param stripPrefix
  *   Optional prefix to strip from request paths (e.g., "/static")
  */
class ResourceServer(
    resourceBasePath: String,
    stripPrefix: Option[String] = None
) extends RequestHandler[Unit, String] {

  override def handle(request: Request[Unit]): Response[String] = {
    val path = request.uri.getPath

    // Strip prefix if configured
    val actualPath = stripPrefix match {
      case Some(prefix) if path.startsWith(prefix) =>
        path.substring(prefix.length)
      case _                                       => path
    }

    // Build resource path (remove leading slash)
    val resourcePath = s"$resourceBasePath${actualPath}".stripPrefix("/")

    // Try to load the resource
    Try {
      val stream = getClass.getClassLoader.getResourceAsStream(resourcePath)
      if (stream == null) {
        throw new RuntimeException(s"Resource not found: $resourcePath")
      }
      stream
    }.flatMap { stream =>
      Using(Source.fromInputStream(stream, "UTF-8")) { source =>
        source.mkString
      }
    } match {
      case scala.util.Success(content) =>
        // Determine content type from file extension
        val contentType = getContentType(actualPath)
        Response(200, content, Map("Content-Type" -> List(contentType)))

      case scala.util.Failure(_) =>
        Response(
          404,
          s"Resource not found: $actualPath",
          Map(ContentType.txt.toHeader)
        )
    }
  }

  /** Get the content type based on file extension.
    *
    * @param path
    *   The file path
    * @return
    *   The content type
    */
  private def getContentType(path: String): String = {
    val extension = path.split("\\.").lastOption.map(_.toLowerCase)
    extension match {
      case Some("html")               => "text/html; charset=utf-8"
      case Some("css")                => "text/css; charset=utf-8"
      case Some("js")                 => "application/javascript; charset=utf-8"
      case Some("json")               => "application/json; charset=utf-8"
      case Some("png")                => "image/png"
      case Some("jpg") | Some("jpeg") => "image/jpeg"
      case Some("gif")                => "image/gif"
      case Some("svg")                => "image/svg+xml"
      case Some("ico")                => "image/x-icon"
      case Some("woff")               => "font/woff"
      case Some("woff2")              => "font/woff2"
      case Some("ttf")                => "font/ttf"
      case Some("eot")                => "application/vnd.ms-fontobject"
      case _                          => "application/octet-stream"
    }
  }
}

object ResourceServer {

  /** Create a resource server with a base path.
    *
    * @param resourceBasePath
    *   The base path in resources (e.g., "static" for
    *   src/main/resources/static/)
    * @param stripPrefix
    *   Optional prefix to strip from request paths
    * @return
    *   A new ResourceServer instance
    */
  def apply(
      resourceBasePath: String,
      stripPrefix: Option[String] = None
  ): ResourceServer = {
    new ResourceServer(resourceBasePath, stripPrefix)
  }
}
