package dev.alteration.branch.hollywood.tools.schema

import dev.alteration.branch.hollywood.tools.*

import scala.quoted.*

case class ToolSchema(
    name: String,
    description: String,
    parameters: ParameterSchema
)

object ToolSchema {

  inline def derive[T]: ToolSchema =
    ${ deriveImpl[T] }

  private def deriveImpl[T: Type](using Quotes): Expr[ToolSchema] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val classSymbol = tpe.typeSymbol

    // Extract @Tool annotation from the case class
    val toolAnnot = classSymbol.annotations
      .find { annot =>
        annot.tpe =:= TypeRepr.of[schema.Tool]
      }
      .getOrElse(
        report.errorAndAbort(
          s"Class ${classSymbol.name} must have @ToolS annotation"
        )
      )

    val description = toolAnnot match {
      case Apply(_, List(Literal(StringConstant(desc)))) => desc
      case _                                             => ""
    }

    // Get case class constructor parameters
    val constructor = classSymbol.primaryConstructor
    val constructorType = tpe.memberType(constructor)

    val (properties, required) = constructorType match {
      case MethodType(paramNames, paramTypes, _) =>
        val props = (paramNames zip paramTypes).map { case (name, tpe) =>
          // Find @Param annotation on this parameter
          val paramSymbol = constructor.paramSymss.flatten.find(_.name == name)
          val paramAnnot = paramSymbol.flatMap { sym =>
            sym.annotations.find(_.tpe =:= TypeRepr.of[Param])
          }

          val paramDesc = paramAnnot match {
            case Some(Apply(_, List(Literal(StringConstant(desc))))) => desc
            case _                                                   => ""
          }

          val (jsonType, enumValues) = typeToJsonType(tpe)
          val isOptional = isOptionType(tpe)

          (name, jsonType, paramDesc, enumValues, isOptional)
        }

        val propsMap = props.map { case (name, jsonType, desc, enumVals, _) =>
          (name, PropertySchema(jsonType, desc, enumVals))
        }.toMap

        val requiredList = props.filterNot(_._5).map(_._1)

        (propsMap, requiredList)

      case _ =>
        report.errorAndAbort(s"Invalid constructor type for ${classSymbol.name}")
    }

    // Generate the expression
    val propsSeq = properties.toSeq.map { case (k, v) =>
      '{
        (
          ${ Expr(k) },
          PropertySchema(
            ${ Expr(v.`type`) },
            ${ Expr(v.description) },
            ${ Expr(v.enumValues) }
          )
        )
      }
    }
    val propsExpr = Expr.ofSeq(propsSeq)

    '{
      ToolSchema(
        ${ Expr(classSymbol.name) },
        ${ Expr(description) },
        ParameterSchema(
          "object",
          $propsExpr.toMap,
          ${ Expr(required.toList) }
        )
      )
    }
  }

  private def typeToJsonType(using
      Quotes
  )(tpe: quotes.reflect.TypeRepr): (String, Option[List[String]]) = {
    import quotes.reflect.*

    if (tpe =:= TypeRepr.of[String]) ("string", None)
    else if (tpe =:= TypeRepr.of[Int] || tpe =:= TypeRepr.of[Long])
      ("integer", None)
    else if (tpe =:= TypeRepr.of[Double] || tpe =:= TypeRepr.of[Float])
      ("number", None)
    else if (tpe =:= TypeRepr.of[Boolean]) ("boolean", None)
    else if (isOptionType(tpe)) {
      // Unwrap Option
      tpe match {
        case AppliedType(_, List(innerType)) => typeToJsonType(innerType)
        case _                               => ("string", None)
      }
    } else if (
      tpe.typeSymbol.flags
        .is(Flags.Sealed) && tpe.typeSymbol.flags.is(Flags.Trait)
    ) {
      // Handle sealed traits as enums
      val children = tpe.typeSymbol.children.map(_.name)
      ("string", Some(children))
    } else ("string", None)
  }

  private def isOptionType(using
      Quotes
  )(tpe: quotes.reflect.TypeRepr): Boolean = {
    import quotes.reflect.*
    tpe.baseType(TypeRepr.of[Option[?]].typeSymbol) != TypeRepr.of[Nothing]
  }

  def toJson(schema: ToolSchema): String = {
    val props = schema.parameters.properties
      .map { case (name, prop) =>
        val enumPart = prop.enumValues
          .map(values =>
            s""", "enum": [${values.map(v => s""""$v"""").mkString(", ")}]"""
          )
          .getOrElse("")

        s""""$name": {
           |  "type": "${prop.`type`}",
           |  "description": "${prop.description}"$enumPart
           |}""".stripMargin
      }
      .mkString(",\n        ")

    val required = if (schema.parameters.required.nonEmpty) {
      s""",
         |      "required": [${schema.parameters.required
          .map(r => s""""$r"""")
          .mkString(", ")}]""".stripMargin
    } else ""

    s"""{
       |  "type": "function",
       |  "function": {
       |    "name": "${schema.name}",
       |    "description": "${schema.description}",
       |    "parameters": {
       |      "type": "object",
       |      "properties": {
       |        $props
       |      }$required
       |    }
       |  }
       |}""".stripMargin
  }
}
