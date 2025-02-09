package dev.wishingtree.branch.friday.http

import dev.wishingtree.branch.friday.JsonCodec
import munit.*

class JsonConversionsSpec extends FunSuite {

  case class Person(name: String, age: Int) derives JsonCodec

  test("JsonConversions.convertToBytes") {
    given conv: Conversion[Person, Array[Byte]] =
      JsonConversions.convertToBytes

    val person = Person("Alice", 42)

    assertEquals(
      conv(person).toSeq,
      person.toJsonString.getBytes.toSeq
    )

  }

  test("JsonConversions.convertFromBytes") {
    given conv: Conversion[Array[Byte], Person] =
      JsonConversions.convertFromBytes

    val bytes: Array[Byte] =
      """{"name":"Alice","age":42}""".getBytes

    val person =
      Person("Alice", 42)

    assertEquals(
      conv(bytes),
      person
    )
  }
}
