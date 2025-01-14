package dev.wishingtree.branch.mustachio

class MustachioDelimeteresSpec extends MustacheSpecSuite {

  specSuite("mustache/delimiters.json").tests
    .foreach(runSpec)

}
