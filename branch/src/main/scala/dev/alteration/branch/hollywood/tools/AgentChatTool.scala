package dev.alteration.branch.hollywood.tools

import scala.util.{Success, Try}

private[hollywood] case class AgentChatTool(message: String)
    extends CallableTool[String] {
  def execute(): Try[String] = Success(message)
}
