package dev.alteration.branch.hollywood

import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class OneShotAgentSpec extends LlamaServerFixture {

  // Comment this in/out to run
  override def munitIgnore: Boolean = true

  // Set this to false if llama-server is already running
  override val shouldStartLlamaServer: Boolean = false

  test("OneShotAgent should respond to basic chat messages") {
    val agent = OneShotAgent(
      systemPrompt = "You are a helpful assistant. Respond concisely."
    )

    val response = agent.chat("What is 2+2?")
    assert(response.nonEmpty)
    assert(response.contains("4"))
  }

  test(
    "OneShotAgent.forTask should create agent with structured task definition"
  ) {
    val summarizer = OneShotAgent.forTask(
      taskName = "Text Summarization",
      taskDescription = "Summarize the given text in one sentence.",
      inputFormat = Some("Raw text"),
      outputFormat = Some("One sentence summary")
    )

    val text    = "Artificial intelligence has made tremendous progress. " +
      "AI systems are becoming increasingly capable in many domains."
    val summary = summarizer.chat(text)

    assert(summary.nonEmpty)
    assert(summary.length < text.length)
  }

  test("OneShotAgent.execute should support variable substitution") {
    val agent = OneShotAgent(
      systemPrompt = "You are a greeting generator."
    )

    val template = "Generate a {style} greeting for {name}."
    val result   = agent.execute(
      template,
      Map("style" -> "formal", "name" -> "Dr. Smith")
    )

    assert(result.nonEmpty)
    assert(!result.contains("{style}"))
    assert(!result.contains("{name}"))
  }
}
