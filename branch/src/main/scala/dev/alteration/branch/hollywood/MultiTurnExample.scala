package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.tools.{
  schema,
  CallableTool,
  ToolRegistry
}
import dev.alteration.branch.hollywood.tools.ToolRegistry.register
import dev.alteration.branch.friday.JsonCodec

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.util.Random

@schema.Tool("Calculate factorial of a number")
case class FactorialTool(
    @Param("The number to calculate factorial for") n: Int
) extends CallableTool[Long] {

  override def execute(): Long = {
    if (n < 0)
      throw new IllegalArgumentException(
        "Factorial is not defined for negative numbers"
      )
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

  val toolRegistry = ToolRegistry()
  toolRegistry.register[FactorialTool]
  toolRegistry.register[RandomNumberTool]
  toolRegistry.register[PrimeCheckTool]

  val client = HttpClient.newHttpClient()

  // Keep track of conversation messages
  var conversationMessages = List(
    ChatMessage(
      role = "user",
      content = Some(
        "Using the tools you have: 1. get a random number 2. get the factorial of it 3. check if the factorial is a prime number. Repeat until it is prime."
      )
    )
  )

  def makeApiRequest(messages: List[ChatMessage]): ChatCompletionsResponse = {
    val requestBody = ChatCompletionsRequest(
      messages = messages,
      tools = Some(toolRegistry.getTools),
      model = "gpt-oss"
    )

    val request = HttpRequest
      .newBuilder()
      .uri(URI.create("http://localhost:8080/v1/chat/completions"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJsonString))
      .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    JsonCodec.derived[ChatCompletionsResponse].decode(response.body()).get
  }

  def executeToolCall(toolCall: ToolCall): String = {
    try {
      val args   = toolCall.function.argumentMap
      val result = toolRegistry.execute(toolCall.function.name, args)
      result match {
        case Some(value) =>
          s"Tool ${toolCall.function.name} executed successfully. Result: $value"
        case None        => s"Tool ${toolCall.function.name} not found"
      }
    } catch {
      case e: Exception =>
        s"Error executing tool ${toolCall.function.name}: ${e.getMessage}"
    }
  }

  // Main conversation loop
  var currentTurn          = 0
  var continueConversation = true

  while (continueConversation) {
    currentTurn += 1
    println(s"\n=== Turn $currentTurn ===")

    // Make API request
    val response = makeApiRequest(conversationMessages)
    val choice   = response.choices.head

    // Get the first choice's message
    val assistantMessage = choice.message.getOrElse(
      ChatMessage(role = "assistant", content = Some("No response"))
    )

    // Add assistant's response to conversation
    conversationMessages = conversationMessages :+ assistantMessage

    println(
      s"Assistant: ${assistantMessage.content.getOrElse("No text content")}"
    )
    println(s"Finish reason: ${choice.finish_reason}")

    // Check finish reason to determine if we should continue
    choice.finish_reason match {
      case Some("tool_calls") =>
        assistantMessage.tool_calls match {
          case Some(toolCalls) =>
            println(s"Executing ${toolCalls.length} tool call(s)...")

            val toolResults = toolCalls.map { toolCall =>
              println(
                s"Calling tool: ${toolCall.function.name} with args: ${toolCall.function.arguments}"
              )
              val result = executeToolCall(toolCall)
              println(s"Tool result: $result")

              ChatMessage(
                role = "tool",
                content = Some(result),
                tool_call_id = Some(toolCall.id)
              )
            }

            // Add tool results to conversation
            conversationMessages = conversationMessages ++ toolResults

          case None =>
            println(
              "Finish reason was tool_calls but no tool calls found. Ending conversation."
            )
            continueConversation = false
        }

      case Some("stop") =>
        println("Model finished. Conversation complete.")
        continueConversation = false

      case other =>
        println(s"Unexpected finish reason: $other. Ending conversation.")
        continueConversation = false
    }
  }

}
