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

    // Because we're reading the tests from a file,
    // it will escape the newlines, tabs, and carriage returns.
    // We need to unescape them.
    extension (s: String) {
      def unescape: String =
        s
          .replaceAll("\\\\n", "\n")
          .replaceAll("\\\\r", "\r")
          .replaceAll("\\\\t", "\t")
    }

    given decoder: JsonDecoder[Spec] with {
      def decode(json: Json): Try[Spec] = Try {
        for {
          name     <- json ? "name"
          desc     <- json ? "desc"
          data     <- json ? "data"
          template <- json ? "template"
          expected <- json ? "expected"
        } yield Spec(
          name.strVal.unescape,
          desc.strVal.unescape,
          data,
          template.strVal.unescape,
          expected.strVal.unescape
        )
      }.map(_.get)
    }

  }

  val defaultDelimiter: Mustachio.Delimiter = Mustachio.Delimiter("{{", "}}")

  def runSpec(
      spec: Spec
  )(implicit loc: munit.Location): Unit = {
    test(spec.name) {
      val context = Stache.fromJson(spec.data)
      assertEquals(
        Mustachio.render(spec.template, context, List.empty, Stache.empty, defaultDelimiter),
        spec.expected
      )
    }
  }
}
