package dev.alteration.branch.hollywood.tools.provided

import dev.alteration.branch.hollywood.tools.CallableTool
import dev.alteration.branch.spider.client.ClientRequest.uri
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.hollywood.tools.schema
import dev.alteration.branch.hollywood.tools.schema.Param

import java.net.http.HttpResponse.BodyHandlers

@schema.Tool("Fetch a webpage by url")
case class UrlGetTool(
    @Param("A valid url") url: String
) extends CallableTool[String] {

  override def execute(): String = {
    println(s"Calling tool with $url")
    val result = Client.defaultClient
      .send(
        ClientRequest.builder(uri"$url").GET().build(),
        BodyHandlers.ofString()
      )
      .body()
    println(s"Got response $result")
    result
  }

}
