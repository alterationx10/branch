package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.{JsonObject, JsonString}

import java.time.Instant
import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.{Failure, Try}

trait JsonEncoder[-A] {
  def encode(a: A): Json

  def contraMap[B](f: B => A): JsonEncoder[B] =
    a => encode(f(a))

  extension [A](a: A) {
    def toJson(using encoder: JsonEncoder[A]): Json =
      encoder.encode(a)
  }

}

object JsonEncoder {
  given JsonEncoder[String] with {
    def encode(a: String): Json = Json.JsonString(a)
  }

  given JsonEncoder[Double] with {
    def encode(a: Double): Json = Json.JsonNumber(a)
  }

  given JsonEncoder[Boolean] with {
    def encode(a: Boolean): Json = Json.JsonBool(a)
  }

  given JsonEncoder[IndexedSeq[Json]] with {
    def encode(a: IndexedSeq[Json]): Json = Json.JsonArray(a)
  }

  given JsonEncoder[Map[String, Json]] with {
    def encode(a: Map[String, Json]): Json = Json.JsonObject(a)
  }

  given JsonEncoder[Int] with {
    def encode(a: Int): Json = Json.JsonNumber(a.toDouble)
  }

  given JsonEncoder[Instant] with {
    def encode(a: Instant): Json = JsonString(a.toString)
  }

  private inline def summonEncoders[T <: Tuple]: List[JsonEncoder[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[JsonEncoder[t]] :: summonEncoders[ts]
    }
  }

  private[friday] inline def buildJsonProduct[A](
      a: A
  )(using m: Mirror.Of[A]): Json = {
    lazy val encoders = summonEncoders[m.MirroredElemTypes]

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

  inline def derived[A](using m: Mirror.Of[A]): JsonEncoder[A] = {
    inline m match {
      case p: Mirror.ProductOf[A] =>
        (a: A) => {
          buildJsonProduct(a)
        }
      case s: Mirror.SumOf[A]     =>
        error("Auto derivation of Sum types is not currently supported")
    }
  }
}
