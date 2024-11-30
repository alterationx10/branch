package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.{JsonObject, JsonString}
import dev.wishingtree.branch.macaroni.meta.Summons.summonHigherListOf

import java.time.Instant
import scala.compiletime.*
import scala.deriving.Mirror

/** A type-class for encoding values to Json
  */
trait JsonEncoder[-A] {

  /** Encodes the value to Json
    */
  def encode(a: A): Json

  /** Contramap the encoder
    */
  def contraMap[B](f: B => A): JsonEncoder[B] =
    a => encode(f(a))

  extension [A](a: A) {

    /** Encodes the value to Json using the given encoder
      */
    def toJson(using encoder: JsonEncoder[A]): Json =
      encoder.encode(a)
  }

}

/** A collection of default JsonEncoders
  */
object JsonEncoder {

  /** A JsonEncoder for Strings
    */
  given JsonEncoder[String] with {
    def encode(a: String): Json = Json.JsonString(a)
  }

  /** A JsonEncoder for Doubles
    */
  given JsonEncoder[Double] with {
    def encode(a: Double): Json = Json.JsonNumber(a)
  }

  /** A JsonEncoder for Booleans
    */
  given JsonEncoder[Boolean] with {
    def encode(a: Boolean): Json = Json.JsonBool(a)
  }

  /** A JsonEncoder for Json Arrays
    */
  given JsonEncoder[IndexedSeq[Json]] with {
    def encode(a: IndexedSeq[Json]): Json = Json.JsonArray(a)
  }

  /** A JsonEncoder for Json Objects / Map[String, Json]
    */
  given JsonEncoder[Map[String, Json]] with {
    def encode(a: Map[String, Json]): Json = Json.JsonObject(a)
  }

  /** A JsonEncoder for Ints
    */
  given JsonEncoder[Int] with {
    def encode(a: Int): Json = Json.JsonNumber(a.toDouble)
  }

  /** A JsonEncoder for Instants
    */
  given JsonEncoder[Instant] with {
    def encode(a: Instant): Json = JsonString(a.toString)
  }

  private[friday] inline def buildJsonProduct[A](
      a: A
  )(using m: Mirror.Of[A]): Json = {
    lazy val encoders = summonHigherListOf[m.MirroredElemTypes, JsonEncoder]

    val jsLabels: Iterator[String] =
      a.asInstanceOf[Product].productElementNames

    val jsValues: Iterator[?] =
      a.asInstanceOf[Product].productIterator

    val js: Iterator[(String, Json)] = jsLabels
      .zip(
        jsValues.zip(encoders)
      )
      .map { case (label, (value, encoder)) =>
        label -> encoder.asInstanceOf[JsonEncoder[Any]].encode(value)
      }
    JsonObject(js.toMap)
  }

  /** Derives a JsonEncoder for a product type
    */
  inline given derived[A](using m: Mirror.Of[A]): JsonEncoder[A] = {
    inline m match {
      case p: Mirror.ProductOf[A] =>
        (a: A) => {
          buildJsonProduct(a)
        }
      case s: Mirror.SumOf[A]     =>
        error("Auto derivation of Sum types is not currently supported")
    }
  }

  /** Encodes a value to Json using the given encoder
    */
  inline def encode[T](t: T)(using encoder: JsonEncoder[T]): Json =
    encoder.encode(t)
}
