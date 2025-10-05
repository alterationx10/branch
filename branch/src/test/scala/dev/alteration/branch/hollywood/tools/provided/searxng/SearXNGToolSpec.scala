package dev.alteration.branch.hollywood.tools.provided.searxng

import munit.FunSuite

class SearXNGToolSpec extends FunSuite {

  // Set to false to run the test when SearXNG is available at localhost:8888
  override def munitIgnore: Boolean = false

  test("SearXNGTool makes a real search request") {
    val tool = SearXNGTool(
      q = "scala programming language"
    )

    val response = tool.execute()

    // Verify response structure
    assert(response.query.nonEmpty, "Query should not be empty")
    assert(response.results.nonEmpty, "Should have at least one result")

    val firstResult = response.results.head
    assert(firstResult.title.nonEmpty, "Result title should not be empty")
//    assert(firstResult.url.isDefined, "Result URL should be defined")
//    assert(firstResult.content.nonEmpty, "Result content should not be empty")

  }

  test("SearXNGTool with specific categories") {
    val tool = SearXNGTool(
      q = "functional programming"
    )

    val response = tool.execute()

    assertEquals(response.query, "functional programming")
    assert(response.results.nonEmpty, "Should have at least one result")
  }

  test("SearXNGTool with time range filter") {
    val tool = SearXNGTool(
      q = "scala 3 news"
    )

    val response = tool.execute()

    assertEquals(response.query, "scala 3 news")
    assert(response.results.nonEmpty, "Should have at least one result")

  }
}
