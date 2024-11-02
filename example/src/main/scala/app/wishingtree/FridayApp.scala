package app.wishingtree

import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonDecoder}

import scala.language.postfixOps

object FridayApp {

  def main(args: Array[String]): Unit = {

    val json =
      """
        |{
        |  "name": "Branch",
        |  "some" : {
        |   "nested": {
        |    "key": "value"
        |   }
        |  }
        |}
        |""".stripMargin


    println {
      Json.parse(json)
    }

    println {
      Json.parse(json).map(js => js ? ("name"))
    }

    println {
      Json.parse(json).map(js => js ? ("some") ? ("nested") ? ("key"))
    }

    println {
      Json.parse(json).map(js => js ? "totally" ? "not" ? "there")
    }

    case class Person(name: String, age: Int) derives JsonCodec

    println {
      Person("Alice", 42).toJson
    }

    val personJson =
      """
        |{
        |  "name": "Alice",
        |  "age": 42
        |}
        |""".stripMargin
    
    println {
      Json.decode[Person](personJson)
    }
    
  }

}
