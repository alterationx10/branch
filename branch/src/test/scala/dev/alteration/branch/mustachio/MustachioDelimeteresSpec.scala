package dev.alteration.branch.mustachio

class MustachioDelimeteresSpec extends MustacheSpecSuite {

  specSuite("mustache/delimiters.json").tests
    .foreach(runSpec)

}
