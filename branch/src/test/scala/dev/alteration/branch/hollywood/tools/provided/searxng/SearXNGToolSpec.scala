package dev.alteration.branch.hollywood.tools.provided.searxng

import munit.FunSuite

class SearXNGToolSpec extends FunSuite {

  // Set to false to run the test when SearXNG is available at localhost:8888
  override def munitIgnore: Boolean = true

  test("SearXNGTool makes a real search request") {
    val tool = SearXNGTool(
      q = "scala programming language"
    )

    val result = tool.execute()

    assert(result.isSuccess, "Request should succeed")
    val response = result.get

    // Verify response structure
    assert(response.query.nonEmpty, "Query should not be empty")
    assert(response.results.nonEmpty, "Should have at least one result")

    val firstResult = response.results.head
    assert(firstResult.title.nonEmpty, "Result title should not be empty")
  }

  test("SearXNGTool with specific categories") {
    val tool = SearXNGTool(
      q = "functional programming",
      categories = Some("science,it")
    )

    val result = tool.execute()

    assert(result.isSuccess, "Request should succeed")
    val response = result.get
    assertEquals(response.query, "functional programming")
    assert(response.results.nonEmpty, "Should have at least one result")
  }

  test("SearXNGTool with time range filter") {
    val tool = SearXNGTool(
      q = "scala 3 news",
      time_range = Some("month")
    )

    val result = tool.execute()

    assert(result.isSuccess, "Request should succeed")
    val response = result.get
    assertEquals(response.query, "scala 3 news")
    assert(response.results.nonEmpty, "Should have at least one result")
  }

  test("SearXNGTool with multiple parameters") {
    val tool = SearXNGTool(
      q = "artificial intelligence",
      categories = Some("news"),
      language = Some("en"),
      pageno = Some(1),
      safesearch = Some(1)
    )

    val result = tool.execute()

    assert(result.isSuccess, "Request should succeed")
    val response = result.get
    assertEquals(response.query, "artificial intelligence")
    assert(response.results.nonEmpty, "Should have at least one result")
  }
}
