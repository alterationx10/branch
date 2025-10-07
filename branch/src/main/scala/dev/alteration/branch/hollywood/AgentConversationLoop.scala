package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.clients.completions.{
  ChatCompletionsRequest,
  ChatMessage,
  CompletionClient
}
import dev.alteration.branch.hollywood.tools.ToolRegistry

/** Shared conversation loop logic used by agent implementations */
private[hollywood] object AgentConversationLoop {

  /** Runs the multi-turn conversation loop
    * @param messages
    *   The current conversation messages
    * @param requestHandler
    *   Function to make chat completion requests
    * @param toolRegistry
    *   Optional tool registry for tool execution
    * @param maxTurns
    *   Maximum number of conversation turns
    * @param model
    *   Model to use for chat completions
    * @param onTurn
    *   Optional callback for each turn
    * @return
    *   Tuple of (final response, updated conversation messages)
    */
  def run(
      messages: List[ChatMessage],
      completionClient: CompletionClient,
      toolRegistry: Option[ToolRegistry],
      maxTurns: Int,
      model: String,
      onTurn: Option[(Int, ChatMessage) => Unit]
  ): (String, List[ChatMessage]) = {
    var conversationMessages = messages
    var currentTurn          = 0
    var continueConversation = true
    var finalResponse        = ""

    while (continueConversation && currentTurn < maxTurns) {
      currentTurn += 1

      // Make API request with full conversation history
      val request = ChatCompletionsRequest(
        messages = conversationMessages,
        tools = toolRegistry.map(_.getTools),
        model = model
      )

      val response = completionClient.getCompletion(request)
      val choice   = response.choices.head

      // Get assistant's message
      val assistantMessage = choice.message.getOrElse(
        ChatMessage(role = "assistant", content = Some("No response"))
      )

      // Add to conversation
      conversationMessages = conversationMessages :+ assistantMessage

      // Callback for observability
      onTurn.foreach(_(currentTurn, assistantMessage))

      // Handle finish reason
      choice.finish_reason match {
        case Some("tool_calls") =>
          assistantMessage.tool_calls match {
            case Some(toolCalls) =>
              val toolResults = toolCalls.map { toolCall =>
                val result =
                  try {
                    val args = toolCall.function.argumentsJson
                    toolRegistry.flatMap(
                      _.execute(toolCall.function.name, args)
                    ) match {
                      case Some(jsonResult) =>
                        // Convert Json result to string for the LLM
                        s"Tool ${toolCall.function.name} executed successfully. Result: ${jsonResult.toJsonString}"
                      case None             => s"Tool ${toolCall.function.name} not found"
                    }
                  } catch {
                    case e: Exception =>
                      s"Error executing tool ${toolCall.function.name}: ${e.getMessage}"
                  }

                ChatMessage(
                  role = "tool",
                  content = Some(result),
                  tool_call_id = Some(toolCall.id)
                )
              }
              conversationMessages = conversationMessages ++ toolResults

            case None =>
              finalResponse = assistantMessage.content
                .getOrElse(
                  "Error: tool_calls finish reason but no tools"
                )
                .translateEscapes()
              continueConversation = false
          }

        case Some("stop") =>
          finalResponse =
            assistantMessage.content.getOrElse("").translateEscapes()
          continueConversation = false

        case other =>
          finalResponse = s"Unexpected finish reason: $other"
          continueConversation = false
      }
    }

    if (currentTurn >= maxTurns && continueConversation) {
      finalResponse =
        s"Max turns ($maxTurns) reached. Last response: ${conversationMessages.lastOption.flatMap(_.content).getOrElse("")}"
          .translateEscapes()
    }

    (finalResponse, conversationMessages)
  }
}
