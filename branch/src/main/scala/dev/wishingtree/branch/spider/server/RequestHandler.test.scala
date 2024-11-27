package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpVerb
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.RequestHandler.given
import dev.wishingtree.branch.testkit.spider.server.HttpFixtureSuite

import java.net.URI
import java.net.http.HttpClient.Version
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class RequestHandlerSpec extends HttpFixtureSuite {

  case class AlohaGreeter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response("Aloha")
    }
  }

  val alohaHandler
      : PartialFunction[(HttpVerb, Segments), RequestHandler[Unit, String]] = {
    case HttpVerb.GET -> >> / "aloha" => AlohaGreeter()
  }
  httpFixture(alohaHandler).test("RequestHandler") { server =>

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
