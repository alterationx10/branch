package dev.alteration.branch.hollywood.tools.provided

import dev.alteration.branch.hollywood.OneShotAgent
import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture
import munit.FunSuite

class UrlGetToolSpec extends FunSuite {

  test("UrlGetTool should GET a webpage by url") {
    val tool = UrlGetTool("https://branch.alteration.dev")
    val result = tool.execute()

    assert(result.nonEmpty, "Response body should not be empty")
    assert(result.contains("branch.alteration.dev") || result.contains("html"),
      "Response should contain expected webpage content")
  }

}

class UrlGetToolAgentSpec extends LlamaServerFixture {

  // Comment this in/out to run
  override def munitIgnore: Boolean = false

  // Set this to false if llama-server is already running
  override val shouldStartLlamaServer: Boolean = false

  test("OneShotAgent should use UrlGetTool to fetch webpage content") {
    val toolRegistry = ToolRegistry().register[UrlGetTool]

    val agent = new OneShotAgent(
      systemPrompt = "You are a helpful assistant that can fetch web pages. Use the available tools to help answer questions.",
      toolRegistry = Some(toolRegistry)
    )

    val response = agent.chat("What's on the page at https://branch.alteration.dev?")
    println(response)
    assert(response.nonEmpty, "Agent response should not be empty")
    assert(response.toLowerCase().contains("framework"), "Refers to the framework documentation")
  }

}
