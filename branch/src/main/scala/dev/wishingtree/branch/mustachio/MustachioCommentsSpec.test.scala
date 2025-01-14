package dev.wishingtree.branch.mustachio

class MustachioCommentsSpec extends MustacheSpecSuite {

  specSuite("mustache/comments.json").tests
    .foreach(runSpec)

}
