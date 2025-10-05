package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.http.JsonBodyHandler
import dev.alteration.branch.hollywood.tools.CallableTool
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.hollywood.tools.schema
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.spider.client.ClientRequest.uri


@schema.Tool("Search the web")
case class SearXNGTool(
    @Param("search query") searchRequest: SearXNGRequest
) extends CallableTool[SearXNGResponse] {

  override def execute(): SearXNGResponse = {
    // Build query parameters
    val params = List(
      Some(s"q=${java.net.URLEncoder.encode(searchRequest.q, "UTF-8")}"),
      Some("format=json"),
      searchRequest.categories.map(c => s"categories=${java.net.URLEncoder.encode(c, "UTF-8")}"),
      searchRequest.engines.map(e => s"engines=${java.net.URLEncoder.encode(e, "UTF-8")}"),
      searchRequest.language.map(l => s"language=${java.net.URLEncoder.encode(l, "UTF-8")}"),
      searchRequest.time_range.map(t => s"time_range=${java.net.URLEncoder.encode(t, "UTF-8")}"),
      searchRequest.pageno.map(p => s"pageno=$p"),
      searchRequest.safesearch.map(s => s"safesearch=$s")
    ).flatten.mkString("&")

    val httpRequest = ClientRequest
      .builder(uri"http://localhost:8888/search?$params")
      .GET()
      .build()

    Client.defaultClient
      .send(httpRequest, JsonBodyHandler.of[SearXNGResponse])
      .body()
      .get
  }
}
