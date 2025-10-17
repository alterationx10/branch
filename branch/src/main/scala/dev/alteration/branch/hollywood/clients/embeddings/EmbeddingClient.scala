package dev.alteration.branch.hollywood.clients.embeddings

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.spider.common.ContentType
import dev.alteration.branch.spider.client.ClientRequest.withContentType
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.veil.Veil

import java.net.URI

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

  val baseUrl: String =
    Veil
      .getFirst("LLAMA_SERVER_EMBEDDING_URL", "LLAMA_SERVER_URL")
      .getOrElse("http://localhost:8080")

  val defaultEmbeddingHandler: EmbeddingsRequest => EmbeddingsResponse = {
    req =>
      {
        val httpRequest = ClientRequest
          .builder(URI.create(s"$baseUrl/v1/embeddings"))
          .withContentType(ContentType.json)
          .POST(
            JsonBodyPublisher.of[EmbeddingsRequest](req, removeNulls = true)
          )
          .build()

        Client.defaultClient
          .send(
            httpRequest,
            JsonBodyHandler.of[EmbeddingsResponse]
          )
          .body()
          .get
      }
  }
}
