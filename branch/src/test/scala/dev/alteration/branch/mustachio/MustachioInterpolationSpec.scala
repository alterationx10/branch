package dev.alteration.branch.mustachio

class MustachioInterpolationSpec extends MustacheSpecSuite {

  specSuite("mustache/interpolation.json").tests
    .foreach(runSpec)

}
