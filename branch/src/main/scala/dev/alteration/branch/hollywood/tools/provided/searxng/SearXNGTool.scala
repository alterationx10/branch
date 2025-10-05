package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.http.JsonBodyHandler
import dev.alteration.branch.hollywood.tools.CallableTool
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.hollywood.tools.schema
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.spider.client.ClientRequest.uri


@schema.Tool("Search the web using SearXNG")
case class SearXNGTool(
    @Param("search query") q: String,
    @Param("comma-separated list of categories") categories: String = "",
    @Param("comma-separated list of search engines") engines: String = "",
    @Param("language code like 'en' or 'fr'") language: String = "en",
    @Param("time filter: 'day', 'month', or 'year'") time_range: String = "",
    @Param("page number") pageno: Int = 1,
    @Param("safe search level: 0 (off), 1 (moderate), or 2 (strict)") safesearch: Int = 0
) extends CallableTool[SearXNGResponse] {

  override def execute(): SearXNGResponse = {
    // Build query parameters
    val params = List(
      Some(s"q=${java.net.URLEncoder.encode(q, "UTF-8")}"),
      Some("format=json"),
      Option(categories).filter(_.nonEmpty).map(c => s"categories=${java.net.URLEncoder.encode(c, "UTF-8")}"),
      Option(engines).filter(_.nonEmpty).map(e => s"engines=${java.net.URLEncoder.encode(e, "UTF-8")}"),
      Option(language).filter(_.nonEmpty).map(l => s"language=${java.net.URLEncoder.encode(l, "UTF-8")}"),
      Option(time_range).filter(_.nonEmpty).map(t => s"time_range=${java.net.URLEncoder.encode(t, "UTF-8")}"),
      Option(pageno).filter(_ > 0).map(p => s"pageno=$p"),
      Option(safesearch).filter(s => s >= 0 && s <= 2).map(s => s"safesearch=$s")
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
