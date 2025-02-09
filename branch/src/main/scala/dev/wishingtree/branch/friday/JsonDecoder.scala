package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.JsonObject
import dev.wishingtree.branch.macaroni.meta.Summons.{
  summonHigherListOf,
  summonListOfValuesAs
}

import java.time.Instant
import scala.collection.*
import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.*

/** A type-class for decoding JSON into a given type
  * @tparam A
  *   the type of the value to decode
  */
trait JsonDecoder[+A] {

  /** Attempts to decode a JSON value into a given type
    * @param json
    *   the JSON value to decode
    * @return
    *   a Try containing the decoded value or an exception on failure
    */
  def decode(json: Json): Try[A]

  /** Attempts to decode a JSON string into a given type
    * @param json
    *   the JSON string to decode
    * @return
    *   a Try containing the decoded value or an exception on failure
    */
  def decode(json: String): Try[A] =
    decode(Json.parse(json).toOption.get)

  /** Map the decoder type to a new JsonDecoder
    * @param f
    *   the function to map the decoded value
    * @tparam B
    *   the new type to map to
    * @return
    *   a new JsonDecoder for the mapped type
    */
  def map[B](f: A => B): JsonDecoder[B] =
    json => decode(json).map(f)

}

/** A collection of default JsonDecoders
  */
object JsonDecoder {

  /** A JsonDecoder for Json
    */
  given JsonDecoder[Json] with {
    def decode(json: Json): Try[Json] =
      Try(json)
  }

  /** A JsonDecoder for JsonObject
    */
  given JsonDecoder[JsonObject] with {
    def decode(json: Json): Try[JsonObject] =
      Try(json.asInstanceOf[JsonObject])
  }

  /** A JsonDecoder for Strings
    */
  given JsonDecoder[String] with {
    def decode(json: Json): Try[String] =
      Try(json.strVal)
  }

  /** A JsonDecoder for Doubles
    */
  given JsonDecoder[Double] with {
    def decode(json: Json): Try[Double] =
      Try(json.numVal)
  }

  /** A JsonDecoder for Booleans
    */
  given JsonDecoder[Boolean] with {
    def decode(json: Json): Try[Boolean] =
      Try(json.boolVal)
  }

  /** A JsonDecoder for Ints
    */
  given JsonDecoder[Int] with {
    def decode(json: Json): Try[Int] =
      Try(json.numVal.toInt)
  }

  /** A JsonDecoder for Longs
    */
  given JsonDecoder[Long] with {
    def decode(json: Json): Try[Long] =
      Try(json.numVal.toLong)
  }

  /** A JsonDecoder for Instant
    */
  given JsonDecoder[Instant] with {
    def decode(json: Json): Try[Instant] =
      Try(Instant.parse(json.strVal))
  }

  /** Creates a JsonDecoder for an iterable type
    * @param builder
    *   the builder for the iterable type
    * @param decoder
    *   the JsonDecoder for the element type
    * @tparam A
    *   the element type
    * @tparam F
    *   the iterable type
    * @return
    *   a new JsonDecoder for the iterable type
    */
  private[friday] def iterableDecoder[A, F[_]](
      builder: mutable.Builder[A, F[A]]
  )(using decoder: JsonDecoder[A]): JsonDecoder[F[A]] =
    (json: Json) =>
      Try {
        json.arrVal.foldLeft(builder)((b, j) => {
          b.addOne(decoder.decode(j).get)
        })
        builder.result()
      }

  /** A JsonDecoder for Sequences
    * @tparam A
    *   the element type
    * @return
    *   a new JsonDecoder for Sequences
    */
  implicit def seqDecoder[A: JsonDecoder]: JsonDecoder[Seq[A]] =
    iterableDecoder[A, Seq](Seq.newBuilder[A])

  /** A JsonDecoder for Lists
    * @tparam A
    *   the element type
    * @return
    *   a new JsonDecoder for Lists
    */
  implicit def listDecoder[A: JsonDecoder]: JsonDecoder[List[A]] =
    iterableDecoder[A, List](List.newBuilder[A])

  /** A JsonDecoder for IndexedSequences
    * @tparam A
    *   the element type
    * @return
    *   a new JsonDecoder for IndexedSequences
    */
  implicit def indexedSeqDecoder[A: JsonDecoder]: JsonDecoder[IndexedSeq[A]] =
    iterableDecoder[A, IndexedSeq](IndexedSeq.newBuilder[A])

  /** A JsonDecoder for Sets
    * @tparam A
    *   the element type
    * @return
    *   a new JsonDecoder for Sets
    */
  implicit def setDecoder[A: JsonDecoder]: JsonDecoder[Set[A]] =
    iterableDecoder[A, Set](Set.newBuilder[A])

  /** A JsonDecoder for Vectors
    * @tparam A
    *   the element type
    * @return
    *   a new JsonDecoder for Vectors
    */
  implicit def vectorDecoder[A: JsonDecoder]: JsonDecoder[Vector[A]] =
    iterableDecoder[A, Vector](Vector.newBuilder[A])

  /** Derives a JsonDecoder for a product type
    * @param m
    *   the Mirror for the product type
    * @tparam A
    *   the product type
    * @return
    *   a new JsonDecoder for the product type
    */
  inline given derived[A](using m: Mirror.Of[A]): JsonDecoder[A] = {
    inline m match {
      case _: Mirror.SumOf[A]     =>
        error(
          "Auto derivation is not supported for Sum types. Please create them explicitly as needed."
        )
      case p: Mirror.ProductOf[A] =>
        (json: Json) =>
          Try {
            val productLabels       =
              constValue[p.MirroredElemLabels].toList.asInstanceOf[List[String]]
            val decoders            = summonHigherListOf[p.MirroredElemTypes, JsonDecoder]
            val underlying          = json.asInstanceOf[JsonObject].value
            val consArr: Array[Any] = productLabels
              .zip(decoders)
              .map { case (label, decoder) =>
                val json = underlying(label)
                decoder.decode(json).get
              }
              .toArray

            p.fromProduct(Tuple.fromArray(consArr))
          }
    }
  }

  /** Attempt to decode a JSON value into a given type
    * @param json
    *   the JSON value to decode
    * @param decoder
    *   the JsonDecoder for the type
    * @tparam T
    *   the type to decode
    * @return
    *   a Try containing the decoded value or an exception on failure
    */
  inline def decode[T](json: Json)(using decoder: JsonDecoder[T]): Try[T] =
    decoder.decode(json)

  def from[A](f: Json => Try[A]): JsonDecoder[A] = (json: Json) => f(json)
}
