package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolRegistry, schema}
import dev.alteration.branch.hollywood.api.ChatCompletionsResponse.derived$JsonCodec
import dev.alteration.branch.hollywood.tools.schema.Param

import java.net.http.{HttpRequest, HttpResponse}
import java.net.URI
import java.net.http.HttpClient

@schema.Tool("Integer addition")
case class AdditionTool(
    @Param("an Int") a: Int,
    @Param("an Int") b: Int
) extends CallableTool[Int] {

  override def execute(): Int = a + b
}

object Example extends App {

  ToolRegistry.register[AdditionTool]

  val client = HttpClient.newHttpClient()

  // Build a minimal chat completions request
  val requestBody = ChatCompletionsRequest(
    messages = List(
      ChatMessage(
        role = "user",
        content = Some("Hello!")
      )
    ),
    model = "gpt-oss"
  )

  // Encode the request to JSON
  val jsonBody = requestBody.toJson.toString

  // Create the HTTP POST request
  val httpRequest = HttpRequest
    .newBuilder()
    .uri(URI.create("http://localhost:8080/v1/chat/completions"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .build()

  // Send the request and print the response
  val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
  println(s"Status: ${response.statusCode()}")
  println(s"Body: ${response.body()}")

  val resp = response.body().decodeAs
  println(s"Decoded response: $resp")
  println(resp.get.choices.head.message.get.content.get)

}
