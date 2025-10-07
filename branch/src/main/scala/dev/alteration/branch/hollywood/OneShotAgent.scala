package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.clients.completions.{ChatCompletionsRequest, ChatCompletionsResponse, ChatMessage}
import dev.alteration.branch.hollywood.tools.ToolRegistry

/** A stateless agent that executes a single request-response cycle with fixed
  * system and user prompts. Useful as a tool within other agents.
  *
  * @param systemPrompt
  *   The system prompt to use for the agent
  * @param requestHandler
  *   Function to make chat completion requests
  * @param toolRegistry
  *   Optional tool registry for tool execution
  * @param maxTurns
  *   Maximum number of turns (default 10, since tool use may require multiple
  *   turns)
  * @param model
  *   Model to use for chat completions
  * @param onTurn
  *   Optional callback for each turn
  */
class OneShotAgent(
    systemPrompt: String,
    requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      Agent.defaultHandler,
    toolRegistry: Option[ToolRegistry] = None,
    maxTurns: Int = 10,
    model: String = "gpt-oss",
    onTurn: Option[(Int, ChatMessage) => Unit] = None
) extends Agent {

  /** Execute the one-shot agent with the given user message
    *
    * @param message
    *   The user message to process
    * @return
    *   The agent's response
    */
  override def chat(message: String): String = {
    val messages = List(
      ChatMessage(role = "system", content = Some(systemPrompt)),
      ChatMessage(role = "user", content = Some(message))
    )

    val (response, _) = AgentConversationLoop.run(
      messages,
      requestHandler,
      toolRegistry,
      maxTurns,
      model,
      onTurn
    )

    response
  }

  /** Execute the one-shot agent with a custom user prompt template
    *
    * @param userPromptTemplate
    *   Template string for the user prompt (can contain placeholders)
    * @param variables
    *   Variables to substitute in the template
    * @return
    *   The agent's response
    */
  def execute(
      userPromptTemplate: String,
      variables: Map[String, String] = Map.empty
  ): String = {
    val userPrompt = variables.foldLeft(userPromptTemplate) {
      case (template, (key, value)) =>
        template.replace(s"{$key}", value)
    }

    chat(userPrompt)
  }
}

object OneShotAgent {

  /** Create a specialized one-shot agent for a specific task
    *
    * @param taskName
    *   Name of the task
    * @param taskDescription
    *   Description of what the agent should do
    * @param inputFormat
    *   Optional description of expected input format
    * @param outputFormat
    *   Optional description of expected output format
    * @param requestHandler
    *   Optional request handler
    * @param toolRegistry
    *   Optional tool registry
    * @param maxTurns
    *   Maximum turns (default 10)
    * @param model
    *   Model to use
    * @return
    *   A new OneShotAgent configured for the task
    */
  def forTask(
      taskName: String,
      taskDescription: String,
      inputFormat: Option[String] = None,
      outputFormat: Option[String] = None,
      requestHandler: ChatCompletionsRequest => ChatCompletionsResponse =
        Agent.defaultHandler,
      toolRegistry: Option[ToolRegistry] = None,
      maxTurns: Int = 10,
      model: String = "gpt-oss"
  ): OneShotAgent = {
    val systemPrompt = buildTaskSystemPrompt(
      taskName,
      taskDescription,
      inputFormat,
      outputFormat
    )
    OneShotAgent(systemPrompt, requestHandler, toolRegistry, maxTurns, model)
  }

  private def buildTaskSystemPrompt(
      taskName: String,
      taskDescription: String,
      inputFormat: Option[String],
      outputFormat: Option[String]
  ): String = {
    val parts = List(
      Some(s"Task: $taskName"),
      Some(taskDescription),
      inputFormat.map(fmt => s"Input format: $fmt"),
      outputFormat.map(fmt => s"Output format: $fmt")
    ).flatten

    parts.mkString("\n\n")
  }
}
