package dev.alteration.branch.hollywood.tools

import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.compiletime.*
import scala.language.implicitConversions

trait ToolExecutor[T <: CallableTool[?]] {
  def execute(
      args: Map[String, String]
  ): String
}

object ToolExecutor {

  // Use explicit method calls, not implicit conversions
  // e.g. Converter[String, Int] = s => _.toInt
  // _.toInt is itself an implicit conversion, so... infinite recursion
  given Conversion[String, String] = identity

  given Conversion[String, Int] = s => Integer.parseInt(s) // Not s.toInt

  given Conversion[String, Long] = s =>
    java.lang.Long.parseLong(s) // Not s.toLong

  given Conversion[String, Double] = s =>
    java.lang.Double.parseDouble(s) // Not s.toDouble

  given Conversion[String, Float] = s =>
    java.lang.Float.parseFloat(s) // Not s.toFloat

  given Conversion[String, Boolean] = s =>
    java.lang.Boolean.parseBoolean(s) // Not s.toBoolean

  private inline def summonConverters[A <: Tuple]: List[Conversion[String, ?]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[Conversion[String, t]]
          .asInstanceOf[Conversion[String, ?]] :: summonConverters[ts]
    }

  inline def derived[T <: CallableTool[?]](using
                                           m: Mirror.ProductOf[T]
  ): ToolExecutor[T] = {
    import ToolExecutor.given
    type Parameters = m.MirroredElemLabels
    type Elements   = m.MirroredElemTypes

    val paramNames: List[String] =
      constValueTuple[Parameters].toList.asInstanceOf[List[String]]

    val elementConverters =
      summonConverters[Elements]

    val namedConverters =
      paramNames.zip(elementConverters).toArray

    new ToolExecutor[T] {
      override def execute(args: Map[String, String]): String = {
        val convertedArgs = namedConverters.map { case (name, converter) =>
          converter(args(name))
        }
        m.fromProduct(Tuple.fromArray(convertedArgs)).execute().toString
      }
    }
  }
}
