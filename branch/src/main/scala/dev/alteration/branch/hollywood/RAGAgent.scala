package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.http.JsonBodyPublisher
import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.ClientRequest
import dev.alteration.branch.spider.client.ClientRequest.withContentType

import java.net.URI
import java.net.http.HttpClient

class RAGAgent(
    vectorStore: VectorStore,
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      ConversationalAgent.defaultHandler,
    embeddingHandler: EmbeddingsRequest => EmbeddingsResponse =
      RAGAgent.defaultEmbeddingHandler,
    toolRegistry: Option[ToolRegistry] = None,
    config: AgentConfig = AgentConfig(),
    ragConfig: RAGConfig = RAGConfig()
) extends Agent {

  private var conversationMessages: List[ChatMessage] = List.empty

  /** Index documents into the vector store */
  def indexDocuments(documents: List[(String, String)]): Unit = {
    documents.foreach { case (id, content) =>
      val embedding = getEmbedding(content)
      vectorStore.add(id, content, embedding)
    }
  }

  /** Get embedding for a text */
  private def getEmbedding(text: String): List[Double] = {
    val request  = EmbeddingsRequest(
      input = dev.alteration.branch.friday.Json.JsonString(text),
      model = ragConfig.embeddingModel
    )
    val response = embeddingHandler(request)
    response.data.headOption
      .map(_.embedding)
      .getOrElse(throw new RuntimeException("Failed to get embedding"))
  }

  override def chat(message: String): String = {
    // Get query embedding and retrieve relevant documents
    val queryEmbedding = getEmbedding(message)
    val relevantDocs   = vectorStore.search(queryEmbedding, ragConfig.topK)

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
      config
    )
    conversationMessages = updatedMessages

    response
  }
}

object RAGAgent {

  val defaultClient: HttpClient =
    HttpClient.newHttpClient()

  val defaultEmbeddingHandler: EmbeddingsRequest => EmbeddingsResponse = {
    req =>
      {
        import java.net.http.HttpResponse
        import java.nio.charset.StandardCharsets
        import dev.alteration.branch.friday.Json

        val httpRequest = ClientRequest
          .builder(URI.create("http://localhost:8080/v1/embeddings"))
          .withContentType(ContentType.json)
          .POST(JsonBodyPublisher.of[EmbeddingsRequest](req))
          .build()

        // First check status code by getting raw response
        val rawResponse = defaultClient.send(
          httpRequest,
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        )

        if (rawResponse.statusCode() != 200) {
          val errorBody = rawResponse.body()
          throw new RuntimeException(
            s"Embeddings API returned error status ${rawResponse.statusCode()}. " +
              s"Response: $errorBody"
          )
        }

        // Parse the successful response
        Json.decode[EmbeddingsResponse](rawResponse.body()).getOrElse {
          throw new RuntimeException(
            s"Failed to parse embeddings response. Body: ${rawResponse.body()}"
          )
        }
      }
  }
}
