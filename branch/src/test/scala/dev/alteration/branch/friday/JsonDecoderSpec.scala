package dev.alteration.branch.friday

import Json.*
import JsonDecoder.*
import munit.FunSuite

import java.time.Instant
import scala.util.Success

class JsonDecoderSpec extends FunSuite {

  case class Person(name: String, age: Int) derives JsonDecoder

  test("decode collections") {
    val jsonStr = """{"numbers": [1, 2, 3], "strings": ["a", "b", "c"]}"""
    val json    = Json.parse(jsonStr).toOption.get

    assertEquals(
      (json ? "numbers").map(_.arrVal.map(_.numVal.toInt).toList).get,
      List(1, 2, 3)
    )
    assertEquals(
      (json ? "strings").map(_.arrVal.map(_.strVal).toVector).get,
      Vector("a", "b", "c")
    )
  }

  test("decode Instant") {
    val timestamp = "2024-03-14T12:00:00Z"
    val jsonStr   = s"""{"time": "$timestamp"}"""
    val json      = Json.parse(jsonStr).toOption.get

    assertEquals(
      (json ? "time").map(_.strVal).map(Instant.parse).get,
      Instant.parse(timestamp)
    )
  }

  test("derive decoder for case classes") {
    case class PersonWithHobbies(name: String, age: Int, hobbies: List[String])

    val jsonStr = """
      {
        "name": "John Doe",
        "age": 30,
        "hobbies": ["reading", "gaming", "coding"]
      }
    """
    val json    = Json.parse(jsonStr).toOption.get

    val expected                         =
      PersonWithHobbies("John Doe", 30, List("reading", "gaming", "coding"))
    given JsonDecoder[PersonWithHobbies] = JsonDecoder.derived
    assertEquals(json.decodeAs[PersonWithHobbies], Success(expected))
  }

  test("derive decoder for nested case classes") {
    case class PersonWithHobbies(name: String, age: Int, hobbies: List[String])
    case class Address(street: String, city: String, zipCode: String)
    case class Employee(
        person: PersonWithHobbies,
        address: Address,
        salary: Double
    )

    val jsonStr = """
      {
        "person": {
          "name": "Jane Smith",
          "age": 28,
          "hobbies": ["painting", "music"]
        },
        "address": {
          "street": "123 Main St",
          "city": "Springfield",
          "zipCode": "12345"
        },
        "salary": 75000.0
      }
    """
    val json    = Json.parse(jsonStr).toOption.get

    given JsonDecoder[Employee] = JsonDecoder.derived

    val expected = Employee(
      PersonWithHobbies("Jane Smith", 28, List("painting", "music")),
      Address("123 Main St", "Springfield", "12345"),
      75000.0
    )
    assertEquals(json.decodeAs[Employee], Success(expected))
  }

  test("handle decoding errors gracefully") {
    val invalidJson = """{"age": "not a number"}"""
    val json        = Json.parse(invalidJson).toOption.get

    case class Person(age: Int)
    given JsonDecoder[Person] = JsonDecoder.derived
    val result                = json.decodeAs[Person]
    assert(result.isFailure)
  }

  test("support decoder composition using map") {
    case class UserId(value: String) derives JsonDecoder

    val userIdDecoder: JsonDecoder[UserId] =
      summon[JsonDecoder[String]].map(UserId.apply)

    val jsonStr = """{"id": "user-123"}"""
    val json    = Json.parse(jsonStr).toOption.get

    assertEquals(
      (json ? "id").map(userIdDecoder.decode).get,
      Success(UserId("user-123"))
    )
  }

  test("JsonDecoder.decode") {
    val json = Json.obj(
      "name" -> JsonString("Alice"),
      "age"  -> JsonNumber(42)
    )
    for {
      person <- JsonDecoder.decode[Person](json)
    } yield assertEquals(person, Person("Alice", 42))
  }

  test("JsonDecoder Json => Seq[A]") {
    val decoder = JsonDecoder.seqDecoder[String]
    val json    = Json.arr(JsonString("Alice"), JsonString("Bob"))
    for {
      seq <- decoder.decode(json)
    } yield assertEquals(seq, Seq("Alice", "Bob"))
  }

  test("JsonDecoder Json => List[A]") {
    val decoder = JsonDecoder.listDecoder[String]
    val json    = Json.arr(JsonString("Alice"), JsonString("Bob"))
    for {
      list <- decoder.decode(json)
    } yield assertEquals(list, List("Alice", "Bob"))
  }

  test("JsonDecoder Json => Set[A]") {
    val decoder = JsonDecoder.setDecoder[String]
    val json    = Json.arr(JsonString("Alice"), JsonString("Bob"))
    for {
      set <- decoder.decode(json)
    } yield assertEquals(set, Set("Alice", "Bob"))
  }

  test("JsonDecoder Json => IndexedSeq[A]") {
    val decoder = JsonDecoder.indexedSeqDecoder[String]
    val json    = Json.arr(JsonString("Alice"), JsonString("Bob"))
    for {
      indexedSeq <- decoder.decode(json)
    } yield assertEquals(indexedSeq, IndexedSeq("Alice", "Bob"))
  }

  test("JsonDecoder Json => Vector[A]") {
    val decoder = JsonDecoder.vectorDecoder[String]
    val json    = Json.arr(JsonString("Alice"), JsonString("Bob"))
    for {
      vector <- decoder.decode(json)
    } yield assertEquals(vector, Vector("Alice", "Bob"))
  }
}
