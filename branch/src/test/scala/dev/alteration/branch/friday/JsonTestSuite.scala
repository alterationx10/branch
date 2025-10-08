package dev.alteration.branch.friday

import dev.alteration.branch.macaroni.parsers.ParseError
import munit.FunSuite

import scala.io.Source
import scala.util.Using
import scala.util.*

trait JsonTestSuite extends FunSuite {

  val jsonResources: List[String] =
    Using(Source.fromResource("json")) { _.getLines().toList }.get

  def loadJson(j: String): String =
    Using(this.getClass.getResourceAsStream(s"/json/$j")) { stream =>
      new String(stream.readAllBytes())
    }.get

  /** y should parse n should not parse i is fair game
    * @param prefix
    * @param assertion
    * @param hint
    */
  def testForPrefix(
      prefix: String,
      assertion: Either[ParseError, Json] => Boolean
  ): Unit = {
    jsonResources.filter(_.startsWith(prefix)).foreach { j =>
      val json = loadJson(j)
      test(j) {
        val parsed = Json.parse(json)
        assert(assertion(parsed), parsed.toString)
      }
    }
  }

}
