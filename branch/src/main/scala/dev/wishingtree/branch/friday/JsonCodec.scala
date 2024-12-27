package dev.wishingtree.branch.friday

import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.Try

/** A type-class for encoding and decoding values to and from Json
  */
trait JsonCodec[A] extends JsonDecoder[A], JsonEncoder[A] { self =>

  /** Transform the codec from one type to another, by providing the functions
    * needed to map/contraMap the underlying decoder/encoders.
    */
  def transform[B](f: A => B)(g: B => A): JsonCodec[B] =
    new JsonCodec[B] {
      override def decode(json: Json): Try[B] = self.decode(json).map(f)
      override def encode(b: B): Json         = self.encode(g(b))
    }

}

object JsonCodec {

  /** Creates a JsonCodec for a given type using the provided JsonEncoder and
    * JsonDecoder.
    */
  def apply[A](using
      encoder: JsonEncoder[A],
      decoder: JsonDecoder[A]
  ): JsonCodec[A] =
    new JsonCodec[A] {
      override def decode(json: Json): Try[A] = decoder.decode(json)

      override def encode(a: A): Json = encoder.encode(a)
    }

  /** Derives a JsonCodec for a given type using the given product type
    */
  inline given derived[A](using m: Mirror.Of[A]): JsonCodec[A] = {
    inline m match {
      case _: Mirror.SumOf[A]     =>
        error("Auto deriving sum types is not currently supported")
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
