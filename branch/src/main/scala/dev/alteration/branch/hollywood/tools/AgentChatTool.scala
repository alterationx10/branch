package dev.alteration.branch.hollywood.tools

private[hollywood] case class AgentChatTool(message: String)
    extends CallableTool[String] {
  def execute(): String = message
}
