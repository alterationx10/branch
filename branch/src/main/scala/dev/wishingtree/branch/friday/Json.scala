package dev.wishingtree.branch.friday

import dev.wishingtree.branch.macaroni.parsers.Reference.Parser
import dev.wishingtree.branch.macaroni.parsers.{ParseError, Parsers, Reference}

import scala.annotation.targetName
import scala.util.Try

/** A JSON AST
  */
enum Json {

  /** A JSON null value
    */
  case JsonNull

  /** A JSON boolean value
    */
  case JsonBool(value: Boolean)

  /** A JSON number value
    */
  case JsonNumber(value: Double)

  /** A JSON string value
    */
  case JsonString(value: String)

  /** A JSON array value
    */
  case JsonArray(value: IndexedSeq[Json])

  /** A JSON object value
    */
  case JsonObject(value: Map[String, Json])
}

object Json {

  /** Creates a JSON object from the given fields
    */
  def obj(fields: (String, Json)*): JsonObject = {
    JsonObject(fields.toMap)
  }

  /** Encode the vale to JSON, returning [[JsonNull]] on failure */
  inline def jsonOrNull[A](a: => A)(using encoder: JsonEncoder[A]): Json = {
    Try(encoder.encode(a)).getOrElse(JsonNull)
  }

  /** Encode a [[StackTraceElement]] to Json */
  def stackTraceElement(ste: StackTraceElement): Json = {
    Json.obj(
      "fileName"   -> jsonOrNull(ste.getFileName),
      "className"  -> jsonOrNull(ste.getClassName),
      "methodName" -> jsonOrNull(ste.getMethodName),
      "lineNumber" -> jsonOrNull(ste.getLineNumber)
    )
  }

  /** Attempt to encode a [[Throwable]] to Json, returning [[JsonNull]] on
    * failure
    */
  def throwable[E <: Throwable](e: E): Json = Try {
    Json.obj(
      "message"    -> jsonOrNull(e.getMessage),
      "cause"      -> Json.throwable(e.getCause),
      "suppressed" -> JsonArray(e.getSuppressed.map(e => Json.throwable(e))),
      "stackTrace" -> JsonArray(
        e.getStackTrace.map(e => Json.stackTraceElement(e))
      )
    )
  }.getOrElse(JsonNull)

  extension (j: Json) {

    /** Optionally get a field from a JSON object if present */
    @targetName("exists")
    def ?(field: String): Option[Json] = j match {
      case JsonObject(value) => value.get(field)
      case _                 => None
    }

    /** Attempt to access the value as a string
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonString]]
      */
    infix def strVal: String = j match {
      case JsonString(str) => str
      case _               => throw IllegalArgumentException("Not a string")
    }

    /** Option wrapped version of [[strVal]] */
    infix def strOpt: Option[String] =
      Try(strVal).toOption

    /** Attempt to access the value as a number
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonNumber]]
      */
    infix def numVal: Double = j match {
      case JsonNumber(num) => num
      case _               => throw IllegalArgumentException("Not a number")
    }

    /** Option wrapped version of [[numVal]] */
    infix def numOpt: Option[Double] =
      Try(numVal).toOption

    /** Attempt to access the value as a boolean
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonBool]]
      */
    infix def boolVal: Boolean = j match {
      case JsonBool(bool) => bool
      case _              => throw IllegalArgumentException("Not a boolean")
    }

    /** Option wrapped version of [[boolVal]] */
    infix def boolOpt: Option[Boolean] =
      Try(boolVal).toOption

    /** Attempt to access the value as an array
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonArray]]
      */
    infix def arrVal: IndexedSeq[Json] = j match {
      case JsonArray(arr) => arr
      case _              => throw IllegalArgumentException("Not an array")
    }

    /** Option wrapped version of [[arrVal]] */
    infix def arrOpt: Option[IndexedSeq[Json]] =
      Try(arrVal).toOption

    /** Attempt to access the value as an object
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonObject]]
      */
    infix def objVal: Map[String, Json] = j match {
      case JsonObject(obj) => obj
      case _               => throw IllegalArgumentException("Not an object")
    }

    /** Option wrapped version of [[objVal]] */
    infix def objOpt: Option[Map[String, Json]] =
      Try(objVal).toOption

    /** Convert the JSON value to a string representation */
    infix def toJsonString: String = j match {
      case JsonNull          => "null"
      case JsonBool(value)   => value.toString
      case JsonNumber(value) => value.toString
      case JsonString(value) => "\"" + value + "\""
      case JsonArray(value)  =>
        "[" + value.map(_.toJsonString).mkString(", ") + "]"
      case JsonObject(value) =>
        "{" + value
          .map { case (k, v) => "\"" + k + "\": " + v.toJsonString }
          .mkString(", ") + "}"
    }

  }

  extension (jo: Option[Json]) {

    /** Get a field from a JSON object if present */
    @targetName("exists")
    def ?(field: String): Option[Json] =
      jo.flatMap(_ ? field)

    /** Attempt to access the value as a string */
    infix def strOpt: Option[String] =
      jo.flatMap(_.strOpt)

    /** Attempt to access the value as a number */
    infix def numOpt: Option[Double] =
      jo.flatMap(_.numOpt)

    /** Attempt to access the value as a boolean */
    infix def boolOpt: Option[Boolean] =
      jo.flatMap(_.boolOpt)

    /** Attempt to access the value as an array */
    infix def arrOpt: Option[IndexedSeq[Json]] =
      jo.flatMap(_.arrOpt)

    /** Attempt to access the value as an object */
    infix def objOpt: Option[Map[String, Json]] =
      jo.flatMap(_.objOpt)

    /** Convert the JSON value to a string representation */
    infix def toJsonString: Option[String] =
      jo.map(_.toJsonString)

  }

  private def parser[Parser[+_]](P: Parsers[Parser]): Parser[Json] = {
    import P.*

    def token(s: String) = string(s).token

    def keyVal: Parser[(String, Json)] =
      escapedQuoted ** (token(":") *> value)

    def obj: Parser[Json] = {
      token("{") *> keyVal.sep(token(",")).map { kvs =>
        JsonObject(kvs.toMap)
      } <* token("}")
    }.scope("object")

    def array: Parser[Json] = {
      token("[") *>
        value.sep(token(",")).map(vs => JsonArray(vs.toIndexedSeq))
        <* token("]")
    }.scope("array")

    def literal: Parser[Json] = {
      token("null").as(JsonNull) |
        double.map(JsonNumber.apply) |
        escapedQuoted.map(JsonString.apply) |
        token("true").as(JsonBool(true)) |
        token("false").as(JsonBool(false))
    }

    def value: Parser[Json] = literal | obj | array

    (whitespace *> (obj | array)).root
  }

  private val defaultParser: Parser[Json] =
    parser(Reference)

  import Reference.*

  /** Attempt to parse a JSON string into a JSON value */
  def parse(json: String): Either[ParseError, Json] =
    defaultParser.run(json)

  /** Attempt to decode a JSON string into a JSON value */
  def decode[A](json: Json)(using JsonDecoder[A]): Try[A] =
    summon[JsonDecoder[A]].decode(json)

  /** Attempt to decode a JSON string into a JSON value */
  def decode[A](json: String)(using JsonDecoder[A]): Try[A] =
    parse(json).left
      .map(e => new Exception(e.toString))
      .toTry
      .flatMap(decode[A])

  /** Encode a value to JSON */
  def encode[A](a: A)(using JsonEncoder[A]): Json =
    summon[JsonEncoder[A]].encode(a)
}
