package dev.wishingtree.branch.mustachio

class MustachioInvertedSpec extends MustacheSpecSuite {

  specSuite("mustache/inverted.json").tests
    .foreach(runSpec)

}
