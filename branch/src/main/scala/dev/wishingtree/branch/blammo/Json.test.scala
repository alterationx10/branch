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

}
