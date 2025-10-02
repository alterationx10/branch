package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.ToolRegistry


case class AgentConfig(
    maxTurns: Int = 50,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None
)

trait Agent {
  def chat(message: String): String
}

/** Shared conversation loop logic used by agent implementations */
private[hollywood] object AgentConversationLoop {

  /** Runs the multi-turn conversation loop
    * @param messages The current conversation messages
    * @param requestHandler Function to make chat completion requests
    * @param toolRegistry Optional tool registry for tool execution
    * @param config Agent configuration
    * @return Tuple of (final response, updated conversation messages)
    */
  def run(
      messages: List[ChatMessage],
      requestHandler: ChatCompletionsRequest => ChatCompletionsResponse,
      toolRegistry: Option[ToolRegistry],
      config: AgentConfig
  ): (String, List[ChatMessage]) = {
    var conversationMessages = messages
    var currentTurn          = 0
    var continueConversation = true
    var finalResponse        = ""

    while (continueConversation && currentTurn < config.maxTurns) {
      currentTurn += 1

      // Make API request with full conversation history
      val request = ChatCompletionsRequest(
        messages = conversationMessages,
        tools = toolRegistry.map(_.getTools),
        model = config.model
      )

      val response = requestHandler(request)
      val choice   = response.choices.head

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
                val result =
                  try {
                    val args = toolCall.function.argumentMap
                    toolRegistry.flatMap(
                      _.execute(toolCall.function.name, args)
                    ) match {
                      case Some(value) =>
                        s"Tool ${toolCall.function.name} executed successfully. Result: $value"
                      case None        => s"Tool ${toolCall.function.name} not found"
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
              finalResponse = assistantMessage.content.getOrElse(
                "Error: tool_calls finish reason but no tools"
              )
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
      finalResponse =
        s"Max turns (${config.maxTurns}) reached. Last response: ${conversationMessages.lastOption.flatMap(_.content).getOrElse("")}"
    }

    (finalResponse, conversationMessages)
  }
}


/** RAG (Retrieval-Augmented Generation) Agent Configuration */
case class RAGConfig(
    embeddingModel: String = "text-embedding-3-small",
    topK: Int = 5,
    embeddingEndpoint: String = "http://localhost:8080/v1/embeddings"
)

