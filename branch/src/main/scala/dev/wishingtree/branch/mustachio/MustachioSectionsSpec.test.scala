package dev.wishingtree.branch.mustachio

class MustachioSectionsSpec extends MustacheSpecSuite {

  specSuite("mustache/sections.json").tests
    .foreach(runSpec)

}
