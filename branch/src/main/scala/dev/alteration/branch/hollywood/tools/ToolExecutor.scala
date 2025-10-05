package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonDecoder}

import scala.deriving.Mirror
import scala.language.implicitConversions

trait ToolExecutor[T <: CallableTool[?]] {
  def execute(args: Json): Json
}

object ToolExecutor {

  inline def derived[T <: CallableTool[?]](using
      m: Mirror.ProductOf[T],
      decoder: JsonDecoder[T]
  ): ToolExecutor[T] = {
    new ToolExecutor[T] {
      override def execute(args: Json): Json = {
        decoder.decode(args) match {
          case scala.util.Success(tool) =>
            encodeResult(tool.execute())
          case scala.util.Failure(e) =>
            Json.JsonString(s"Error decoding tool arguments: ${e.getMessage}")
        }
      }
    }
  }

  private def encodeResult[A](result: A): Json = {
    result match {
      case s: String  => Json.JsonString(s)
      case i: Int     => Json.JsonNumber(i.toDouble)
      case l: Long    => Json.JsonNumber(l.toDouble)
      case d: Double  => Json.JsonNumber(d)
      case f: Float   => Json.JsonNumber(f.toDouble)
      case b: Boolean => Json.JsonBool(b)
      case null       => Json.JsonNull
      case other      =>
        // For other types, try to find a JsonEncoder
        // If not available, fall back to toString wrapped in JsonString
        Json.JsonString(other.toString)
    }
  }
}
