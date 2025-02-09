package dev.wishingtree.branch.friday

import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.Try

/** A type-class for encoding and decoding values to and from Json
  * @tparam A
  *   the type of the value to encode/decode
  */
trait JsonCodec[A] { self =>
  given encoder: JsonEncoder[A]
  given decoder: JsonDecoder[A]

  def encode(a: A): Json = encoder.encode(a)
  def decode(json: Json): Try[A] = decoder.decode(json)

  /** Transform the codec from one type to another, by providing the functions
    * needed to map/contraMap the underlying decoder/encoders.
    * @param f
    *   the function to map the decoded value
    * @param g
    *   the function to contraMap the encoded value
    * @tparam B
    *   the new type to transform to
    * @return
    *   a new JsonCodec for the transformed type
    */
  def transform[B](f: A => B)(g: B => A): JsonCodec[B] =
    new JsonCodec[B] {
      def encoder: JsonEncoder[B] = self.encoder.contraMap(g)
      def decoder: JsonDecoder[B] = self.decoder.map(f)
    }

  /** Transform the codec from one type to another, with better type safety
    */
  def bimap[B](f: A => B)(g: B => A): JsonCodec[B] = transform(f)(g)

  /** Map the decoded value while preserving the encoder
    */
  def map[B](f: A => B)(g: B => A): JsonCodec[B] = transform(f)(g)

}

object JsonCodec {

  /** Creates a JsonCodec for a given type using the provided JsonEncoder and
    * JsonDecoder.
    * @param encoder
    *   the JsonEncoder for the type
    * @param decoder
    *   the JsonDecoder for the type
    * @tparam A
    *   the type of the value to encode/decode
    * @return
    *   a new JsonCodec for the given type
    */
  def apply[A](using e: JsonEncoder[A], d: JsonDecoder[A]): JsonCodec[A] =
    new JsonCodec[A] {
      def encoder: JsonEncoder[A] = e
      def decoder: JsonDecoder[A] = d
    }

  /** Derives a JsonCodec for a given type using the given product type
    * @param m
    *   the Mirror for the type
    * @tparam A
    *   the type of the value to encode/decode
    * @return
    *   a new JsonCodec for the derived type
    */
  inline given derived[A](using m: Mirror.Of[A]): JsonCodec[A] = {
    inline m match {
      case _: Mirror.SumOf[A]     =>
        error("Auto deriving sum types is not currently supported")
      case p: Mirror.ProductOf[A] =>
        new JsonCodec[A] {
          def encoder: JsonEncoder[A] = JsonEncoder.derived[A]
          def decoder: JsonDecoder[A] = JsonDecoder.derived[A]
        }
    }
  }

  // Type aliases for cleaner signatures
  type Decoder[A] = Json => Try[A]
  type Encoder[A] = A => Json

  /** Creates a JsonCodec from explicit encode/decode functions
    */
  def from[A](decode: Decoder[A], encode: Encoder[A]): JsonCodec[A] = 
    new JsonCodec[A] {
      def encoder: JsonEncoder[A] = JsonEncoder.from(this.encode)
      def decoder: JsonDecoder[A] = JsonDecoder.from(this.decode)
    }

}
