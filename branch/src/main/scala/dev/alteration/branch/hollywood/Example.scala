package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.Json
import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolRegistry, schema}
import dev.alteration.branch.hollywood.api.ChatCompletionsResponse.derived$JsonCodec
import dev.alteration.branch.hollywood.tools.schema.Param

import java.net.http.{HttpRequest, HttpResponse}
import java.net.URI
import java.net.http.HttpClient

@schema.Tool("Integer addition")
case class AdditionTool(
    @Param("a string represenation of a number") a: Int,
    @Param("a string represetnation of a number") b: Int
) extends CallableTool[Int] {

  override def execute(): Int = a + b
}

object Example extends App {

  ToolRegistry.register[AdditionTool]

  val client = HttpClient.newHttpClient()

  // Build a minimal chat completions request with tools
  val requestBody = ChatCompletionsRequest(
    messages = List(
      ChatMessage(
        role = "user",
        content = Some("I need you to use the addition tool to calculate two random numbers")
      )
    ),
    model = "gpt-oss",
    tools = Some(ToolRegistry.getTools)
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

  val choice = resp.get.choices.head
  val message = choice.message.get

  message.content match {
    case Some(content) =>
      println(s"Response: $content")
    case None =>
      message.tool_calls match {
        case Some(toolCalls) =>
          println("Tool calls made:")
          toolCalls.foreach { toolCall =>
            println(s"  Tool: ${toolCall.function.name}")
            println(s"  Arguments: ${toolCall.function.arguments.translateEscapes()}")

            // Execute the tool if it's registered
            // Get arguments using the helper method
            val argsMap = Json.parse(toolCall.function.arguments.translateEscapes()).map(_.objVal.map{case (k,v) => k -> v.toString}).getOrElse(Map.empty)
            println(s"  Parsed args map: $argsMap")

            ToolRegistry.execute(toolCall.function.name, argsMap) match {
              case Some(result) => println(s"  Result: $result")
              case None => println(s"  Tool not found in registry")
            }
          }
        case None =>
          println("No content or tool calls in response")
      }
  }

}
