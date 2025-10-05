package dev.alteration.branch.hollywood.tools.provided.searxng

import dev.alteration.branch.hollywood.OneShotAgent
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class SearXNGAgentSpec extends LlamaServerFixture {

  // Set to false to run the test when both llama-server and SearXNG are available
  override def munitIgnore: Boolean = false

  // Set this to false if llama-server is already running
  override val shouldStartLlamaServer: Boolean = true

  test("OneShotAgent using SearXNGTool to answer a question") {
    // Create a tool registry and register the SearXNG tool
    val toolRegistry = ToolRegistry()
      .register[SearXNGTool]

    // Create an agent with the search tool
    val agent = OneShotAgent(
      systemPrompt =
        "You are a helpful assistant with access to a web search tool. Use it to find current information when needed.",
      toolRegistry = Some(toolRegistry),
    )

    // Test the agent using the search tool
    val response = agent.chat("What is Scala 3? Give me a brief overview based on web search results.")

    println(s"Agent response: $response")

    assert(response.nonEmpty, "Agent should provide a response")
    assert(
      response.toLowerCase.contains("scala") ||
      response.toLowerCase.contains("programming"),
      "Response should mention Scala or programming"
    )
  }

  test("OneShotAgent using SearXNGTool for recent information") {
    val toolRegistry = ToolRegistry()
      .register[SearXNGTool]

    val agent = OneShotAgent(
      systemPrompt =
        "You are a research assistant. When asked about current topics, use the search tool to find recent information.",
      toolRegistry = Some(toolRegistry)
    )

    val response = agent.chat("What are some recent developments in functional programming?")

    println(s"Agent response about recent developments: $response")

    assert(response.nonEmpty, "Agent should provide a response")
  }

  test("OneShotAgent using SearXNGTool multiple times") {
    val toolRegistry = ToolRegistry()
      .register[SearXNGTool]

    val agent = OneShotAgent(
      systemPrompt =
        "You are a research assistant that uses web search to answer questions thoroughly. You may use the search tool multiple times if needed.",
      toolRegistry = Some(toolRegistry)
    )

    val response = agent.chat("Compare Scala and Kotlin. Search for information about both languages.")

    println(s"Agent response comparing languages: $response")

    assert(response.nonEmpty, "Agent should provide a response")
    assert(
      (response.toLowerCase.contains("scala") && response.toLowerCase.contains("kotlin")),
      "Response should mention both Scala and Kotlin"
    )
  }
}
