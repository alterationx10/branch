import dev.wishingtree.branch.friday.Json
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

  test("Parses \"") {
    val json =
      """
        |    {
        |      "name": "Ampersand",
        |      "desc": "Ampersand should interpolate without HTML escaping.",
        |      "data": {
        |        "forbidden": "& \" < >"
        |      },
        |      "template": "These characters should not be HTML escaped: {{&forbidden}}\n",
        |      "expected": "These characters should not be HTML escaped: & \" < >\n"
        |    }
        |""".stripMargin

    val result = Json.parse(json)
    assert(result.isRight)
  }

}
