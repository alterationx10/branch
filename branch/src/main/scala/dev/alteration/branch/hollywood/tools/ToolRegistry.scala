package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonEncoder, JsonSchema}
import dev.alteration.branch.hollywood.api.{FunctionDefinition, Tool}
import dev.alteration.branch.hollywood.tools.schema.ToolSchema
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolExecutor}

import scala.collection.mutable
import scala.deriving.Mirror

trait ToolRegistry {
  def registerTool(
      schema: ToolSchema,
      executor: ToolExecutor[? <: CallableTool[?]]
  ): Unit
  def getSchemas: List[ToolSchema]
  def getFunctionDefinitions: List[FunctionDefinition]
  def getTools: List[Tool]
  def getSchemasJson: String
  def execute(toolName: String, args: Json): Option[Json]
  def clear(): Unit
  def getRegisteredToolNames: List[String]
}

object ToolRegistry {
  def apply(): ToolRegistry = MutableToolRegistry()

  // Extension method to add inline register to any ToolRegistry
  extension (registry: ToolRegistry) {
    inline def register[T <: CallableTool[?]](using
        m: Mirror.ProductOf[T],
        encoder: JsonEncoder[ToolExecutor.ResultType[T]]
    ): ToolRegistry = {
      val schema   = ToolSchema.derive[T]
      val executor = ToolExecutor.derived[T]
      registry.registerTool(schema, executor)
      registry
    }

    // Method to register with pre-built schema and executor (for agent tools)
    def register[A](
        schema: ToolSchema,
        executor: ToolExecutor[? <: CallableTool[A]]
    ): ToolRegistry = {
      registry.registerTool(schema, executor)
      registry
    }

    // Method to register a tuple of (ToolSchema, ToolExecutor) directly
    def register(
        tool: (ToolSchema, ToolExecutor[? <: CallableTool[?]])
    ): ToolRegistry = {
      registry.registerTool(tool._1, tool._2)
      registry
    }
  }
}

case class MutableToolRegistry() extends ToolRegistry {
  private val tools =
    mutable.Map[String, (ToolSchema, ToolExecutor[? <: CallableTool[?]])]()

  def registerTool(
      schema: ToolSchema,
      executor: ToolExecutor[? <: CallableTool[?]]
  ): Unit = {
    tools(schema.name) = (schema, executor)
  }

  def getSchemas: List[ToolSchema] = tools.values.map(_._1).toList

  def getFunctionDefinitions: List[FunctionDefinition] = {
    getSchemas.map(schemaToFunctionDefinition)
  }

  def getTools: List[Tool] = {
    getFunctionDefinitions.map(funcDef => Tool("function", funcDef))
  }

  def getSchemasJson: String = {
    val schemas = getSchemas.map(ToolSchema.toJson)
    s"[${schemas.mkString(", ")}]"
  }

  private def schemaToFunctionDefinition(
      schema: ToolSchema
  ): FunctionDefinition = {
    val parametersJson = JsonSchema.toJson(schema.parameters)
    FunctionDefinition(
      name = schema.name,
      description = Some(schema.description),
      parameters = Some(parametersJson),
      strict = None
    )
  }

  def execute(toolName: String, args: Json): Option[Json] = {
    tools.get(toolName).map { case (_, executor) =>
      executor.execute(args)
    }
  }

  def clear(): Unit = tools.clear()

  def getRegisteredToolNames: List[String] = tools.keys.toList
}
