package dev.alteration.branch.friday

import Json.JsonObject
import dev.alteration.branch.macaroni.meta.Summons.summonHigherListOf

import java.time.Instant
import scala.collection.mutable
import scala.deriving.Mirror
import scala.util.*
import dev.alteration.branch.macaroni.meta.Summons.summonListOfValuesAs

import scala.language.implicitConversions

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
    Json.parse(json) match {
      case Left(error) =>
        Failure(new Exception(s"Failed to parse JSON: $error"))
      case Right(json) => decode(json)
    }

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

  /** Extension methods for any type with a JsonDecoder */
  extension (json: Json) {

    /** Decodes the JSON value using this decoder
      * @return
      *   a Try containing the decoded value
      */
    def decodeAs[B >: A](using decoder: JsonDecoder[B]): Try[B] =
      decoder.decode(json)
  }

  extension (jsonStr: String) {

    /** Decodes the JSON string using this decoder
      * @return
      *   a Try containing the decoded value
      */
    def decodeAs[B >: A](using decoder: JsonDecoder[B]): Try[B] =
      decoder.decode(jsonStr)
  }
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
    * @param builderFactory
    *   a function that creates a fresh builder for the iterable type
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
      builderFactory: => mutable.Builder[A, F[A]]
  )(using decoder: JsonDecoder[A]): JsonDecoder[F[A]] =
    (json: Json) =>
      Try {
        // Create a fresh builder for each decode to avoid state reuse
        val builder = builderFactory
        var idx     = 0
        json.arrVal.foldLeft(builder)((b, j) => {
          decoder.decode(j) match {
            case Success(value) =>
              idx += 1
              b.addOne(value)
            case Failure(ex)    =>
              throw new Exception(
                s"Failed to decode array element at index $idx: ${ex.getMessage}",
                ex
              )
          }
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

  implicit def optionDecoder[A](using
      aDecoder: JsonDecoder[A]
  ): JsonDecoder[Option[A]] = {
    {
      case Json.JsonNull => Try(Option.empty[A])
      case other         => aDecoder.decode(other).map(Option.apply)
    }
  }

  protected class DerivedJsonDecoder[A](using
      p: Mirror.ProductOf[A],
      decoders: List[JsonDecoder[?]],
      productLabels: List[String]
  ) extends JsonDecoder[A] {
    def decode(json: Json): Try[A] = {
      Try {
        val underlying          = json.asInstanceOf[JsonObject].value
        val consArr: Array[Any] = productLabels
          .zip(decoders)
          .map { case (label, decoder) =>
            val jsonValue = underlying.getOrElse(label, Json.JsonNull)
            decoder.decode(jsonValue) match {
              case Success(value) => value
              case Failure(ex)    =>
                throw new Exception(
                  s"Failed to decode field '$label': ${ex.getMessage}",
                  ex
                )
            }
          }
          .toArray

        p.fromProduct(Tuple.fromArray(consArr))
      }
    }
  }

  /** Decoder for sum types (sealed traits/enums).
    *
    * Decodes ADTs using a tagged union with a "type" field:
    *   - Case objects: { "type": "Increment" }
    *   - Case classes: { "type": "SetCount", "value": 42 }
    */
  protected class DerivedSumJsonDecoder[A](using
      decoders: List[JsonDecoder[?]],
      typeLabels: List[String]
  ) extends JsonDecoder[A] {
    def decode(json: Json): Try[A] = {
      Try {
        val underlying = json.asInstanceOf[JsonObject].value
        val typeName   = underlying.get("type") match {
          case Some(Json.JsonString(name)) => name
          case _                           =>
            throw new Exception("Missing or invalid 'type' field in sum type")
        }

        // Find the decoder for this type
        typeLabels.indexOf(typeName) match {
          case -1  =>
            throw new Exception(
              s"Unknown type '$typeName' for sum type. Expected one of: ${typeLabels.mkString(", ")}"
            )
          case idx =>
            val decoder         = decoders(idx).asInstanceOf[JsonDecoder[Any]]
            // Remove the "type" field before decoding the value
            val remainingFields = underlying - "type"
            val valueJson       = if (remainingFields.isEmpty) {
              // Case object with no fields - use empty object
              JsonObject(Map.empty)
            } else {
              // Case class with fields
              JsonObject(remainingFields)
            }
            // Decode and convert to the target type
            decoder.decode(valueJson).get.asInstanceOf[A]
        }
      }
    }
  }

  inline given derived[A](using m: Mirror.Of[A]): JsonDecoder[A] = {
    inline m match {
      case s: Mirror.SumOf[A]     =>
        new DerivedSumJsonDecoder[A](using
          summonHigherListOf[s.MirroredElemTypes, JsonDecoder],
          summonListOfValuesAs[s.MirroredElemLabels, String]
        )
      case p: Mirror.ProductOf[A] =>
        new DerivedJsonDecoder[A](using
          p,
          summonHigherListOf[p.MirroredElemTypes, JsonDecoder],
          summonListOfValuesAs[p.MirroredElemLabels, String]
        )
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
