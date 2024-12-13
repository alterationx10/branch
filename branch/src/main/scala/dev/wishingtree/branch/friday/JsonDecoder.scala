package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.JsonObject
import dev.wishingtree.branch.macaroni.meta.Summons.{
  summonHigherListOf,
  summonListOfValuesAs
}

import java.time.Instant
import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.*

/** A type-class for decoding JSON into a given type
  */
trait JsonDecoder[+A] {

  /** Attempts to decode a JSON value into a given type
    */
  def decode(json: Json): Try[A]

  /** Attempts to decode a JSON string into a given type
    */
  def decode(json: String): Try[A] =
    decode(Json.parse(json).toOption.get)

  /** Map the decoder type to a new JsonDecoder */
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

  /** A JsonDecoder for Instant
    */
  given JsonDecoder[Instant] with {
    def decode(json: Json): Try[Instant] =
      Try(Instant.parse(json.strVal))
  }

  private[friday] inline def buildJsonProduct[A](
      p: Mirror.ProductOf[A],
      b: Json
  ): A = {
    {
      val productLabels       = summonListOfValuesAs[p.MirroredElemLabels, String]
      val decoders            = summonHigherListOf[p.MirroredElemTypes, JsonDecoder]
      val underlying          = b.asInstanceOf[JsonObject].value
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

  /** Derives a JsonDecoder for a product type */
  inline given derived[A](using m: Mirror.Of[A]): JsonDecoder[A] = {
    inline m match {
      case _: Mirror.SumOf[A]     =>
        error(
          "Auto derivation is not supported for Sum types. Please create them explicitly as needed."
        )
      case p: Mirror.ProductOf[A] =>
        (json: Json) =>
          Try {
            buildJsonProduct(p, json)
          }
    }
  }

  /** Attempt to decode a JSON value into a given type
    */
  inline def decode[T](json: Json)(using decoder: JsonDecoder[T]): Try[T] =
    decoder.decode(json)
}
