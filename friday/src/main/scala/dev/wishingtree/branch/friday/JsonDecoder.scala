package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.JsonObject

import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.*

trait JsonDecoder[+A] {
  
  def decode(json: Json): Try[A]

  def decode(json: String): Try[A] =
    decode(Json.parse(json).toOption.get)

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

  private[friday] inline def summonLabels[A <: Tuple]: List[String] = {
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        summonInline[ValueOf[t]].value
          .asInstanceOf[String] :: summonLabels[ts]
  }

  private[friday] inline def buildJsonProduct[A](
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
