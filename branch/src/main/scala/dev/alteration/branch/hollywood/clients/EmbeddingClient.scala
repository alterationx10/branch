package dev.alteration.branch.hollywood.clients

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.ClientRequest
import dev.alteration.branch.spider.client.ClientRequest.withContentType

import java.net.URI
import java.net.http.HttpClient

class EmbeddingClient(
    embeddingHandler: EmbeddingsRequest => EmbeddingsResponse =
      EmbeddingClient.defaultEmbeddingHandler,
    embeddingModel: String = "gpt-oss"
) {

  /** Get embedding for a text */
  def getEmbedding(text: String): List[Double] = {
    val request  = EmbeddingsRequest(
      input = dev.alteration.branch.friday.Json.JsonString(text),
      model = embeddingModel
    )
    val response = embeddingHandler(request)
    response.data.headOption
      .map(_.embedding)
      .getOrElse(throw new RuntimeException("Failed to get embedding"))
  }
}

object EmbeddingClient {

  val defaultClient: HttpClient =
    HttpClient.newHttpClient()

  val defaultEmbeddingHandler: EmbeddingsRequest => EmbeddingsResponse = {
    req =>
      {
        val httpRequest = ClientRequest
          .builder(URI.create("http://localhost:8080/v1/embeddings"))
          .withContentType(ContentType.json)
          .POST(JsonBodyPublisher.of[EmbeddingsRequest](req))
          .build()

        defaultClient
          .send(
            httpRequest,
            JsonBodyHandler.of[EmbeddingsResponse]
          )
          .body()
          .get
      }
  }
}
