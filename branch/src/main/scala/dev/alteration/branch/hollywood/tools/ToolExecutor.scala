package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonDecoder, JsonEncoder}

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
      decoder: JsonDecoder[T],
      encoder: JsonEncoder[ResultType[T]]
  ): ToolExecutor[T] = {
    new ToolExecutor[T] {
      override def execute(args: Json): Json = {
        decoder.decode(args) match {
          case scala.util.Success(tool) =>
            tool.execute() match {
              case scala.util.Success(result) =>
                encoder.encode(result.asInstanceOf[ResultType[T]])
              case scala.util.Failure(e) =>
                Json.JsonString(s"Error executing tool: ${e.getMessage}")
            }
          case scala.util.Failure(e)    =>
            Json.JsonString(s"Error decoding tool arguments: ${e.getMessage}")
        }
      }
    }
  }

}
