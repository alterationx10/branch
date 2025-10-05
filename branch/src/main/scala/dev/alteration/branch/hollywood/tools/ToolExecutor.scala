package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonDecoder}

import scala.deriving.Mirror
import scala.language.implicitConversions

trait ToolExecutor[T <: CallableTool[?]] {
  def execute(args: Json): String
}

object ToolExecutor {

  inline def derived[T <: CallableTool[?]](using
      m: Mirror.ProductOf[T],
      decoder: JsonDecoder[T]
  ): ToolExecutor[T] = {
    new ToolExecutor[T] {
      override def execute(args: Json): String = {
        decoder.decode(args) match {
          case scala.util.Success(tool) => tool.execute().toString
          case scala.util.Failure(e) =>
            s"Error decoding tool arguments: ${e.getMessage}"
        }
      }
    }
  }
}
