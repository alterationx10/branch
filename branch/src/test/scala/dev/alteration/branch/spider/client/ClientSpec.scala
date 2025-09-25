package dev.alteration.branch.spider.client

import dev.alteration.branch.testkit.fixtures.HttpFixtureSuite
import ClientRequest.uri
import dev.alteration.branch.friday.JsonEncoder
import dev.alteration.branch.friday.http.JsonBodyHandler
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.HttpMethod
import dev.alteration.branch.macaroni.extensions.StringContextExtensions.*
import dev.alteration.branch.spider.server.{Request, RequestHandler, Response}
import munit.FunSuite

class ClientSpec extends HttpFixtureSuite {

  case class Person(name: String)

  given Conversion[Person, Array[Byte]] = { person =>
    summon[JsonEncoder[Person]].encode(person).toJsonString.getBytes
  }

  case class PersonHandler(name: String) extends RequestHandler[Unit, Person] {
    override def handle(request: Request[Unit]): Response[Person] =
      Response(200, Person(name))
  }

  val personHandler: PartialFunction[(HttpMethod, List[String]), RequestHandler[
    Unit,
    Person
  ]] = { case HttpMethod.GET -> (ci"person" :: s"$name" :: Nil) =>
    PersonHandler(name)
  }

  httpFixture(personHandler).test("Client") { server =>
    val client = Client.build()

    val request = ClientRequest
      .build(uri"http://localhost:${server.getAddress.getPort}/PerSoN/Mark")

    val response = client.send(request, JsonBodyHandler.of[Person])

    assertEquals(response.body().get.name, "Mark")
  }

}
