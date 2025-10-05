package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.friday.http.JsonBodyHandler
import dev.alteration.branch.hollywood.tools.CallableTool
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.hollywood.tools.schema
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.spider.client.ClientRequest.uri
import dev.alteration.branch.veil.Veil

import scala.util.Try

@schema.Tool("Search the web using SearXNG")
case class SearXNGTool(
    @Param("search query (required)") q: String,
    @Param(
      "comma-separated list of categories (e.g., 'general', 'news', 'images', 'videos', 'science')"
    ) categories: Option[String] = None,
    @Param("comma-separated list of specific engines") engines: Option[String] =
      None,
    @Param("language code (e.g., 'en', 'es', 'fr')") language: Option[String] =
      Some("en"),
    @Param("search page number (default: 1)") pageno: Option[Int] = None,
    @Param("time range filter: 'day', 'month', or 'year'") time_range: Option[
      String
    ] = None,
    @Param(
      "safesearch level: 0 (off), 1 (moderate), or 2 (strict)"
    ) safesearch: Option[Int] = Some(0),
    @Param(
      "maximum number of search results to return. Defaults to 10"
    ) max_results: Option[Int] = None
) extends CallableTool[SearXNGResponse] {

  override def execute(): Try[SearXNGResponse] = {
    // Build query parameters
    val params = List(
      Some(s"q=${java.net.URLEncoder.encode(q, "UTF-8")}"),
      Some("format=json"),
      categories
        .filter(_.nonEmpty)
        .map(c => s"categories=${java.net.URLEncoder.encode(c, "UTF-8")}"),
      engines
        .filter(_.nonEmpty)
        .map(e => s"engines=${java.net.URLEncoder.encode(e, "UTF-8")}"),
      language
        .filter(_.nonEmpty)
        .map(l => s"language=${java.net.URLEncoder.encode(l, "UTF-8")}"),
      time_range
        .filter(_.nonEmpty)
        .map(t => s"time_range=${java.net.URLEncoder.encode(t, "UTF-8")}"),
      pageno
        .filter(_ > 0)
        .map(p => s"pageno=$p"),
      safesearch
        .filter(s => s >= 0 && s <= 2)
        .map(s => s"safesearch=$s")
    ).flatten.mkString("&")

    val baseUrl: String =
      Veil
        .get("SEARXNG_URL")
        .getOrElse("http://localhost:8888")

    val httpRequest = ClientRequest
      .builder(uri"$baseUrl/search?$params")
      .GET()
      .build()

    Try {
      Client.defaultClient
        .send(httpRequest, JsonBodyHandler.of[SearXNGResponse])
        .body()
        .map(res =>
          res.copy(results = res.results.take(max_results.getOrElse(10)))
        )
        .get
    }
  }
}
