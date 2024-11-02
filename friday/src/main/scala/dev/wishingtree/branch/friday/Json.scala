package dev.wishingtree.branch.friday

import dev.wishingtree.branch.macaroni.parsers.Reference.Parser
import dev.wishingtree.branch.macaroni.parsers.{ParseError, Parsers, Reference}

import scala.annotation.targetName
import scala.util.Try

enum Json {
  case JsonNull
  case JsonBool(value: Boolean)
  case JsonNumber(value: Double)
  case JsonString(value: String)
  case JsonArray(value: IndexedSeq[Json])
  case JsonObject(value: Map[String, Json])
}

object Json {

  extension (j: Json) {

    @targetName("exists")
    def ?(field: String): Option[Json] = j match {
      case JsonObject(value) => value.get(field)
      case _                 => None
    }

    infix def strVal: String = j match {
      case JsonString(str) => str
      case _               => throw IllegalArgumentException("Not a string")
    }

    infix def strOpt: Option[String] =
      Try(strVal).toOption

    infix def numVal: Double = j match {
      case JsonNumber(num) => num
      case _               => throw IllegalArgumentException("Not a number")
    }

    infix def numOpt: Option[Double] =
      Try(numVal).toOption

    infix def boolVal: Boolean = j match {
      case JsonBool(bool) => bool
      case _              => throw IllegalArgumentException("Not a boolean")
    }

    infix def boolOpt: Option[Boolean] =
      Try(boolVal).toOption

    infix def arrVal: IndexedSeq[Json] = j match {
      case JsonArray(arr) => arr
      case _              => throw IllegalArgumentException("Not an array")
    }

    infix def arrOpt: Option[IndexedSeq[Json]] =
      Try(arrVal).toOption

    infix def objVal: Map[String, Json] = j match {
      case JsonObject(obj) => obj
      case _               => throw IllegalArgumentException("Not an object")
    }

    infix def objOpt: Option[Map[String, Json]] =
      Try(objVal).toOption

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
    @targetName("exists")
    def ?(field: String): Option[Json] =
      jo.flatMap(_ ? field)

    infix def strOpt: Option[String] =
      jo.flatMap(_.strOpt)

    infix def numOpt: Option[Double] =
      jo.flatMap(_.numOpt)

    infix def boolOpt: Option[Boolean] =
      jo.flatMap(_.boolOpt)

    infix def arrOpt: Option[IndexedSeq[Json]] =
      jo.flatMap(_.arrOpt)

    infix def objOpt: Option[Map[String, Json]] =
      jo.flatMap(_.objOpt)

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
  def parse(json: String): Either[ParseError, Json] =
    defaultParser.run(json)
  
  def decode[A](json: Json)(using JsonDecoder[A]): Try[A] =
    summon[JsonDecoder[A]].decode(json)
    
  def decode[A](json: String)(using JsonDecoder[A]): Try[A] =
    parse(json).left.map(e => new Exception(e.toString)).toTry.flatMap(decode[A])

  def encode[A](a: A)(using JsonEncoder[A]): Json =
    summon[JsonEncoder[A]].encode(a)
}
