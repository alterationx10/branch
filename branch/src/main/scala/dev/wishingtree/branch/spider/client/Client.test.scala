package dev.wishingtree.branch.spider.client

import dev.wishingtree.branch.friday.JsonEncoder
import dev.wishingtree.branch.friday.http.JsonBodyHandler
import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.spider.client.ClientRequest.uri
import dev.wishingtree.branch.spider.server.{Request, RequestHandler, Response}
import dev.wishingtree.branch.spider.server.OpaqueSegments.*
import dev.wishingtree.branch.spider.server.RequestHandler.given
import dev.wishingtree.branch.testkit.fixtures.HttpFixtureSuite
import munit.FunSuite

class ClientSpec extends HttpFixtureSuite {

  case class Person(name: String)

  given Conversion[Person, Array[Byte]] = { person =>
    summon[JsonEncoder[Person]].encode(person).toJsonString.getBytes
  }

  case class PersonHandler(name: String) extends RequestHandler[Unit, Person] {
    override def handle(request: Request[Unit]): Response[Person] =
      Response(Person(name))
  }

  val personHandler: PartialFunction[(HttpMethod, Segments), RequestHandler[
    Unit,
    Person
  ]] = { case HttpMethod.GET -> >> / ci"person" / s"$name" =>
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
