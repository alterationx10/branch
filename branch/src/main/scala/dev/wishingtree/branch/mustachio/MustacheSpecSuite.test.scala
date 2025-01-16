package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.friday.{Json, JsonDecoder}

import scala.io.Source
import scala.util.{Try, Using}

case class Spec(
    name: String,
    desc: String,
    data: Json,
    template: String,
    expected: String,
    partials: Json
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
        partials <- (json ? "partials").orElse(Some(Json.obj()))
      } yield Spec(
        name.strVal.unescape,
        desc.strVal.unescape,
        data,
        template.strVal.unescape,
        expected.strVal.unescape,
        partials
      )
    }.map(_.get)
  }

}

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

trait MustacheSpecSuite extends munit.FunSuite {

  def runSpec(
      spec: Spec
  )(implicit loc: munit.Location): Unit = {
    test(spec.name) {
      val context  = Stache.fromJson(spec.data)
      val partials = Option(Stache.fromJson(spec.partials))
      assertEquals(
        Mustachio.render(
          spec.template,
          context,
          partials
        ),
        spec.expected
      )
    }
  }

  /** Attempts to load and parse a Moustache spec suite from a resource file.
    * @return
    */
  def specSuite(resource: String): SpecSuite =
    Using(Source.fromResource(resource)) { source =>
      SpecSuite.decoder.decode(source.mkString)
    }.flatten
      .getOrElse(throw new Exception("Failed to parse json for specSuite"))
}
