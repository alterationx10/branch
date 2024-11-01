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
  given JsonEncoder[String] with
    def encode(a: String): Json = Json.JsonString(a)

  given JsonEncoder[Double] with
    def encode(a: Double): Json = Json.JsonNumber(a)

  given JsonEncoder[Boolean] with
    def encode(a: Boolean): Json = Json.JsonBool(a)

  given JsonEncoder[IndexedSeq[Json]] with
    def encode(a: IndexedSeq[Json]): Json = Json.JsonArray(a)

  given JsonEncoder[Map[String, Json]] with
    def encode(a: Map[String, Json]): Json = Json.JsonObject(a)

  given JsonEncoder[Int] with
    def encode(a: Int): Json = Json.JsonNumber(a.toDouble)

  given JsonEncoder[Instant] with
    def encode(a: Instant): Json = JsonString(a.toString)

  private inline def summonEncoders[T <: Tuple]: List[JsonEncoder[?]] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[JsonEncoder[t]] :: summonEncoders[ts]
    }
  }

  private inline def buildJsonProduct[A <: Product](
      a: A
  )(encoders: List[JsonEncoder[?]]): Json = {

    val jsLabels: Iterator[String] =
      a.productElementNames

    val jsValues: Iterator[?] =
      a.productIterator

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
    lazy val encoders = summonEncoders[m.MirroredElemTypes]
    inline m match {
      case p: Mirror.ProductOf[A] =>
        (a: A) => {
          buildJsonProduct(a.asInstanceOf[Product])(encoders)
        }
      case s: Mirror.SumOf[A]     =>
        error("Auto derivation of Sum types is not currently supported")
    }
  }
}

trait JsonDecoder[+A] {

  import Reference.*

  def decode(json: Json): Try[A]

  def decode(json: String): Try[A] =
    decode(Json.defaultParser.run(json).toOption.get)

  def map[B](f: A => B): JsonDecoder[B] =
    json => decode(json).map(f)

}

object JsonDecoder {

  given JsonDecoder[String] with
    def decode(json: Json): Try[String] =
      Try(json.strVal)

  given JsonDecoder[Int] with
    def decode(json: Json): Try[Int] =
      Try(json.numVal.toInt)

  private inline def summonDecoders[A <: Tuple]: List[JsonDecoder[?]] = {
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[JsonDecoder[t]] :: summonDecoders[ts]
  }

  private inline def summonLabels[A <: Tuple]: List[String] = {
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value
          .asInstanceOf[String] :: summonLabels[ts]
  }

  private inline def buildJsonProduct[A](
      p: Mirror.ProductOf[A],
      b: Json
  ): A = {
    {
      val productLabels     = summonLabels[p.MirroredElemLabels]
      val decoders          = summonDecoders[p.MirroredElemTypes]
      val underlying        = b.asInstanceOf[JsonObject].value
      val consArr: Array[?] = productLabels
        .zip(decoders)
        .map { case (label, decoder) =>
          val json = underlying(label)
          decoder.decode(json).get
        }
        .toArray

      p.fromProduct(Tuple.fromArray(consArr))
    }
  }

  inline def derived[A](using m: Mirror.Of[A]): JsonDecoder[A] = {
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
}
