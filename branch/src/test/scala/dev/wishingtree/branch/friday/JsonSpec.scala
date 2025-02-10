package dev.wishingtree.branch.friday

import munit.FunSuite

class JsonSpec extends FunSuite {

  test("parse JSON arrays") {
    val arrayJson = """[1, "two", true, null]"""
    val expected  = Json.JsonArray(
      IndexedSeq(
        Json.JsonNumber(1),
        Json.JsonString("two"),
        Json.JsonBool(true),
        Json.JsonNull
      )
    )
    assertEquals(Json.parse(arrayJson).toOption.get, expected)
  }

  test("parse nested JSON objects") {
    val objectJson =
      """{"name": "John", "age": 30, "address": {"city": "New York"}}"""
    val expected   = Json.obj(
      "name"    -> Json.JsonString("John"),
      "age"     -> Json.JsonNumber(30),
      "address" -> Json.obj(
        "city" -> Json.JsonString("New York")
      )
    )
    assertEquals(Json.parse(objectJson).toOption.get, expected)
  }

  test("safely access JSON values") {
    val json = Json.obj(
      "string" -> Json.JsonString("test"),
      "number" -> Json.JsonNumber(42),
      "bool"   -> Json.JsonBool(true),
      "array"  -> Json.arr(Json.JsonNumber(1), Json.JsonNumber(2)),
      "object" -> Json.obj("key" -> Json.JsonString("value"))
    )

    assertEquals(json ? "string" map (_.strVal), Some("test"))
    assertEquals(json ? "number" map (_.numVal), Some(42.0))
    assertEquals(json ? "bool" map (_.boolVal), Some(true))
    assertEquals(
      json ? "array" map (_.arrVal),
      Some(IndexedSeq(Json.JsonNumber(1), Json.JsonNumber(2)))
    )
    assertEquals(
      json ? "object" map (_.objVal),
      Some(Map("key" -> Json.JsonString("value")))
    )
    assertEquals(json ? "nonexistent", None)
  }

  test("handle invalid access gracefully") {
    val json = Json.JsonString("test")

    assertEquals(json.numOpt, None)
    assertEquals(json.boolOpt, None)
    assertEquals(json.arrOpt, None)
    assertEquals(json.objOpt, None)

    assertEquals(json.strOpt, Some("test"))
  }

  test("encode/decode with custom encoders and decoders") {
    case class Person(name: String, age: Int)

    given JsonEncoder[Person] = new JsonEncoder[Person] {
      def encode(p: Person): Json = Json.obj(
        "name" -> Json.JsonString(p.name),
        "age"  -> Json.JsonNumber(p.age)
      )
    }

    given JsonDecoder[Person] = new JsonDecoder[Person] {
      def decode(json: Json): scala.util.Try[Person] = {
        for {
          name <- (json ? "name")
                    .flatMap(_.strOpt)
                    .toRight(new Exception("Missing name"))
                    .toTry
          age  <- (json ? "age")
                    .flatMap(_.numOpt)
                    .map(_.toInt)
                    .toRight(new Exception("Missing age"))
                    .toTry
        } yield Person(name, age)
      }
    }

    val person  = Person("Alice", 25)
    val encoded = Json.encode(person)
    val decoded = Json.decode[Person](encoded)

    assertEquals(decoded.toOption.get, person)
  }

  test("encode exceptions") {
    val exception = new RuntimeException("test error")
    val json      = Json.throwable(exception)

    assertEquals(json ? "message" flatMap (_.strOpt), Some("test error"))
    assert((json ? "stackTrace" flatMap (_.arrOpt)).isDefined)
  }

  test("parse escaped characters") {
    val json =
      """
        |{
        |  "name": "Ampersand",
        |  "desc": "Ampersand should interpolate without HTML escaping.",
        |  "data": {
        |    "forbidden": "& \" < >"
        |  },
        |  "template": "These characters should not be HTML escaped: {{&forbidden}}\n",
        |  "expected": "These characters should not be HTML escaped: & \" < >\n"
        |}
        |""".stripMargin

    assert(Json.parse(json).isRight)
  }

  test("derive a Decoder that has a Json field") {
    case class JsClass(name: String, json: Json) derives JsonCodec
    val json    = """
                 |{
                 |  "name": "Alice",
                 |  "json": {
                 |    "age": 42
                 |  }
                 |}
                 |""".stripMargin
    val parsed  = Json.parse(json)
    assert(parsed.isRight)
    val decoder = summon[JsonCodec[JsClass]]
    val decoded = decoder.decode(json)
    assert(decoded.isSuccess)
    assertEquals(
      decoded.toOption.get,
      JsClass("Alice", Json.obj("age" -> Json.JsonNumber(42)))
    )
  }
}
