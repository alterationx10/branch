package dev.alteration.branch.hollywood

import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class ConversationalAgentSpec extends LlamaServerFixture {

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
