package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.ClientRequest
import dev.alteration.branch.spider.client.ClientRequest.withContentType

import java.net.URI
import java.net.http.HttpClient

case class AgentConfig(
    maxTurns: Int = 50,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None
)

trait Agent {
  def chat(message: String): String
}

object ConversationalAgent {

  import ChatCompletionsRequest.given

  val defaultClient: HttpClient =
    HttpClient.newHttpClient()

  val defaultHandler: ChatCompletionsRequest => ChatCompletionsResponse = {
    req =>
      {
        val httpRequest = ClientRequest
          .builder(URI.create("http://localhost:8080/v1/chat/completions"))
          .withContentType(ContentType.json)
          .POST(JsonBodyPublisher.of[ChatCompletionsRequest](req))
          .build()

        defaultClient
          .send(httpRequest, JsonBodyHandler.of[ChatCompletionsResponse])
          .body()
          .get
      }
  }
  
}

class ConversationalAgent(
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      defaultHandler,
    toolRegistry: Option[ToolRegistry] = None,
    config: AgentConfig = AgentConfig()
) extends Agent {

  private var conversationMessages: List[ChatMessage] = List.empty

  def chat(message: String): String = {
    // Add user message to conversation
    val userMessage = ChatMessage(role = "user", content = Some(message))
    conversationMessages = conversationMessages :+ userMessage

    // Run multi-turn loop
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
        s"Max turns ($config.maxTurns) reached. Last response: ${conversationMessages.lastOption.flatMap(_.content).getOrElse("")}"
    }

    finalResponse
  }
}

/** RAG (Retrieval-Augmented Generation) Agent Configuration */
case class RAGConfig(
    embeddingModel: String = "text-embedding-3-small",
    topK: Int = 5,
    embeddingEndpoint: String = "http://localhost:8080/v1/embeddings"
)

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
    val request = EmbeddingsRequest(
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
    val relevantDocs = vectorStore.search(queryEmbedding, ragConfig.topK)

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
    val userMessage = ChatMessage(role = "user", content = Some(message))

    conversationMessages = List(systemMessage) ++ conversationMessages.filterNot(_.role == "system")
    conversationMessages = conversationMessages :+ userMessage

    // Run multi-turn loop (similar to ConversationalAgent)
    var currentTurn          = 0
    var continueConversation = true
    var finalResponse        = ""

    while (continueConversation && currentTurn < config.maxTurns) {
      currentTurn += 1

      val request = ChatCompletionsRequest(
        messages = conversationMessages,
        tools = toolRegistry.map(_.getTools),
        model = config.model
      )

      val response = requestHandler(request)
      val choice   = response.choices.head

      val assistantMessage = choice.message.getOrElse(
        ChatMessage(role = "assistant", content = Some("No response"))
      )

      conversationMessages = conversationMessages :+ assistantMessage

      config.onTurn.foreach(_(currentTurn, assistantMessage))

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

    finalResponse
  }
}

object RAGAgent {

  val defaultClient: HttpClient =
    HttpClient.newHttpClient()

  val defaultEmbeddingHandler: EmbeddingsRequest => EmbeddingsResponse = {
    req =>
      {
        val httpRequest = ClientRequest
          .builder(URI.create("http://localhost:8080/v1/embeddings"))
          .withContentType(ContentType.json)
          .POST(JsonBodyPublisher.of[EmbeddingsRequest](req))
          .build()

        defaultClient
          .send(httpRequest, JsonBodyHandler.of[EmbeddingsResponse])
          .body()
          .get
      }
  }
}
