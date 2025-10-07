package dev.alteration.branch.hollywood.tools.provided.http

import dev.alteration.branch.friday.Json
import dev.alteration.branch.hollywood.tools.{schema, CallableTool}
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.spider.client.ClientRequest.uri
import dev.alteration.branch.spider.client.{Client, ClientRequest}

import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import scala.util.Try

@schema.Tool("Make HTTP requests to any API endpoint")
case class HttpClientTool(
    @Param("The URL to request") url: String,
    @Param(
      "HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)"
    ) method: String = "GET",
    @Param("Optional request headers as JSON object") headers: Option[String] =
      None,
    @Param("Optional request body as string") body: Option[String] = None
) extends CallableTool[String] {

  override def execute(): Try[String] = Try {
    val builder = ClientRequest.builder(uri"$url")

    // Add headers if provided
    headers.foreach { headerJson =>
      Json.parse(headerJson).toOption match {
        case Some(Json.JsonObject(fields)) =>
          fields.foreach {
            case (key, Json.JsonString(value)) => builder.setHeader(key, value)
            case _                             => // Ignore non-string values
          }
        case _                             => // Invalid JSON, skip
      }
    }

    // Set HTTP method and body
    val requestBuilder = method.toUpperCase match {
      case "GET"     => builder.GET()
      case "POST"    =>
        builder.POST(BodyPublishers.ofString(body.getOrElse("")))
      case "PUT"     =>
        builder.PUT(BodyPublishers.ofString(body.getOrElse("")))
      case "DELETE"  => builder.DELETE()
      case "PATCH"   =>
        builder.method(
          "PATCH",
          BodyPublishers.ofString(body.getOrElse(""))
        )
      case "HEAD"    => builder.method("HEAD", BodyPublishers.noBody())
      case "OPTIONS" => builder.method("OPTIONS", BodyPublishers.noBody())
      case _         => builder.GET() // Default to GET
    }

    val request = requestBuilder.build()

    Client.defaultClient
      .send(request, BodyHandlers.ofString())
      .body()
  }

}
