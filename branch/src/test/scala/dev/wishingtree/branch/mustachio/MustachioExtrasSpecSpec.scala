package dev.wishingtree.branch.mustachio

class MustachioExtrasSpec extends MustacheSpecSuite {

  specSuite("mustache/extras.json").tests
    .foreach(runSpec)

}
