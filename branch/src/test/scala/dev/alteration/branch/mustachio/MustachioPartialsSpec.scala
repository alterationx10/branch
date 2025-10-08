package dev.alteration.branch.mustachio

class MustachioPartialsSpec extends MustacheSpecSuite {

  specSuite("mustache/partials.json").tests
    .foreach(runSpec)

}
