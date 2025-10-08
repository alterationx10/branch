package dev.alteration.branch.mustachio

class MustachioExtrasSpec extends MustacheSpecSuite {

  specSuite("mustache/extras.json").tests
    .foreach(runSpec)

}
