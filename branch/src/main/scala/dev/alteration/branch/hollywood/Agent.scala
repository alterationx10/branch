package dev.alteration.branch.hollywood

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
      override def execute(args: dev.alteration.branch.friday.Json): String = {
        import dev.alteration.branch.friday.Json.*
        val message = args match {
          case JsonObject(obj) =>
            obj.get("message") match {
              case Some(JsonString(str)) => str
              case _                     => "No message provided"
            }
          case _ => "Invalid arguments"
        }
        agent.chat(message)
      }
    }

    (toolSchema, executor)
  }

}
