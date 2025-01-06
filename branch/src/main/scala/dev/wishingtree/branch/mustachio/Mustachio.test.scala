package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.Json.*
import dev.wishingtree.branch.friday.JsonDecoder.given
import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonDecoder}

import scala.io.Source
import scala.util.Using

case class SpecSuite(tests: IndexedSeq[Spec]) derives JsonCodec

case class Spec(name: String, desc: String, template: String, expected: String)
    derives JsonCodec

class MustachioSpec extends munit.FunSuite {

//  def runSpec(
//      spec: Spec
//  )(implicit loc: munit.Location): Unit = {
//    test(spec.name) {
//      val context = Stache.fromJson(spec.data)
//      assertEquals(Mustachio.compile(spec.template, context), spec.expected)
//    }
//  }

  test("wtf") {
    val jsonTest = Using(Source.fromResource("mustache/interpolation.json")) {
      source =>
        val jsString = source.mkString
        val json = Json.parse(jsString)
        println(s"json: $json")
        val decoder: JsonDecoder[SpecSuite] = summon[JsonDecoder[SpecSuite]]
        val decoded = decoder.decode(jsString)
        println(s"decoded: $decoded")
        decoded
    }.flatten.getOrElse(throw new Exception("Failed to parse json"))

    val firstSpec = jsonTest.tests.head
  }

}
