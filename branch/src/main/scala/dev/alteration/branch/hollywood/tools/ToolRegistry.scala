package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.Json
import dev.alteration.branch.hollywood.api.{FunctionDefinition, Tool}
import dev.alteration.branch.hollywood.tools.schema.{
  ParameterSchema,
  ToolSchema
}
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
  def execute(toolName: String, args: Map[String, String]): Option[String]
  def clear(): Unit
  def getRegisteredToolNames: List[String]
}

object ToolRegistry {
  def apply(): ToolRegistry = MutableToolRegistry()

  // Extension method to add inline register to any ToolRegistry
  extension (registry: ToolRegistry) {
    inline def register[T <: CallableTool[?]](using
        m: Mirror.ProductOf[T]
    ): ToolRegistry = {
      val schema   = ToolSchema.derive[T]
      val executor = ToolExecutor.derived[T]
      registry.registerTool(schema, executor)
      registry
    }

    // Method to register with pre-built schema and executor (for agent tools)
    def register(
        schema: ToolSchema,
        executor: ToolExecutor[? <: CallableTool[?]]
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
    val parametersJson = parametersToJson(schema.parameters)
    FunctionDefinition(
      name = schema.name,
      description = Some(schema.description),
      parameters = Some(parametersJson),
      strict = None
    )
  }

  private def parametersToJson(params: ParameterSchema): Json = {
    val properties = Json.JsonObject(
      params.properties.map { case (name, prop) =>
        val propJson = Json.JsonObject(
          Map(
            "type"        -> Json.JsonString(prop.`type`),
            "description" -> Json.JsonString(prop.description)
          ) ++ prop.enumValues
            .map(values =>
              "enum" -> Json.JsonArray(
                values.map(Json.JsonString.apply).toIndexedSeq
              )
            )
            .toMap
        )
        name -> propJson
      }
    )

    Json.JsonObject(
      Map(
        "type"       -> Json.JsonString(params.`type`),
        "properties" -> properties,
        "required"   -> Json.JsonArray(
          params.required.map(Json.JsonString.apply).toIndexedSeq
        )
      )
    )
  }

  def execute(toolName: String, args: Map[String, String]): Option[String] = {
    tools.get(toolName).map { case (_, executor) =>
      executor.execute(args)
    }
  }

  def clear(): Unit = tools.clear()

  def getRegisteredToolNames: List[String] = tools.keys.toList
}
