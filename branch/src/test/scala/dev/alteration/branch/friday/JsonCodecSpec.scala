package dev.alteration.branch.friday

import scala.util.*
import java.time.*
import munit.*
import Json.JsonString
import Json.JsonNumber
import Json.JsonObject

class JsonCodecSpec extends FunSuite {

  test("JsonCodec.transform") {
    given instantCodec: JsonCodec[Instant] =
      JsonCodec[String]
        .transform(str => Try(Instant.parse(str)).get)(_.toString)
    val instant                            = Instant.now()
    val json                               = instantCodec.encode(instant)
    assertEquals(instantCodec.decode(json), Success(instant))

    case class TimeCapsule(instant: Instant) derives JsonCodec
    val jsStr    = """{"instant":"2024-12-27T03:30:29.460232Z"}"""
    val expected = Instant.parse("2024-12-27T03:30:29.460232Z")
    assertEquals(
      summon[JsonCodec[TimeCapsule]].decode(jsStr),
      Success(TimeCapsule(expected))
    )

  }

  test("JsonCodec.from constructor") {
    case class UserId(value: String)
    val codec = JsonCodec.from[UserId](
      json => Try(UserId(json.strVal)),
      user => JsonString(user.value)
    )

    val userId = UserId("user-123")
    val json   = codec.encode(userId)
    assertEquals(codec.decode(json), Success(userId))
  }

  test("extension methods") {
    case class User(name: String, age: Int) derives JsonCodec
    val user = User("Alice", 30)

    // Test toJson and toJsonString
    assertEquals(
      user.toJson,
      Json.obj("name" -> JsonString("Alice"), "age" -> JsonNumber(30))
    )
    assertEquals(user.toJsonString, """{"name":"Alice","age":30}""")

    // Test decodeAs
    val jsonStr             = """{"name":"Bob","age":25}"""
    given JsonDecoder[User] = JsonDecoder.derived
    assertEquals(jsonStr.decodeAs[User], Success(User("Bob", 25)))
  }

  test("nested case classes") {
    case class Address(street: String, city: String) derives JsonCodec
    case class Person(name: String, address: Address) derives JsonCodec

    val person = Person("Alice", Address("123 Main St", "Springfield"))
    val json   = person.toJson
    assertEquals(JsonCodec[Person].decode(json), Success(person))
  }

  test("collection types") {
    val numbers   = List(1, 2, 3)
    val jsonArray = JsonCodec[List[Int]].encode(numbers)
    assertEquals(JsonCodec[List[Int]].decode(jsonArray), Success(numbers))

    given mapCodec: JsonCodec[Map[String, Int]] = JsonCodec.from(
      json => Try(json.objVal.map((k, v) => (k, v.numVal.toInt)).toMap),
      map => JsonObject(map.map((k, v) => k -> JsonNumber(v)))
    )

    val map     = Map("a" -> 1, "b" -> 2)
    val jsonMap = mapCodec.encode(map)
    assertEquals(mapCodec.decode(jsonMap), Success(map))
  }
}
