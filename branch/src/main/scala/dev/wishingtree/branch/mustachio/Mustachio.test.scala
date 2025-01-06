package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.Json.*
import dev.wishingtree.branch.friday.JsonDecoder.given
import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonDecoder}

import scala.io.Source
import scala.util.{Try, Using}

case class SpecSuite(tests: IndexedSeq[Spec]) derives JsonCodec

object SpecSuite {

  given decoder: JsonDecoder[SpecSuite] with {
    def decode(json: Json): Try[SpecSuite] = Try {
      for {
        tests <- json ? "tests"
      } yield SpecSuite(tests.arrVal.map(Spec.decoder.decode(_).get))
    }.map(_.get)
  }

}

case class Spec(
    name: String,
    desc: String,
    data: Json,
    template: String,
    expected: String
)

object Spec {

  given decoder: JsonDecoder[Spec] with {
    def decode(json: Json): Try[Spec] = Try {
      for {
        name     <- json ? "name"
        desc     <- json ? "desc"
        data     <- json ? "data"
        template <- json ? "template"
        expected <- json ? "expected"
      } yield Spec(
        name.strVal,
        desc.strVal,
        data,
        template.strVal,
        expected.strVal
      )
    }.map(_.get)
  }

}

class MustachioSpec extends munit.FunSuite {

  def runSpec(
      spec: Spec
  )(implicit loc: munit.Location): Unit = {
    test(spec.name) {
      val context = Stache.fromJson(spec.data)
      assertEquals(Mustachio.render(spec.template, context), spec.expected)
    }
  }

  val interpolationSpec: SpecSuite =
    Using(Source.fromResource("mustache/interpolation.json")) { source =>
      SpecSuite.decoder.decode(source.mkString)
    }.flatten.getOrElse(throw new Exception("Failed to parse json"))

  interpolationSpec.tests.foreach(runSpec)

}
