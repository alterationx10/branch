package dev.alteration.branch.hollywood

import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class ConversationalAgentSpec extends LlamaServerFixture {

  // Comment this in/out to run
  override def munitIgnore: Boolean = true

  // Set this to false if llama-server is already running
  override val shouldStartLlamaServer: Boolean = false

  test("ConversationalAgent should maintain conversation history") {
    val agent     = ConversationalAgent()
    val response1 = agent.chat("I have an orange cat named Whiskers.")
    assert(response1.nonEmpty)
    val response2 = agent.chat("What color was it?")
    assert(response2.nonEmpty)
    assert(
      response2.toLowerCase.contains("orange")
    )
  }

}
