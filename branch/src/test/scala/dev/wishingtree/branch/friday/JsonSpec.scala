package dev.wishingtree.branch.friday

import munit.FunSuite

class JsonSpec extends FunSuite {
  case class Person(name: String, age: Int)

  test("Json.decode") {
    val personJson =
      """
        |{
        |  "name": "Alice",
        |  "age": 42
        |}
        |""".stripMargin
    for {
      person <- Json.decode[Person](personJson)
    } yield assertEquals(person, Person("Alice", 42))
  }

  test("Parses \"") {
    val json =
      """
        |    {
        |      "name": "Ampersand",
        |      "desc": "Ampersand should interpolate without HTML escaping.",
        |      "data": {
        |        "forbidden": "& \" < >"
        |      },
        |      "template": "These characters should not be HTML escaped: {{&forbidden}}\n",
        |      "expected": "These characters should not be HTML escaped: & \" < >\n"
        |    }
        |""".stripMargin

    val result = Json.parse(json)
    assert(result.isRight)
  }

  test("Derive a Decoder that has a Json field") {
    case class JsClass(name: String, json: Json) derives JsonCodec
    val json    =
      """
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
