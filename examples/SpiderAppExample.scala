import com.sun.net.httpserver.Filter
import dev.wishingtree.branch.friday.JsonEncoder
import dev.wishingtree.branch.friday.http.JsonBodyHandler
import dev.wishingtree.branch.spider.*
import dev.wishingtree.branch.spider.client.ClientRequest.uri
import dev.wishingtree.branch.spider.client.{Client, ClientRequest}
import dev.wishingtree.branch.spider.server.*
import dev.wishingtree.branch.spider.server.OpaqueSegments.*

import java.net.http.HttpResponse

object SpiderAppExample extends SpiderApp {

  import RequestHandler.given

  val staticFilesPath = Segments.wd / "site" / "book"
  val files           = FileContext(staticFilesPath)

  case class Person(name: String)

  given Conversion[Person, Array[Byte]] = { person =>
    summon[JsonEncoder[Person]].encode(person).toJsonString.getBytes
  }

  val personHandler = new ContextHandler("/") {
    override val contextRouter: PartialFunction[
      (HttpVerb, Segments),
      RequestHandler[Unit, Person]
    ] = { case HttpVerb.GET -> >> / "person" =>
      new RequestHandler[Unit, Person] {
        override def handle(request: Request[Unit]): Response[Person] =
          Response(Person("Mark"))
      }
    }
  }

  val handlers: ContextHandler = files |+| personHandler
  ContextHandler.registerHandler(handlers)

  val client = Client.build()

  val request = ClientRequest
    .build(uri"http://localhost:9000/person")

  val response =
    client.sendAsync(request, JsonBodyHandler.of[Person])

  response
    .thenApply { resp =>
      println(resp.body())
    }

}
