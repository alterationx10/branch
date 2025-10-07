package dev.alteration.branch.hollywood.tools.provided.http

import dev.alteration.branch.hollywood.tools.{schema, CallableTool}
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.spider.client.ClientRequest.uri
import dev.alteration.branch.spider.client.{Client, ClientRequest}

import java.net.http.HttpResponse.BodyHandlers
import scala.util.Try

@schema.Tool("Fetch a webpage by url")
case class WebFetch(
    @Param("A valid url") url: String
) extends CallableTool[String] {

  override def execute(): Try[String] = Try {
    Client.defaultClient
      .send(
        ClientRequest.builder(uri"$url").GET().build(),
        BodyHandlers.ofString()
      )
      .body()
  }

}
