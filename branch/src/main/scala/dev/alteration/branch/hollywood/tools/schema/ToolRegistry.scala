package dev.alteration.branch.hollywood.tools.schema

import dev.alteration.branch.hollywood.tools.{Tool, ToolExecutor}

import scala.collection.mutable
import scala.deriving.Mirror

// Don't know how I feel about this being a singleton with a global state...
object ToolRegistry {
  private val tools = mutable.Map[String, (ToolSchema, ToolExecutor[? <: Tool[?]])]()

  inline def register[T <: Tool[?]](using m: Mirror.ProductOf[T]): Unit = {
    val schema = ToolSchema.derive[T]
    val executor = ToolExecutor.derived[T]
    tools(schema.name) = (schema, executor)
  }

  def getSchemas: List[ToolSchema] = tools.values.map(_._1).toList

  def getSchemasJson: String = {
    val schemas = getSchemas.map(ToolSchema.toJson)
    s"[${schemas.mkString(", ")}]"
  }

  def execute(toolName: String, args: Map[String, String]): Option[String] = {
    tools.get(toolName).map { case (_, executor) =>
      executor.execute(args)
    }
  }

  def clear(): Unit = tools.clear()

  def getRegisteredToolNames: List[String] = tools.keys.toList
}
