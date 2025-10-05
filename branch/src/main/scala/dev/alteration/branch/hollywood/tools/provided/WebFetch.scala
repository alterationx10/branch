package dev.alteration.branch.hollywood.tools.provided

import dev.alteration.branch.hollywood.tools.CallableTool
import dev.alteration.branch.spider.client.ClientRequest.uri
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.hollywood.tools.schema
import dev.alteration.branch.hollywood.tools.schema.Param

import java.net.http.HttpResponse.BodyHandlers

@schema.Tool("Fetch a webpage by url")
case class WebFetch(
    @Param("A valid url") url: String
) extends CallableTool[String] {

  override def execute(): String = {
    Client.defaultClient
      .send(
        ClientRequest.builder(uri"$url").GET().build(),
        BodyHandlers.ofString()
      )
      .body()
  }

}
