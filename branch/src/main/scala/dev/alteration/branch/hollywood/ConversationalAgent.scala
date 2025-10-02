package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.hollywood.api.{
  ChatCompletionsRequest,
  ChatCompletionsResponse,
  ChatMessage
}
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.ClientRequest
import dev.alteration.branch.spider.client.ClientRequest.withContentType

import java.net.URI
import java.net.http.HttpClient

object ConversationalAgent {

  import ChatCompletionsRequest.given

  val defaultClient: HttpClient =
    HttpClient.newHttpClient()

  val defaultHandler: ChatCompletionsRequest => ChatCompletionsResponse = {
    req =>
      {
        val httpRequest = ClientRequest
          .builder(URI.create("http://localhost:8080/v1/chat/completions"))
          .withContentType(ContentType.json)
          .POST(JsonBodyPublisher.of[ChatCompletionsRequest](req))
          .build()

        defaultClient
          .send(httpRequest, JsonBodyHandler.of[ChatCompletionsResponse])
          .body()
          .get
      }
  }

}

class ConversationalAgent(
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      ConversationalAgent.defaultHandler,
    toolRegistry: Option[ToolRegistry] = None,
    maxTurns: Int = 50,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None
) extends Agent {

  private var conversationMessages: List[ChatMessage] = List.empty

  def chat(message: String): String = {
    // Add user message to conversation
    val userMessage = ChatMessage(role = "user", content = Some(message))
    conversationMessages = conversationMessages :+ userMessage

    // Run shared conversation loop
    val (response, updatedMessages) = AgentConversationLoop.run(
      conversationMessages,
      requestHandler,
      toolRegistry,
      maxTurns,
      model,
      onTurn
    )
    conversationMessages = updatedMessages

    response
  }
}
