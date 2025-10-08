package dev.alteration.branch.friday

class JsonTestTheRestOfThemSpec extends JsonTestSuite {

  // The i prefix is for edge cases, and generally just interesting if a particular parser handles it or not
  testForPrefix(
    "i",
    e => {
      println(e)
      e.isRight || e.isLeft
    }
  )

}
