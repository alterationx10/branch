package dev.alteration.branch.mustachio

class MustachioInvertedSpec extends MustacheSpecSuite {

  specSuite("mustache/inverted.json").tests
    .foreach(runSpec)

}
