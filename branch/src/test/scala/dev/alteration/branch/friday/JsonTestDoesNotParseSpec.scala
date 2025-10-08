package dev.alteration.branch.friday

class JsonTestDoesNotParseSpec extends JsonTestSuite {

  // These test cases represent extreme nesting, and will stack-overflow this parser.
  val ignoreFiles: String => Boolean =
    fn =>
      fn.equals("n_structure_100000_opening_arrays.json") ||
        fn.equals("n_structure_open_array_object.json")

  testForPrefix("n", _.isLeft, ignoreFiles)

}
