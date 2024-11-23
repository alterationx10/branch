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

}
