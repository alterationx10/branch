package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.schema.{
  ParameterSchema,
  PropertySchema,
  ToolSchema
}
import dev.alteration.branch.hollywood.tools.{
  AgentChatTool,
  CallableTool,
  ToolExecutor,
  ToolRegistry
}

trait Agent {
  def chat(message: String): String
}

object Agent {

  /** Derives a tool that wraps an agent's chat method for agent-to-agent
    * communication
    *
    * @param agent
    *   The agent instance to wrap
    * @param agentName
    *   Name to use for the tool (defaults to agent's class name)
    * @param description
    *   Description of what the agent does
    * @return
    *   Tuple of (ToolSchema, ToolExecutor) ready to register in a ToolRegistry
    */
  inline def deriveAgentTool[A <: Agent](
      agent: A,
      agentName: Option[String] = None,
      description: String = "Chat with an agent to get specialized assistance"
  ): (ToolSchema, ToolExecutor[? <: CallableTool[?]]) = {

    val toolName =
      agentName.getOrElse(agent.getClass.getSimpleName.stripSuffix("$"))

    // Create the schema manually since we're not using @Tool annotation
    val toolSchema = ToolSchema(
      name = toolName,
      description = description,
      parameters = ParameterSchema(
        `type` = "object",
        properties = Map(
          "message" -> PropertySchema(
            `type` = "string",
            description = "The message to send to the agent",
            enumValues = None
          )
        ),
        required = List("message")
      )
    )

    // Create the executor - we're ignoring the implementation of AgentChatTool.execute()
    val executor = new ToolExecutor[AgentChatTool] {
      override def execute(args: Map[String, String]): String = {
        val message = args("message")
        agent.chat(message)
      }
    }

    (toolSchema, executor)
  }

}

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
      requestHandler: ChatCompletionsRequest => ChatCompletionsResponse,
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

      val response = requestHandler(request)
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

    if (currentTurn >= maxTurns && continueConversation) {
      finalResponse =
        s"Max turns ($maxTurns) reached. Last response: ${conversationMessages.lastOption.flatMap(_.content).getOrElse("")}"
    }

    (finalResponse, conversationMessages)
  }
}
