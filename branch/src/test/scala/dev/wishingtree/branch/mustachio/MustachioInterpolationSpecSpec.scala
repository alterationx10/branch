package dev.wishingtree.branch.mustachio

class MustachioInterpolationSpec extends MustacheSpecSuite {

  specSuite("mustache/interpolation.json").tests
    .foreach(runSpec)

}
