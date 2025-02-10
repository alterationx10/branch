package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.*
import munit.FunSuite

class JsonEncoderSpec extends FunSuite {

  case class Person(name: String, age: Int) derives JsonEncoder

  test("JsonEncoder.encode") {
    val json = JsonEncoder.encode(Person("Alice", 42))
    assertEquals(
      json,
      Json.obj(
        "name" -> JsonString("Alice"),
        "age"  -> JsonNumber(42)
      )
    )
  }

  test("JsonEncoder A => Seq[A]") {
    val strSeqEncoder = summon[JsonEncoder[Seq[String]]]
    assertEquals(
      strSeqEncoder.encode(Seq("Alice", "Bob")),
      Json.arr(JsonString("Alice"), JsonString("Bob"))
    )
  }

  test("JsonEncoder A => List[A]") {
    val strListEncoder = summon[JsonEncoder[List[String]]]
    assertEquals(
      strListEncoder.encode(List("Alice", "Bob")),
      Json.arr(JsonString("Alice"), JsonString("Bob"))
    )
  }

  test("JsonEncoder A => Vector[A]") {
    val strVectorEncoder = summon[JsonEncoder[Vector[String]]]
    assertEquals(
      strVectorEncoder.encode(Vector("Alice", "Bob")),
      Json.arr(JsonString("Alice"), JsonString("Bob"))
    )
  }

  test("JsonEncoder A => Set[A]") {
    val strSetEncoder = summon[JsonEncoder[Set[String]]]
    assertEquals(
      strSetEncoder.encode(Set("Alice", "Bob")),
      Json.arr(JsonString("Alice"), JsonString("Bob"))
    )
  }

  test("JsonEncoder for Option[A] - Some case") {
    given JsonEncoder[Option[String]] = JsonEncoder.from {
      case Some(value) => JsonString(value)
      case None        => Json.JsonNull
    }

    assertEquals(
      summon[JsonEncoder[Option[String]]].encode(Some("test")),
      JsonString("test")
    )
  }

  test("JsonEncoder for Option[A] - None case") {
    given JsonEncoder[Option[String]] = JsonEncoder.from {
      case Some(value) => JsonString(value)
      case None        => Json.JsonNull
    }

    assertEquals(
      summon[JsonEncoder[Option[String]]].encode(None),
      Json.JsonNull
    )
  }

  test("JsonEncoder.contraMap") {
    val stringEncoder      = summon[JsonEncoder[String]]
    val intToStringEncoder = stringEncoder.contraMap[Int](_.toString)

    assertEquals(
      intToStringEncoder.encode(42),
      JsonString("42")
    )
  }

  test("JsonEncoder for nested case classes") {
    case class Address(street: String, city: String) derives JsonEncoder
    case class User(name: String, address: Address) derives JsonEncoder

    val user = User("Alice", Address("123 Main St", "Springfield"))
    assertEquals(
      JsonEncoder.encode(user),
      Json.obj(
        "name"    -> JsonString("Alice"),
        "address" -> Json.obj(
          "street" -> JsonString("123 Main St"),
          "city"   -> JsonString("Springfield")
        )
      )
    )
  }

  test("JsonEncoder extension methods") {
    val person = Person("Bob", 25)

    assertEquals(
      person.toJson,
      Json.obj(
        "name" -> JsonString("Bob"),
        "age"  -> JsonNumber(25)
      )
    )

    assertEquals(
      person.toJsonString,
      """{"name":"Bob","age":25}"""
    )
  }

  test("JsonEncoder for Map[String, String]") {
    val map                                = Map("key1" -> "value1", "key2" -> "value2")
    given JsonEncoder[Map[String, String]] = JsonEncoder.from { m =>
      JsonObject(m.map((k, v) => k -> JsonString(v)))
    }

    assertEquals(
      summon[JsonEncoder[Map[String, String]]].encode(map),
      Json.obj(
        "key1" -> JsonString("value1"),
        "key2" -> JsonString("value2")
      )
    )
  }

  test("JsonEncoder for Instant") {
    import java.time.Instant
    val instant = Instant.parse("2024-03-14T12:00:00Z")

    assertEquals(
      JsonEncoder.encode(instant),
      JsonString("2024-03-14T12:00:00Z")
    )
  }


}
