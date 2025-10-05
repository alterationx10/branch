package dev.alteration.branch.hollywood.tools.provided.searxng

import munit.FunSuite

class SearXNGToolSpec extends FunSuite {

  // Set to false to run the test when SearXNG is available at localhost:8888
  override def munitIgnore: Boolean = false

  test("SearXNGTool makes a real search request") {
    val tool = SearXNGTool(
      searchRequest = SearXNGRequest(
        q = "scala programming language",
        language = Some("en"),
        pageno = Some(1)
      )
    )

    val response = tool.execute()

    // Verify response structure
    assert(response.query.nonEmpty, "Query should not be empty")
    assert(response.number_of_results >= 0, "Number of results should be non-negative")

    // Verify results if any are returned
    if (response.results.nonEmpty) {
      val firstResult = response.results.head
      assert(firstResult.title.nonEmpty, "Result title should not be empty")
      assert(firstResult.url.nonEmpty, "Result URL should not be empty")
      assert(firstResult.content.nonEmpty, "Result content should not be empty")

      println(s"Query: ${response.query}")
      println(s"Number of results: ${response.number_of_results}")
      println(s"First result: ${firstResult.title}")
      println(s"  URL: ${firstResult.url}")
      println(s"  Content: ${firstResult.content.take(100)}...")
    }
  }

  test("SearXNGTool with specific categories") {
    val tool = SearXNGTool(
      searchRequest = SearXNGRequest(
        q = "functional programming",
        categories = Some("general"),
        engines = Some("duckduckgo"),
        language = Some("en")
      )
    )

    val response = tool.execute()

    assertEquals(response.query, "functional programming")
    assert(response.results.nonEmpty, "Should have at least one result")
  }

  test("SearXNGTool with time range filter") {
    val tool = SearXNGTool(
      searchRequest = SearXNGRequest(
        q = "scala 3 news",
        time_range = Some("month"),
        language = Some("en")
      )
    )

    val response = tool.execute()

    assertEquals(response.query, "scala 3 news")
    // Results may be empty depending on recent news
    println(s"Recent results count: ${response.number_of_results}")
  }
}
