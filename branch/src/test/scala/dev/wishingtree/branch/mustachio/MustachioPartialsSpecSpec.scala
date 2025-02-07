package dev.wishingtree.branch.mustachio

class MustachioPartialsSpec extends MustacheSpecSuite {

  specSuite("mustache/partials.json").tests
    .foreach(runSpec)

}
