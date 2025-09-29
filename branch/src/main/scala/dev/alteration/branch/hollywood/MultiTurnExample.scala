package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.api.ChatCompletionsResponse.derived$JsonCodec
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolRegistry, schema}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.util.Random

@schema.Tool("Calculate factorial of a number")
case class FactorialTool(
    @Param("The number to calculate factorial for") n: Int
) extends CallableTool[Long] {

  override def execute(): Long = {
    if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers")
    if (n == 0 || n == 1) 1L
    else (2L to n.toLong).product
  }
}

@schema.Tool("Generate a random number between min and max (inclusive)")
case class RandomNumberTool(
    @Param("Minimum value") min: Int,
    @Param("Maximum value") max: Int
) extends CallableTool[Int] {

  override def execute(): Int = {
    Random.between(min, max + 1)
  }
}

@schema.Tool("Check if a number is prime")
case class PrimeCheckTool(
    @Param("The number to check for primality") number: Int
) extends CallableTool[Boolean] {

  override def execute(): Boolean = {
    if (number <= 1) false
    else if (number <= 3) true
    else if (number % 2 == 0 || number % 3 == 0) false
    else {
      var i = 5
      while (i * i <= number) {
        if (number % i == 0 || number % (i + 2) == 0) return false
        i += 6
      }
      true
    }
  }
}

object MultiTurnExample extends App {

  ToolRegistry.register[FactorialTool]
  ToolRegistry.register[RandomNumberTool]
  ToolRegistry.register[PrimeCheckTool]

  val client = HttpClient.newHttpClient()

  // Keep track of conversation messages
  var conversationMessages = List(
    ChatMessage(
      role = "user",
      content = Some(
        "I want you to generate a random number between 1 and 10, then calculate its factorial, and finally tell me if the factorial result is prime. Work through this step by step."
      )
    )
  )

  var continueConversation = true
  var turnCount = 0
  val maxTurns = 10 // Safety limit

  while (continueConversation && turnCount < maxTurns) {
    turnCount += 1
    println(s"\n=== Turn $turnCount ===")

    // Build request with current conversation
    val requestBody = ChatCompletionsRequest(
      messages = conversationMessages,
      model = "gpt-oss",
      tools = Some(ToolRegistry.getTools)
    )

    val jsonBody = requestBody.toJson.toString
    println(s"Request: $jsonBody")

    val httpRequest = HttpRequest
      .newBuilder()
      .uri(URI.create("http://localhost:8080/v1/chat/completions"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()

    val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
    println(s"Status: ${response.statusCode()}")

    if (response.statusCode() != 200) {
      println(s"Error: ${response.body()}")
      continueConversation = false
    } else {
      val resp: Option[ChatCompletionsResponse] = response.body().decodeAs.toOption

      resp match {
        case Some(chatResponse) =>
          val choice = chatResponse.choices.head
          val message = choice.message.get

          // Add assistant's message to conversation
          conversationMessages = conversationMessages :+ message

          message.content match {
            case Some(content) =>
              println(s"Assistant: $content")
              // If there's content and no tool calls, conversation is likely done
              message.tool_calls match {
                case None => continueConversation = false
                case Some(_) => // Continue, there are also tool calls
              }

            case None =>
              // No content, check for tool calls
          }

          message.tool_calls match {
            case Some(toolCalls) =>
              println("Tool calls made:")
              val toolResults = toolCalls.map { toolCall =>
                println(s"  Tool: ${toolCall.function.name}")
                println(s"  Arguments: ${toolCall.function.arguments}")

                val result = ToolRegistry.execute(
                  toolCall.function.name,
                  toolCall.function.argumentMap
                ) match {
                  case Some(result) =>
                    println(s"  Result: $result")
                    result.toString
                  case None =>
                    println(s"  Tool not found in registry")
                    "Error: Tool not found"
                }

                // Create tool result message
                ChatMessage(
                  role = "tool",
                  content = Some(result),
                  tool_call_id = Some(toolCall.id)
                )
              }

              // Add tool results to conversation
              conversationMessages = conversationMessages ++ toolResults

            case None =>
              if (message.content.isEmpty) {
                println("No content or tool calls in response")
                continueConversation = false
              }
          }

        case None =>
          println("Failed to decode response")
          continueConversation = false
      }
    }
  }

  if (turnCount >= maxTurns) {
    println(s"\nReached maximum turns ($maxTurns). Conversation stopped.")
    conversationMessages.foreach(msg => println(msg.toJsonString))
  } else {
    println(s"\nConversation completed in $turnCount turns.")
  }
}