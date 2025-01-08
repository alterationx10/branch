package dev.wishingtree.branch.mustachio

import scala.io.Source
import scala.util.Using

class MustachioCommentsSpec extends MustacheSpecSuite {

  val commentSpec: SpecSuite =
    Using(Source.fromResource("mustache/comments.json")) { source =>
      SpecSuite.decoder.decode(source.mkString)
    }.flatten.getOrElse(throw new Exception("Failed to parse json"))

  commentSpec.tests.foreach(runSpec)

}
