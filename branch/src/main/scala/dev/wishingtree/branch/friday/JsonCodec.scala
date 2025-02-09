package dev.wishingtree.branch.friday

import scala.deriving.Mirror
import scala.util.Try

/** A type-class for encoding and decoding values to and from Json
  * @tparam A
  *   the type of the value to encode/decode
  */
trait JsonCodec[A] { self =>
  given encoder: JsonEncoder[A]
  given decoder: JsonDecoder[A]

  def encode(a: A): Json         = encoder.encode(a)
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

  def decode(json: String): Try[A] = 
    Json.parse(json)
    .left.map(e => new RuntimeException(s"Failed to parse json: $json"))
    .toTry.flatMap(decode)
    
  extension (a: A) {
    def toJson: Json = encode(a)
  }
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

  protected class DerivedCodec[A](
      val encoder: JsonEncoder[A],
      val decoder: JsonDecoder[A]
  ) extends JsonCodec[A]

  inline given derived[A](using m: Mirror.Of[A]): JsonCodec[A] = {
    val encoder = JsonEncoder.derived[A]
    val decoder = JsonDecoder.derived[A]
    new DerivedCodec[A](encoder, decoder)
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
