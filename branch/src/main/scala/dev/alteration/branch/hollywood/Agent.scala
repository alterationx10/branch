package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*

case class AgentConfig(
  maxTurns: Int = 50,
  model: String = "gpt-oss",
  onTurn: Option[(Int, ChatMessage) => Unit] = None
)

trait Agent {
  def chat(message: String): String
}

class ConversationalAgent(
  requestHandler: ChatCompletionsRequest => ChatCompletionsResponse,
  config: AgentConfig = AgentConfig()
) extends Agent {

  private var conversationMessages: List[ChatMessage] = List.empty

  def chat(message: String): String = {
    // Add user message to conversation
    val userMessage = ChatMessage(role = "user", content = Some(message))
    conversationMessages = conversationMessages :+ userMessage

    // Run multi-turn loop
    var currentTurn = 0
    var continueConversation = true
    var finalResponse = ""

    while (continueConversation && currentTurn < config.maxTurns) {
      currentTurn += 1

      // Make API request with full conversation history
      val request = ChatCompletionsRequest(
        messages = conversationMessages,
        tools = Some(tools.ToolRegistry.getTools),
        model = config.model
      )

      val response = requestHandler(request)
      val choice = response.choices.head

      // Get assistant's message
      val assistantMessage = choice.message.getOrElse(
        ChatMessage(role = "assistant", content = Some("No response"))
      )

      // Add to conversation
      conversationMessages = conversationMessages :+ assistantMessage

      // Callback for observability
      config.onTurn.foreach(_(currentTurn, assistantMessage))

      // Handle finish reason
      choice.finish_reason match {
        case Some("tool_calls") =>
          assistantMessage.tool_calls match {
            case Some(toolCalls) =>
              val toolResults = toolCalls.map { toolCall =>
                val result = try {
                  val args = toolCall.function.argumentMap
                  tools.ToolRegistry.execute(toolCall.function.name, args) match {
                    case Some(value) => s"Tool ${toolCall.function.name} executed successfully. Result: $value"
                    case None => s"Tool ${toolCall.function.name} not found"
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
              finalResponse = assistantMessage.content.getOrElse("Error: tool_calls finish reason but no tools")
              continueConversation = false
          }

        case Some("stop") =>
          finalResponse = assistantMessage.content.getOrElse("")
          continueConversation = false

        case other =>
          finalResponse = s"Unexpected finish reason: $other"
          continueConversation = false
      }
    }

    if (currentTurn >= config.maxTurns && continueConversation) {
      finalResponse = s"Max turns ($config.maxTurns) reached. Last response: ${conversationMessages.lastOption.flatMap(_.content).getOrElse("")}"
    }

    finalResponse
  }
}
