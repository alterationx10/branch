package dev.wishingtree.branch.friday

import dev.wishingtree.branch.friday.Json.*
import munit.FunSuite

class JsonDecoderSpec extends FunSuite {

  case class Person(name: String, age: Int) derives JsonDecoder

  test("JsonDecoder.decode") {

    val json = Json.obj(
      "name" -> JsonString("Alice"),
      "age"  -> JsonNumber(42)
    )
    for {
      person <- JsonDecoder.decode[Person](json)
    } yield assertEquals(person, Person("Alice", 42))

  }
}
