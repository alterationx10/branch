package spider.server

import dev.alteration.branch.spider.server.{SocketServer, RequestHandler, Request, Response}
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.server.Response.*

/** A complete HTTP server example using SocketServer with routing.
  *
  * Test with:
  *   curl http://localhost:9000/hello
  *   curl http://localhost:9000/echo -d "Hello World"
  *   curl -H "Connection: close" http://localhost:9000/hello
  *   curl http://localhost:9000/notfound
  */
object SocketServerTest {

  // Handler for GET /hello
  val helloHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      html"""
      <h1>Hello from SocketServer!</h1>
      <p>This is a custom HTTP server built on raw ServerSocket.</p>
      <p>Request URI: ${request.uri}</p>
      """
    }
  }

  // Handler for POST /echo - echoes back the request body
  val echoHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      val body = new String(request.body, "UTF-8")
      json"""
      {
        "echo": "$body",
        "method": "POST",
        "uri": "${request.uri}"
      }
      """
    }
  }

  // Handler for GET /info - returns request info as JSON
  val infoHandler = new RequestHandler[Array[Byte], String] {
    override def handle(request: Request[Array[Byte]]): Response[String] = {
      val headers = request.headers.map { case (k, v) =>
        s""""$k": "${v.mkString(", ")}""""
      }.mkString(",\n    ")

      json"""
      {
        "uri": "${request.uri}",
        "headers": {
          $headers
        }
      }
      """
    }
  }

  // Router - maps (HttpMethod, path segments) to handlers
  val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case (HttpMethod.GET, "hello" :: Nil) => helloHandler
    case (HttpMethod.POST, "echo" :: Nil) => echoHandler
    case (HttpMethod.GET, "info" :: Nil)  => infoHandler
  }

  def main(args: Array[String]): Unit = {
    val server = SocketServer.withShutdownHook(
      port = 9000,
      router = router
    )

    println("SocketServer listening on port 9000")
    println()
    println("Try these commands:")
    println("  curl http://localhost:9000/hello")
    println("  curl http://localhost:9000/echo -d 'Hello World'")
    println("  curl http://localhost:9000/info")
    println("  curl http://localhost:9000/notfound")
    println()
    println("Press Ctrl+C to stop")

    server.start()
  }
}
