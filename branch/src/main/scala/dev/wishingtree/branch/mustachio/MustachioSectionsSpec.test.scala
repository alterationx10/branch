package dev.wishingtree.branch.mustachio

class MustachioSectionsSpec extends MustacheSpecSuite {

  specSuite("mustache/sectionz.json").tests
    .foreach(runSpec)

}
