package dev.alteration.branch.friday

import munit.FunSuite

class JsonRemoveNullsSpec extends FunSuite {

  test("removeNulls removes JsonNull from objects") {
    import Json.*

    val jsonWithNulls = Json.obj(
      "name"    -> JsonString("Alice"),
      "age"     -> JsonNumber(30),
      "address" -> JsonNull,
      "active"  -> JsonBool(true)
    )

    val expected = Json.obj(
      "name"   -> JsonString("Alice"),
      "age"    -> JsonNumber(30),
      "active" -> JsonBool(true)
    )

    assertEquals(jsonWithNulls.removeNulls(), expected)
  }

  test("removeNulls recursively removes nulls from nested objects") {
    import Json.*

    val nestedJson = Json.obj(
      "user"     -> Json.obj(
        "name"    -> JsonString("Bob"),
        "email"   -> JsonNull,
        "profile" -> Json.obj(
          "bio"    -> JsonString("Developer"),
          "avatar" -> JsonNull
        )
      ),
      "metadata" -> JsonNull
    )

    val expected = Json.obj(
      "user" -> Json.obj(
        "name"    -> JsonString("Bob"),
        "profile" -> Json.obj(
          "bio" -> JsonString("Developer")
        )
      )
    )

    assertEquals(nestedJson.removeNulls(), expected)
  }

  test("removeNulls removes nulls from arrays") {
    import Json.*

    val arrayWithNulls = Json.obj(
      "items" -> JsonArray(
        IndexedSeq(
          Json.obj("id" -> JsonNumber(1), "value" -> JsonNull),
          Json.obj("id" -> JsonNumber(2), "value" -> JsonString("test"))
        )
      )
    )

    val expected = Json.obj(
      "items" -> JsonArray(
        IndexedSeq(
          Json.obj("id" -> JsonNumber(1)),
          Json.obj("id" -> JsonNumber(2), "value" -> JsonString("test"))
        )
      )
    )

    assertEquals(arrayWithNulls.removeNulls(), expected)
  }

  test("removeNulls preserves non-object types") {
    import Json.*

    assertEquals(JsonString("test").removeNulls(), JsonString("test"))
    assertEquals(JsonNumber(42).removeNulls(), JsonNumber(42))
    assertEquals(JsonBool(true).removeNulls(), JsonBool(true))
    assertEquals(JsonNull.removeNulls(), JsonNull)
  }

  test("derived encoder now keeps nulls by default") {
    import Json.*

    case class Person(name: String, email: Option[String])

    given JsonEncoder[Person] = JsonEncoder.derived

    val person  = Person("Alice", None)
    val encoded = summon[JsonEncoder[Person]].encode(person)

    // Should contain null for the None value
    assertEquals(encoded.objVal.contains("email"), true)
    assertEquals(encoded.objVal("email"), JsonNull)
  }

  test("removeNulls can be used to get old behavior") {
    import Json.*

    case class Person(name: String, email: Option[String])

    given JsonEncoder[Person] = JsonEncoder.derived

    val person  = Person("Alice", None)
    val encoded = summon[JsonEncoder[Person]].encode(person).removeNulls()

    // Should NOT contain the null email field after removeNulls
    assertEquals(encoded.objVal.contains("email"), false)
  }
}
