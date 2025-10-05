package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.hollywood.OneShotAgent
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class SearXNGAgentSpec extends LlamaServerFixture {

  test("OneShotAgent using SearXNGTool returns search results") {
    val toolRegistry = ToolRegistry()
      .register[SearXNGTool]

    val agent = OneShotAgent(
      systemPrompt =
        "You are a search assistant. Use the search tool to search the web.",
      toolRegistry = Some(toolRegistry)
    )

    val response = agent.chat(
      "Search for 'Scala programming' results and tell me how many results you found. Give a list of the urls as well."
    )

    println(s"Agent response: $response")

    assert(response.nonEmpty, "Agent should provide a response")
    assert(
      response.matches(".*\\d+.*"),
      s"Response should contain a number (result count), got: '$response'"
    )
  }
}
