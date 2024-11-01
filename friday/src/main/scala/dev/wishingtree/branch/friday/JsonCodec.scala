package dev.wishingtree.branch.friday

import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.Try

trait JsonCodec[A] extends JsonDecoder[A], JsonEncoder[A]

object JsonCodec {

  given JsonCodec[String] with {
    def decode(json: Json): Try[String] =
      Try(json.strVal)

    def encode(a: String): Json =
      Json.JsonString(a)
  }

  given JsonCodec[Int] with {
    def decode(json: Json): Try[Int] =
      Try(json.numVal.toInt)

    def encode(a: Int): Json =
      Json.JsonNumber(a.toDouble)
  }

  private inline def summonCodecs[T <: Tuple]: List[JsonCodec[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[JsonCodec[t]] :: summonCodecs[ts]
    }
  }

  private inline def summonEncoder[T]: JsonEncoder[T] =
    summonInline[JsonEncoder[T]]

  private inline def summonDecoder[T]: JsonDecoder[T] =
    summonInline[JsonDecoder[T]]

  inline def derived[A](using m: Mirror.Of[A]): JsonCodec[A] = {
    inline m match {
      case _: Mirror.SumOf[A]     => error("")
      case p: Mirror.ProductOf[A] =>
        new JsonCodec[A] {
          override def decode(json: Json): Try[A] =
            Try(JsonDecoder.buildJsonProduct(p, json))
          override def encode(a: A): Json         =
            JsonEncoder.buildJsonProduct(a)(using p)
        }
    }

  }
}
