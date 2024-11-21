import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonDecoder}

import scala.language.postfixOps
import scala.util.*

object FridayExample {

  def main(args: Array[String]): Unit = {

    val json =
      """
        |{
        |  "name": "Branch",
        |  "some" : {
        |    "nested": {
        |      "key": "value"
        |    }
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
      Json.parse(json).map(js => js ? "some" ? "nested" ? "key")
    }

    println {
      Json.parse(json).map(js => js ? "totally" ? "not" ? "there")
    }

    case class Person(name: String, age: Int) derives JsonCodec

    println {
      Person("Alice", 42).toJson
    }
    
    Json.encode(Person("Mark", 42))

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

    Try(1 / 0) match {
      case Failure(exception) => println {
        Json.throwable(exception).toJsonString
      }
      case _ => ()
    }
    
  }

}
