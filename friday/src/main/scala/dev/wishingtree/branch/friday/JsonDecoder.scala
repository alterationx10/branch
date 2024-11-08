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

trait JsonDecoder[+A] {

  def decode(json: Json): Try[A]

  def decode(json: String): Try[A] =
    decode(Json.parse(json).toOption.get)

  def map[B](f: A => B): JsonDecoder[B] =
    json => decode(json).map(f)

}

object JsonDecoder {

  given JsonDecoder[String] with {
    def decode(json: Json): Try[String] =
      Try(json.strVal)
  }

  given JsonDecoder[Double] with {
    def decode(json: Json): Try[Double] =
      Try(json.numVal)
  }

  given JsonDecoder[Boolean] with {
    def decode(json: Json): Try[Boolean] =
      Try(json.boolVal)
  }

  given JsonDecoder[Int] with {
    def decode(json: Json): Try[Int] =
      Try(json.numVal.toInt)
  }

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
