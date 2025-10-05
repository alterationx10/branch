package dev.alteration.branch.hollywood.tools.provided

import dev.alteration.branch.hollywood.OneShotAgent
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture
import munit.FunSuite

class WebFetchSpec extends FunSuite {

  test("WebFetch should GET a webpage by url") {
    val tool   = WebFetch("https://branch.alteration.dev")
    val result = tool.execute()

    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(body.nonEmpty, "Response body should not be empty")
      assert(
        body.contains("branch.alteration.dev") || body.contains("html"),
        "Response should contain expected webpage content"
      )
    }
  }

}

class WebFetchAgentSpec extends LlamaServerFixture {

  test("OneShotAgent should use WebFetch to fetch webpage content") {
    val toolRegistry = ToolRegistry().register[WebFetch]

    val agent = new OneShotAgent(
      systemPrompt =
        "You are a helpful assistant that can fetch web pages. Use the available tools to help answer questions.",
      toolRegistry = Some(toolRegistry)
    )

    val response =
      agent.chat("What's on the page at https://branch.alteration.dev?")
    println(response)
    assert(response.nonEmpty, "Agent response should not be empty")
    assert(
      response.toLowerCase().contains("framework"),
      "Refers to the framework documentation"
    )
  }

}
