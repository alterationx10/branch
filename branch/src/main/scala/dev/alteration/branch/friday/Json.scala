package dev.alteration.branch.friday

import dev.alteration.branch.macaroni.parsers.{ParseError, Parsers, Reference}

import scala.annotation.targetName
import scala.util.Try

/** A JSON Abstract Syntax Tree (AST)
  */
enum Json {

  /** A JSON null value
    */
  case JsonNull

  /** A JSON boolean value
    * @param value
    *   the boolean value
    */
  case JsonBool(value: Boolean)

  /** A JSON number value
    * @param value
    *   the number value
    */
  case JsonNumber(value: Double)

  /** A JSON string value
    * @param value
    *   the string value
    */
  case JsonString(value: String)

  /** A JSON array value
    * @param value
    *   the array of JSON values
    */
  case JsonArray(value: IndexedSeq[Json])

  /** A JSON object value
    * @param value
    *   the map of string keys to JSON values
    */
  case JsonObject(value: Map[String, Json])

  override def toString: String = toJsonString

  def toJsonString: String = this match {
    case JsonNull           => "null"
    case JsonBool(value)    => value.toString
    case JsonNumber(value)  =>
      if (value % 1 == 0) value.toInt.toString else value.toString
    case JsonString(value)  =>
      value match {
        case null => "null"
        case _    => s""""${escapeString(value)}""""
      }
    case JsonArray(values)  => values.map(_.toJsonString).mkString("[", ",", "]")
    case JsonObject(values) =>
      values
        .map { case (k, v) => s""""${escapeString(k)}":${v.toJsonString}""" }
        .mkString("{", ",", "}")
  }

  private def escapeString(s: String): String = {
    s.flatMap {
      case '"'          => "\\\""
      case '\\'         => "\\\\"
      case '\b'         => "\\b"
      case '\f'         => "\\f"
      case '\n'         => "\\n"
      case '\r'         => "\\r"
      case '\t'         => "\\t"
      case c if c < ' ' => f"\\u${c.toInt}%04x"
      case c            => c.toString
    }
  }
}

object Json {

  /** Creates a JSON object from the given fields
    * @param fields
    *   the fields to include in the JSON object
    * @return
    *   a JSON object containing the given fields
    */
  def obj(fields: (String, Json)*): JsonObject = {
    JsonObject(fields.toMap)
  }

  /** Creates a JSON array from the given values
    * @param values
    *   the values to include in the JSON array
    * @return
    *   a JSON array containing the given values
    */
  def arr(values: Json*): JsonArray = {
    JsonArray(values.toIndexedSeq)
  }

  /** Encode the value to JSON, returning [[JsonNull]] on failure
    * @param a
    *   the value to encode
    * @param encoder
    *   the encoder to use for encoding the value
    * @tparam A
    *   the type of the value to encode
    * @return
    *   the encoded JSON value or [[JsonNull]] on failure
    */
  inline def jsonOrNull[A](a: => A)(using encoder: JsonEncoder[A]): Json = {
    Try(encoder.encode(a)).getOrElse(JsonNull)
  }

  /** Encode a [[StackTraceElement]] to JSON
    * @param ste
    *   the stack trace element to encode
    * @return
    *   the encoded JSON object representing the stack trace element
    */
  def stackTraceElement(ste: StackTraceElement): Json = {
    Json.obj(
      "fileName"   -> jsonOrNull(ste.getFileName),
      "className"  -> jsonOrNull(ste.getClassName),
      "methodName" -> jsonOrNull(ste.getMethodName),
      "lineNumber" -> jsonOrNull(ste.getLineNumber)
    )
  }

  /** Attempt to encode a [[Throwable]] to JSON, returning [[JsonNull]] on
    * failure
    * @param e
    *   the throwable to encode
    * @tparam E
    *   the type of the throwable
    * @return
    *   the encoded JSON object representing the throwable or [[JsonNull]] on
    *   failure
    */
  def throwable[E <: Throwable](e: E): Json = Try {
    Json.obj(
      "message"    -> jsonOrNull(e.getMessage),
      "cause"      -> Json.throwable(e.getCause),
      "suppressed" -> JsonArray(
        e.getSuppressed.toIndexedSeq.map(e => Json.throwable(e))
      ),
      "stackTrace" -> JsonArray(
        e.getStackTrace.toIndexedSeq.map(e => Json.stackTraceElement(e))
      )
    )
  }.getOrElse(JsonNull)

  extension (j: Json) {

    /** Recursively remove all null values from JSON objects
      * @return
      *   a new JSON value with all JsonNull values removed from objects
      */
    def removeNulls(): Json = j match {
      case JsonObject(values) =>
        JsonObject(
          values
            .filter(_._2 != JsonNull)
            .map { case (k, v) => k -> v.removeNulls() }
        )
      case JsonArray(values) => JsonArray(values.map(_.removeNulls()))
      case other             => other
    }

    /** Optionally get a field from a JSON object if present
      * @param field
      *   the field name to retrieve
      * @return
      *   an option containing the JSON value if the field is present, otherwise
      *   None
      */
    @targetName("exists")
    def ?(field: String): Option[Json] = j match {
      case JsonObject(value) => value.get(field)
      case _                 => None
    }

    /** Attempt to access the value as a string
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonString]]
      * @return
      *   the string value
      */
    infix def strVal: String = j match {
      case JsonString(str) => str
      case _               => throw IllegalArgumentException("Not a string")
    }

    /** Option wrapped version of [[strVal]]
      * @return
      *   an option containing the string value if present, otherwise None
      */
    infix def strOpt: Option[String] =
      Try(strVal).toOption

    /** Attempt to access the value as a number
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonNumber]]
      * @return
      *   the number value
      */
    infix def numVal: Double = j match {
      case JsonNumber(num) => num
      case _               => throw IllegalArgumentException("Not a number")
    }

    /** Option wrapped version of [[numVal]]
      * @return
      *   an option containing the number value if present, otherwise None
      */
    infix def numOpt: Option[Double] =
      Try(numVal).toOption

    /** Attempt to access the value as a boolean
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonBool]]
      * @return
      *   the boolean value
      */
    infix def boolVal: Boolean = j match {
      case JsonBool(bool) => bool
      case _              => throw IllegalArgumentException("Not a boolean")
    }

    /** Option wrapped version of [[boolVal]]
      * @return
      *   an option containing the boolean value if present, otherwise None
      */
    infix def boolOpt: Option[Boolean] =
      Try(boolVal).toOption

    /** Attempt to access the value as an array
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonArray]]
      * @return
      *   the array of JSON values
      */
    infix def arrVal: IndexedSeq[Json] = j match {
      case JsonArray(arr) => arr
      case _              => throw IllegalArgumentException("Not an array")
    }

    /** Option wrapped version of [[arrVal]]
      * @return
      *   an option containing the array of JSON values if present, otherwise
      *   None
      */
    infix def arrOpt: Option[IndexedSeq[Json]] =
      Try(arrVal).toOption

    /** Attempt to access the value as an object
      * @throws IllegalArgumentException
      *   if the value is not a [[JsonObject]]
      * @return
      *   the map of string keys to JSON values
      */
    infix def objVal: Map[String, Json] = j match {
      case JsonObject(obj) => obj
      case _               => throw IllegalArgumentException("Not an object")
    }

    /** Option wrapped version of [[objVal]]
      * @return
      *   an option containing the map of string keys to JSON values if present,
      *   otherwise None
      */
    infix def objOpt: Option[Map[String, Json]] =
      Try(objVal).toOption

  }

  extension (jo: Option[Json]) {

    /** Get a field from a JSON object if present
      * @param field
      *   the field name to retrieve
      * @return
      *   an option containing the JSON value if the field is present, otherwise
      *   None
      */
    @targetName("exists")
    def ?(field: String): Option[Json] =
      jo.flatMap(_ ? field)

    /** Attempt to access the value as a string
      * @return
      *   an option containing the string value if present, otherwise None
      */
    infix def strOpt: Option[String] =
      jo.flatMap(_.strOpt)

    /** Attempt to access the value as a number
      * @return
      *   an option containing the number value if present, otherwise None
      */
    infix def numOpt: Option[Double] =
      jo.flatMap(_.numOpt)

    /** Attempt to access the value as a boolean
      * @return
      *   an option containing the boolean value if present, otherwise None
      */
    infix def boolOpt: Option[Boolean] =
      jo.flatMap(_.boolOpt)

    /** Attempt to access the value as an array
      * @return
      *   an option containing the array of JSON values if present, otherwise
      *   None
      */
    infix def arrOpt: Option[IndexedSeq[Json]] =
      jo.flatMap(_.arrOpt)

    /** Attempt to access the value as an object
      * @return
      *   an option containing the map of string keys to JSON values if present,
      *   otherwise None
      */
    infix def objOpt: Option[Map[String, Json]] =
      jo.flatMap(_.objOpt)

    /** Convert the JSON value to a string representation
      * @return
      *   an option containing the string representation of the JSON value if
      *   present, otherwise None
      */
    infix def toJsonString: Option[String] =
      jo.map(_.toJsonString)

  }

  /** Create a JSON parser using the given parser implementation
    * @param P
    *   the parser implementation to use
    * @tparam Parser
    *   the type of the parser
    * @return
    *   a parser for JSON values
    */
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

  private val defaultParser: Reference.Parser[Json] =
    parser(Reference)

  import Reference.*

  /** Attempt to parse a JSON string into a JSON value
    * @param json
    *   the JSON string to parse
    * @return
    *   either a parse error or the parsed JSON value
    */
  def parse(json: String): Either[ParseError, Json] =
    defaultParser.run(json)

  /** Attempt to decode a JSON value into a value of type A
    * @param json
    *   the JSON value to decode
    * @param decoder
    *   the decoder to use for decoding the JSON value
    * @tparam A
    *   the type of the value to decode
    * @return
    *   a Try containing the decoded value or an exception on failure
    */
  def decode[A](json: Json)(using JsonDecoder[A]): Try[A] =
    summon[JsonDecoder[A]].decode(json)

  /** Attempt to decode a JSON string into a value of type A
    * @param json
    *   the JSON string to decode
    * @param decoder
    *   the decoder to use for decoding the JSON value
    * @tparam A
    *   the type of the value to decode
    * @return
    *   a Try containing the decoded value or an exception on failure
    */
  def decode[A](json: String)(using JsonDecoder[A]): Try[A] =
    parse(json).left
      .map(e => new Exception(e.toString))
      .toTry
      .flatMap(decode[A])

  /** Encode a value to JSON
    * @param a
    *   the value to encode
    * @param encoder
    *   the encoder to use for encoding the value
    * @tparam A
    *   the type of the value to encode
    * @return
    *   the encoded JSON value
    */
  def encode[A](a: A)(using JsonEncoder[A]): Json =
    summon[JsonEncoder[A]].encode(a)
}
