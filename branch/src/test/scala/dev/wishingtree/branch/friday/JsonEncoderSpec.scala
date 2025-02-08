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

}
