package dev.wishingtree.branch.friday

import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.Try

trait JsonCodec[A] extends JsonDecoder[A], JsonEncoder[A]

object JsonCodec {

  def apply[A](using
      encoder: JsonEncoder[A],
      decoder: JsonDecoder[A]
  ): JsonCodec[A] =
    new JsonCodec[A] {
      override def decode(json: Json): Try[A] = decoder.decode(json)

      override def encode(a: A): Json = encoder.encode(a)
    }

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
