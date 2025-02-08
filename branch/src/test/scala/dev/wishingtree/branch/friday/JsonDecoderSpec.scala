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
