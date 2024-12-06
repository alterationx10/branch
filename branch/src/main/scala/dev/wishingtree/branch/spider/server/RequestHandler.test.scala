package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.friday.http.JsonBodyHandler
import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.RequestHandler.given
import dev.wishingtree.branch.spider.server.Response.*
import dev.wishingtree.branch.testkit.fixtures.HttpFixtureSuite

import java.net.URI
import java.net.http.HttpClient.Version
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

class RequestHandlerSpec extends HttpFixtureSuite {

  case class AlohaGreeter() extends RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      Response("Aloha")
    }
  }

  val jsonHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      json"""
            {
              "message": "Aloha, World!"
            }
            """
    }
  }

  val textHandler = new RequestHandler[Unit, String] {
    override def handle(request: Request[Unit]): Response[String] = {
      html"""
            I am a text/html response.
       """
    }
  }

  val alohaHandler: PartialFunction[(HttpMethod, Segments), RequestHandler[
    Unit,
    String
  ]] = {
    case HttpMethod.GET -> >> / "aloha"  => AlohaGreeter()
    case HttpMethod.GET -> >> / "jaloha" => jsonHandler
    case HttpMethod.GET -> >> / "txt"    => textHandler
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

    case class Message(message: String)
    val response2 = client.send(
      HttpRequest.newBuilder
        .uri(
          URI.create(s"http://localhost:${server.getAddress.getPort}/jaloha")
        )
        .build,
      JsonBodyHandler.of[Message]
    )

    val response3 = client.send(
      HttpRequest.newBuilder
        .uri(URI.create(s"http://localhost:${server.getAddress.getPort}/txt"))
        .build,
      HttpResponse.BodyHandlers.ofString
    )

    client.close()

    assertEquals(response.statusCode, 200)
    assertEquals(response.body, "Aloha")

    assertEquals(response2.statusCode, 200)
    assertEquals(response2.body().get, Message("Aloha, World!"))

    assertEquals(response3.statusCode, 200)
    assertEquals(
      response3.body,
      "I am a text/html response."
    )

  }

}
