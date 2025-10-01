package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.hollywood.api.{
  ChatCompletionsRequest,
  ChatCompletionsResponse
}
import dev.alteration.branch.friday.JsonDecoder
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.ClientRequest
import dev.alteration.branch.spider.client.ClientRequest.*

import java.net.URI
import java.net.http.HttpClient
import scala.io.StdIn

object AgentExample extends App {

  ToolRegistry.register[FactorialTool]
  ToolRegistry.register[RandomNumberTool]
  ToolRegistry.register[PrimeCheckTool]

  import ChatCompletionsRequest.given

  val client = HttpClient.newHttpClient()

  val handler: ChatCompletionsRequest => ChatCompletionsResponse = { req =>
    {
      val httpRequest = ClientRequest
        .builder(URI.create("http://localhost:8080/v1/chat/completions"))
        .withContentType(ContentType.json)
        .POST(JsonBodyPublisher.of[ChatCompletionsRequest](req))
        .build()

      client
        .send(httpRequest, JsonBodyHandler.of[ChatCompletionsResponse])
        .body()
        .get
    }
  }

  val agent = ConversationalAgent(handler)

  var continue = true
  while (continue) {
    print("> ")
    val input = StdIn.readLine()

    if (input != null && input.trim.startsWith("quit")) {
      continue = false
      println("Goodbye!")
    } else if (input != null && input.trim.nonEmpty) {
      val response = agent.chat(input)
      println(s"Agent: $response")
    }
  }

}
