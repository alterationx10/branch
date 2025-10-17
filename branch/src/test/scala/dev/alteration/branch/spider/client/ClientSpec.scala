package dev.alteration.branch.spider.client

import dev.alteration.branch.testkit.fixtures.HttpFixtureSuite
import ClientRequest.uri
import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBody}
import dev.alteration.branch.friday.http.JsonConversions.*
import dev.alteration.branch.friday.http.JsonBody.given
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.macaroni.extensions.StringContextExtensions.*
import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import munit.FunSuite

class ClientSpec extends HttpFixtureSuite {

  case class Person(name: String)

  case class PersonHandler(name: String) extends RequestHandler[Unit, JsonBody[Person]] {
    override def handle(request: Request[Unit]): Response[JsonBody[Person]] =
      Response(200, Person(name)).jsonBody()
  }

  val personHandler: PartialFunction[(HttpMethod, List[String]), RequestHandler[
    Unit,
    JsonBody[Person]
  ]] = { case HttpMethod.GET -> (ci"person" :: s"$name" :: Nil) =>
    PersonHandler(name)
  }

  httpFixture(personHandler).test("Client") { server =>
    val client = Client.build()

    val request = ClientRequest
      .build(uri"http://localhost:${server.port}/PerSoN/Mark")

    val response = client.send(request, JsonBodyHandler.of[Person])

    assertEquals(response.body().get.name, "Mark")
  }

}
