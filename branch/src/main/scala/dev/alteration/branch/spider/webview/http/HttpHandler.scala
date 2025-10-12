package dev.alteration.branch.spider.webview.http

import java.io.PrintWriter
import java.net.Socket

/** Handler for HTTP requests (non-WebSocket).
  *
  * This allows serving static files, HTML pages, and other HTTP content
  * alongside WebSocket handlers.
  */
trait HttpHandler {

  /** Handle an HTTP GET request.
    *
    * @param path
    *   The request path (e.g., "/index.html")
    * @param headers
    *   The request headers
    * @param socket
    *   The client socket for writing response
    */
  def handleGet(
      path: String,
      headers: Map[String, List[String]],
      socket: Socket
  ): Unit
}

/** Helper methods for HTTP responses. */
object HttpResponse {

  /** Send a 200 OK response with HTML content.
    *
    * @param socket
    *   The client socket
    * @param html
    *   The HTML content
    */
  def ok(socket: Socket, html: String): Unit = {
    sendResponse(socket, 200, "OK", "text/html; charset=utf-8", html)
  }

  /** Send a 200 OK response with custom content type.
    *
    * @param socket
    *   The client socket
    * @param content
    *   The response content
    * @param contentType
    *   The content type (e.g., "application/javascript")
    */
  def ok(socket: Socket, content: String, contentType: String): Unit = {
    sendResponse(socket, 200, "OK", contentType, content)
  }

  /** Send a 200 OK response with binary content.
    *
    * @param socket
    *   The client socket
    * @param bytes
    *   The response bytes
    * @param contentType
    *   The content type
    */
  def ok(socket: Socket, bytes: Array[Byte], contentType: String): Unit = {
    sendBinaryResponse(socket, 200, "OK", contentType, bytes)
  }

  /** Send a 404 Not Found response.
    *
    * @param socket
    *   The client socket
    * @param message
    *   The error message
    */
  def notFound(socket: Socket, message: String = "Not Found"): Unit = {
    sendResponse(socket, 404, "Not Found", "text/plain", message)
  }

  /** Send a 500 Internal Server Error response.
    *
    * @param socket
    *   The client socket
    * @param message
    *   The error message
    */
  def internalError(
      socket: Socket,
      message: String = "Internal Server Error"
  ): Unit = {
    sendResponse(socket, 500, "Internal Server Error", "text/plain", message)
  }

  /** Send an HTTP response with text content.
    *
    * @param socket
    *   The client socket
    * @param statusCode
    *   The HTTP status code
    * @param statusText
    *   The HTTP status text
    * @param contentType
    *   The content type
    * @param content
    *   The response content
    */
  private def sendResponse(
      socket: Socket,
      statusCode: Int,
      statusText: String,
      contentType: String,
      content: String
  ): Unit = {
    val out      = new PrintWriter(socket.getOutputStream, true)
    val response = s"""HTTP/1.1 $statusCode $statusText\r
Content-Type: $contentType\r
Content-Length: ${content.getBytes("UTF-8").length}\r
Connection: close\r
\r
$content"""
    out.print(response)
    out.flush()
  }

  /** Send an HTTP response with binary content.
    *
    * @param socket
    *   The client socket
    * @param statusCode
    *   The HTTP status code
    * @param statusText
    *   The HTTP status text
    * @param contentType
    *   The content type
    * @param bytes
    *   The response bytes
    */
  private def sendBinaryResponse(
      socket: Socket,
      statusCode: Int,
      statusText: String,
      contentType: String,
      bytes: Array[Byte]
  ): Unit = {
    val headerBytes = s"""HTTP/1.1 $statusCode $statusText\r
Content-Type: $contentType\r
Content-Length: ${bytes.length}\r
Connection: close\r
\r
""".getBytes("UTF-8")

    val out = socket.getOutputStream
    out.write(headerBytes)
    out.write(bytes)
    out.flush()
  }
}
