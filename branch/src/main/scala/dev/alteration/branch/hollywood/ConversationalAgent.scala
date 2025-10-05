package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.{
  ChatCompletionsRequest,
  ChatCompletionsResponse,
  ChatMessage
}
import dev.alteration.branch.hollywood.tools.ToolRegistry

class ConversationalAgent(
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      Agent.defaultHandler,
    toolRegistry: Option[ToolRegistry] = None,
    maxTurns: Int = 50,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None,
    conversationState: ConversationState = new InMemoryState()
) extends Agent {

  def chat(message: String): String = {
    // Add user message to conversation
    val userMessage     = ChatMessage(role = "user", content = Some(message))
    val currentMessages = conversationState.get :+ userMessage

    // Run shared conversation loop
    val (response, updatedMessages) = AgentConversationLoop.run(
      currentMessages,
      requestHandler,
      toolRegistry,
      maxTurns,
      model,
      onTurn
    )
    conversationState.update(updatedMessages)

    response
  }
}
