package dev.alteration.branch.hollywood

import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.http.{JsonBodyHandler, JsonBodyPublisher}
import dev.alteration.branch.hollywood.api.{
  ChatCompletionsRequest,
  ChatCompletionsResponse
}
import dev.alteration.branch.hollywood.tools.schema.{
  ParameterSchema,
  PropertySchema,
  ToolSchema
}
import dev.alteration.branch.hollywood.tools.{
  AgentChatTool,
  CallableTool,
  ToolExecutor
}
import dev.alteration.branch.spider.ContentType
import dev.alteration.branch.spider.client.{Client, ClientRequest}
import dev.alteration.branch.spider.client.ClientRequest.withContentType
import dev.alteration.branch.veil.Veil

import java.net.URI
import scala.util.*

trait Agent {
  def chat(message: String): String
}

object Agent {

  import ChatCompletionsRequest.given

  val defaultHandler: ChatCompletionsRequest => ChatCompletionsResponse = {
    req =>
      {
        val baseUrl = Veil
          .getFirst("LLAMA_SERVER_COMPLETION_URL", "LLAMA_SERVER_URL")
          .getOrElse("http://localhost:8080")

        val httpRequest = ClientRequest
          .builder(URI.create(s"$baseUrl/v1/chat/completions"))
          .withContentType(ContentType.json)
          .POST(
            JsonBodyPublisher
              .of[ChatCompletionsRequest](req, removeNulls = true)
          )
          .build()

        Client.defaultClient
          .send(httpRequest, JsonBodyHandler.of[ChatCompletionsResponse])
          .body()
          .get
      }
  }

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
      override def execute(
          args: dev.alteration.branch.friday.Json
      ): dev.alteration.branch.friday.Json = {
        import dev.alteration.branch.friday.Json.*
        val message = args match {
          case JsonObject(obj) =>
            obj.get("message") match {
              case Some(JsonString(str)) => str
              case _                     => "No message provided"
            }
          case _               => "Invalid arguments"
        }
        val result  = Try(agent.chat(message)) match {
          case Success(value) => JsonString(value)
          case Failure(e)     =>
            Json.JsonString(s"Error executing Agent tool: ${e.getMessage}")
        }
        result
      }
    }

    (toolSchema, executor)
  }

}
