package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.clients.completions.ChatMessage

trait ConversationState {
  def get: List[ChatMessage]
  def update(messages: List[ChatMessage]): Unit
}

class InMemoryState(maxMessages: Int = 50) extends ConversationState {
  private var messages: List[ChatMessage] = List.empty

  override def get: List[ChatMessage] = messages

  override def update(messages: List[ChatMessage]): Unit = {
    this.messages = messages.takeRight(maxMessages)
  }
}
