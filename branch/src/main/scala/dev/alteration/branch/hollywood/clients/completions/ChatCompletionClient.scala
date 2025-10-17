package dev.alteration.branch.hollywood.clients.completions

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.spider.common.ContentType
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.spider.client.ClientRequest.withContentType
import dev.alteration.branch.veil.Veil
import ChatCompletionsRequest.given

import java.net.URI

class ChatCompletionClient(
    completionHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      ChatCompletionClient.defaultCompletionHandler
) {

  def getCompletion(
      request: ChatCompletionsRequest
  ): ChatCompletionsResponse = {
    completionHandler(request)
  }
}

object ChatCompletionClient {

  val baseUrl: String = Veil
    .getFirst("LLAMA_SERVER_COMPLETION_URL", "LLAMA_SERVER_URL")
    .getOrElse("http://localhost:8080")

  val defaultCompletionHandler
      : ChatCompletionsRequest => ChatCompletionsResponse = { req =>
    {
      val httpRequest = ClientRequest
        .builder(URI.create(s"$baseUrl/v1/chat/completions"))
        .withContentType(ContentType.json)
        .POST(
          JsonBodyPublisher
            .of[ChatCompletionsRequest](req, removeNulls = true)
        )
        .build()

      Client.defaultClient
        .send(
          httpRequest,
          JsonBodyHandler.of[ChatCompletionsResponse]
        )
        .body()
        .get
    }
  }

}
