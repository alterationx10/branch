package dev.alteration.branch.friday

class JsonTestDoesNotParseSpec extends JsonTestSuite {

  testForPrefix("n", _.isLeft)

}
