package dev.alteration.branch.hollywood.tools.schema

import dev.alteration.branch.friday.{JsonSchema, Schema}
import dev.alteration.branch.hollywood.tools.*

import scala.quoted.*

case class ToolSchema(
    name: String,
    description: String,
    parameters: Schema
)

object ToolSchema {

  inline def derive[T]: ToolSchema =
    ${ deriveImpl[T] }

  private def deriveImpl[T: Type](using Quotes): Expr[ToolSchema] = {
    import quotes.reflect.*

    val tpe         = TypeRepr.of[T]
    val classSymbol = tpe.typeSymbol

    // Extract @Tool annotation from the case class
    val toolAnnot = classSymbol.annotations
      .find { annot =>
        annot.tpe =:= TypeRepr.of[schema.Tool]
      }
      .getOrElse(
        report.errorAndAbort(
          s"Class ${classSymbol.name} must have @Tool annotation"
        )
      )

    val toolDescription = toolAnnot match {
      case Apply(_, List(Literal(StringConstant(desc)))) => desc
      case _                                             => ""
    }

    // Get parameter descriptions from @Param annotations
    val constructor     = classSymbol.primaryConstructor
    val constructorType = tpe.memberType(constructor)

    val paramDescriptions = constructorType match {
      case MethodType(paramNames, _, _) =>
        paramNames.map { name =>
          val paramSymbol = constructor.paramSymss.flatten.find(_.name == name)
          val paramAnnot  = paramSymbol.flatMap { sym =>
            sym.annotations.find(_.tpe =:= TypeRepr.of[Param])
          }

          val desc = paramAnnot match {
            case Some(Apply(_, List(Literal(StringConstant(desc))))) => desc
            case _                                                   => ""
          }
          name -> desc
        }.toMap

      case other =>
        report.errorAndAbort(
          s"Invalid constructor type for ${classSymbol.name}: expected MethodType, got ${other.show}"
        )
    }

    // Use JsonSchema to derive the schema structure
    // We need to summon the Mirror at compile time for the JsonSchema derivation
    Expr.summon[scala.deriving.Mirror.Of[T]] match {
      case Some(mirrorExpr) =>
        '{
          val schema =
            JsonSchema.of[T](using JsonSchema.derived[T](using $mirrorExpr))
          ToolSchema.fromJsonSchema(
            ${ Expr(classSymbol.fullName) },
            ${ Expr(toolDescription) },
            schema,
            ${ Expr(paramDescriptions) }
          )
        }
      case None             =>
        report.errorAndAbort(
          s"Cannot derive JsonSchema for ${classSymbol.name}: Mirror.Of not found"
        )
    }
  }

  /** Convert a JsonSchema Schema to ToolSchema using parameter descriptions */
  private def fromJsonSchema(
      name: String,
      description: String,
      schema: Schema,
      paramDescriptions: Map[String, String]
  ): ToolSchema = {
    schema match {
      case Schema.ObjectSchema(properties, required, _) =>
        // Add descriptions from @Param annotations to the schema
        val propsWithDescriptions = properties.map {
          case (propName, propSchema) =>
            val desc = paramDescriptions.getOrElse(propName, "")
            propName -> addDescription(propSchema, desc)
        }
        ToolSchema(
          name,
          description,
          Schema.ObjectSchema(propsWithDescriptions, required, None)
        )

      case _ =>
        throw new IllegalArgumentException(
          s"Expected ObjectSchema for tool parameters, got ${schema.getClass.getSimpleName}"
        )
    }
  }

  /** Add a description to a Schema */
  private def addDescription(schema: Schema, description: String): Schema = {
    if (description.isEmpty) schema
    else
      schema match {
        case Schema.StringSchema(_) =>
          Schema.StringSchema(Some(description))

        case Schema.NumberSchema(_) =>
          Schema.NumberSchema(Some(description))

        case Schema.IntegerSchema(_) =>
          Schema.IntegerSchema(Some(description))

        case Schema.BooleanSchema(_) =>
          Schema.BooleanSchema(Some(description))

        case Schema.ArraySchema(items, _) =>
          Schema.ArraySchema(items, Some(description))

        case Schema.EnumSchema(values, _) =>
          Schema.EnumSchema(values, Some(description))

        case Schema.ObjectSchema(properties, required, _) =>
          Schema.ObjectSchema(properties, required, Some(description))
      }
  }

  def toJson(schema: ToolSchema): String = {
    import dev.alteration.branch.friday.Json

    val parametersJson = JsonSchema.toJson(schema.parameters)

    val toolJson = Json.JsonObject(
      Map(
        "type"     -> Json.JsonString("function"),
        "function" -> Json.JsonObject(
          Map(
            "name"        -> Json.JsonString(schema.name),
            "description" -> Json.JsonString(schema.description),
            "parameters"  -> parametersJson
          )
        )
      )
    )

    toolJson.toJsonString
  }
}
