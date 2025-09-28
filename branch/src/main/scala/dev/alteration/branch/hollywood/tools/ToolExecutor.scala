package dev.alteration.branch.hollywood.tools

import scala.quoted.*

trait ToolExecutor[T] {
  def execute(
      functionName: String,
      args: Map[String, String]
  ): String
}

object ToolExecutor {
  // Derive a ToolExecutor for any type at compile time
  inline def derived[T]: ToolExecutor[T] = ${ derivedImpl[T] }

  private def derivedImpl[T: Type](using Quotes): Expr[ToolExecutor[T]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    // Find all @Tool methods
    val toolMethods = tpe.typeSymbol.methodMembers.filter { method =>
      method.annotations.exists(_.tpe =:= TypeRepr.of[Tool])
    }

    if (toolMethods.isEmpty) {
      report.errorAndAbort(s"No @Tool methods found in ${tpe.show}")
    }

    // Generate match cases for each tool
    val cases = toolMethods.map { method =>
      val methodName = method.name
      val methodType = tpe.memberType(method).widen

      methodType match {
        case MethodType(paramNames, paramTypes, _) =>
          // Generate code to extract and convert each parameter
          val paramExprs = (paramNames zip paramTypes).map { case (name, tpe) =>
            convertParameter(name, tpe)
          }

          // Generate the method call on a summoned instance
          val methodCallExpr = '{
            val instance = ${ 
              Expr.summon[T] match {
                case Some(inst) => inst
                case None => report.errorAndAbort(s"No given instance of ${tpe.show} found. Please provide: given ${tpe.show} = ...")
              }
            }
            ${
              Select
                .unique('{ instance }.asTerm, methodName)
                .appliedToArgs(paramExprs.map(_.asTerm))
                .asExprOf[Any]
            }.toString
          }

          (methodName, methodCallExpr)

        case _ =>
          report.errorAndAbort(s"Unsupported method type: ${methodType.show}")
      }
    }

    // Build the executor
    '{
      new ToolExecutor[T] {
        def execute(
            functionName: String,
            args: Map[String, String]
        ): String = {
          given Map[String, String] = args
          ${
            val matchExpr = Match(
              '{ functionName }.asTerm,
              cases.map { case (name, expr) =>
                CaseDef(Literal(StringConstant(name)), None, expr.asTerm)
              }.toList :+
                CaseDef(
                  Wildcard(),
                  None,
                  '{
                    throw new IllegalArgumentException(
                      s"Unknown function: $functionName"
                    )
                  }.asTerm
                )
            )
            matchExpr.asExprOf[String]
          }
        }
      }
    }
  }

  private def convertParameter(using
      Quotes
  )(paramName: String, tpe: quotes.reflect.TypeRepr): Expr[Any] = {
    import quotes.reflect.*

    // Special case for String parameters - no conversion needed
    if (tpe =:= TypeRepr.of[String]) {
      '{
        val args = ${ Expr.summon[Map[String, String]].get }
        args(${ Expr(paramName) })
      }
    } else {
      // Try to summon a Conversion[String, T] for other parameter types
      tpe.asType match {
        case '[t] =>
          Expr.summon[Conversion[String, t]] match {
            case Some(conversion) =>
              '{
                val args  = ${ Expr.summon[Map[String, String]].get }
                val value = args(${ Expr(paramName) })
                $conversion.apply(value)
              }
            case None =>
              report.errorAndAbort(
                s"No given Conversion[String, ${tpe.show}] found for parameter '$paramName'. " +
                  s"Please define: given Conversion[String, ${tpe.show}] = ..."
              )
          }
      }
    }
  }

}
