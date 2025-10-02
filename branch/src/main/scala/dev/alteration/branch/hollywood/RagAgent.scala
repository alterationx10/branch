package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.clients.EmbeddingClient
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.rag.VectorStore

class RagAgent(
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      ConversationalAgent.defaultHandler,
    embeddingClient: EmbeddingClient,
    toolRegistry: Option[ToolRegistry] = None,
    vectorStore: VectorStore,
    maxTurns: Int = 50,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None,
    topK: Int = 5
) extends Agent {

  private var conversationMessages: List[ChatMessage] = List.empty

  override def chat(message: String): String = {
    // Get query embedding and retrieve relevant documents
    val queryEmbedding = embeddingClient.getEmbedding(message)
    val relevantDocs   = vectorStore.search(queryEmbedding, topK)

    // Build context from retrieved documents
    val context = if (relevantDocs.nonEmpty) {
      val docsText = relevantDocs
        .map { scored =>
          s"[Relevance: ${"%.1f".format(scored.score * 100)}%]\n${scored.document.content}"
        }
        .mkString("\n\n---\n\n")

      s"""Here are relevant documents from the knowledge base:

$docsText

---

Based on the above context, please answer the following question:"""
    } else {
      "No relevant context found in the knowledge base. Please answer based on your general knowledge:"
    }

    // Create system message with context and add user message
    val systemMessage = ChatMessage(role = "system", content = Some(context))
    val userMessage   = ChatMessage(role = "user", content = Some(message))

    conversationMessages =
      List(systemMessage) ++ conversationMessages.filterNot(_.role == "system")
    conversationMessages = conversationMessages :+ userMessage

    // Run shared conversation loop
    val (response, updatedMessages) = AgentConversationLoop.run(
      conversationMessages,
      requestHandler,
      toolRegistry,
      maxTurns,
      model,
      onTurn
    )
    conversationMessages = updatedMessages

    response
  }
}
