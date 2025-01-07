package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.{Json, JsonDecoder}

import scala.util.Try

trait MustacheSpecSuite extends munit.FunSuite {

  case class SpecSuite(tests: IndexedSeq[Spec])

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

  def runSpec(
      spec: Spec
  )(implicit loc: munit.Location): Unit = {
    test(spec.name) {
      val context = Stache.fromJson(spec.data)
      assertEquals(Mustachio.render(spec.template, context), spec.expected)
    }
  }
}
