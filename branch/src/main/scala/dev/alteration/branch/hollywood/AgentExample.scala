package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.ChatCompletionsRequest.derived$JsonCodec
import dev.alteration.branch.hollywood.api.{ChatCompletionsRequest, ChatCompletionsResponse}
import dev.alteration.branch.friday.{JsonCodec, JsonDecoder}
import dev.alteration.branch.hollywood.tools.ToolRegistry

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.io.StdIn

object AgentExample extends App {

  ToolRegistry.register[FactorialTool]
  ToolRegistry.register[RandomNumberTool]
  ToolRegistry.register[PrimeCheckTool]
  
  val client = HttpClient.newHttpClient()

  val handler: ChatCompletionsRequest => ChatCompletionsResponse = { req =>
    {
      val jsonBody    = req.toJson.toString
      val httpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:8080/v1/chat/completions"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build()
      val response    =
        client.send(httpRequest, HttpResponse.BodyHandlers.ofString())

      JsonCodec.derived[ChatCompletionsResponse].decode(response.body()).get
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
