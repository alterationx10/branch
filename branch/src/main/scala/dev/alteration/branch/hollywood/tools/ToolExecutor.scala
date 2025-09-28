package dev.alteration.branch.hollywood.tools

import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.compiletime.*

trait ToolExecutor[T <: Tool[?]] {
  def execute(
      args: Map[String, String]
  ): String
}

object ToolExecutor {

  private inline def summonConverters[A <: Tuple]: List[Conversion[String, ?]] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[Conversion[String, t]]
          .asInstanceOf[Conversion[String, ?]] :: summonConverters[ts]
    }

  inline def derived[T <: Tool[?]: ClassTag](using
      m: Mirror.ProductOf[T]
  ): ToolExecutor[T] = {
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
          println("converting " + name + " with " + args(name))
          converter.convert(args(name))
        }
        m.fromProduct(Tuple.fromArray(convertedArgs)).execute().toString
      }
    }
  }
}

import scala.language.implicitConversions

object Fart extends App {


  case class Turd(a: Int, b: String) extends Tool[Int] {
    override def execute(): Int = a + b.toInt
  }

  given Conversion[String, Int]    = (s: String) => s.toInt
  given Conversion[String, String] = (s: String) => s
  val thingy                       = ToolExecutor.derived[Turd]
  println(thingy.execute(Map("a" -> "1", "b" -> "2")))
}
