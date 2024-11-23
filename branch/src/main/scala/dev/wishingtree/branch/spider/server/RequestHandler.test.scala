package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpVerb
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.RequestHandler.given
import dev.wishingtree.branch.spider.server.{
  ContextHandler,
  Request,
  RequestHandler,
  Response
}

import java.net.URI
import java.net.http.HttpClient.Version
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class RequestHandlerSpec extends HttpFunSuite {

  case class AlohaGreeter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response("Aloha")
    }
  }

  contextFixture.test("RequestHandler") { (server, fn) =>

    val testHandler = fn { case HttpVerb.GET -> >> / "aloha" =>
      AlohaGreeter()
    }

    ContextHandler.registerHandler(testHandler)(using server)

    val client = HttpClient.newBuilder
      .version(Version.HTTP_1_1)
      .build

    val response = client.send(
      HttpRequest.newBuilder
        .uri(URI.create(s"http://localhost:${server.getAddress.getPort}/aloha"))
        .build,
      HttpResponse.BodyHandlers.ofString
    )

    client.close()

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Aloha")
  }

}
