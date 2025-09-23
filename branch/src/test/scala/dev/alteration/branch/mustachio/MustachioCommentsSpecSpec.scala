package dev.alteration.branch.mustachio

class MustachioCommentsSpec extends MustacheSpecSuite {

  specSuite("mustache/comments.json").tests
    .foreach(runSpec)

}
