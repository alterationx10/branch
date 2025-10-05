package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonDecoder, JsonEncoder}

import scala.compiletime.summonInline
import scala.deriving.Mirror
import scala.language.implicitConversions

trait ToolExecutor[T <: CallableTool[?]] {
  def execute(args: Json): Json
}

object ToolExecutor {

  // Match type to extract the result type A from CallableTool[A]
  type ResultType[T <: CallableTool[?]] <: Any = T match {
    case CallableTool[a] => a
  }

  inline def derived[T <: CallableTool[?]](using
      m: Mirror.ProductOf[T],
      decoder: JsonDecoder[T]
  ): ToolExecutor[T] = {
    // Summon the encoder for the result type at compile time
    // TODO move this to a using arg
    val encoder = summonInline[JsonEncoder[ResultType[T]]]

    new ToolExecutor[T] {
      override def execute(args: Json): Json = {
        decoder.decode(args) match {
          case scala.util.Success(tool) =>
            encoder.encode(tool.execute().asInstanceOf[ResultType[T]])
          case scala.util.Failure(e)    =>
            Json.JsonString(s"Error decoding tool arguments: ${e.getMessage}")
        }
      }
    }
  }

}
