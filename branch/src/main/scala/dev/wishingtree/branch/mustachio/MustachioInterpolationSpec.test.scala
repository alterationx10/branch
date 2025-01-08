package dev.wishingtree.branch.mustachio

import scala.io.Source
import scala.util.Using

class MustachioInterpolationSpec extends MustacheSpecSuite {

  val interpolationSpec: SpecSuite =
    Using(Source.fromResource("mustache/interpolation.json")) { source =>
      SpecSuite.decoder.decode(source.mkString)
    }.flatten.getOrElse(throw new Exception("Failed to parse json"))

  interpolationSpec.tests.foreach(runSpec)

}
