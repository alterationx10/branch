package hollywood.oneshot

import dev.alteration.branch.hollywood.OneShotAgent

/** A basic example showing how to use OneShotAgent for stateless request-response
  * cycles.
  *
  * OneShotAgent is useful for:
  *   - Single-purpose tasks that don't require conversation state
  *   - Agents that can be used as tools within other agents
  *   - Simple text processing, analysis, or generation tasks
  *
  * This example assumes you have an OpenAI-compatible LLM server running:
  *   - Default URL: http://localhost:8080
  *   - Or set LLAMA_SERVER_URL environment variable
  *
  * To run this example: sbt "examples/runMain hollywood.oneshot.BasicOneShotExample"
  */
object BasicOneShotExample {

  def main(args: Array[String]): Unit = {
    println("=== Hollywood OneShotAgent Example ===\n")

    // Example 1: Simple text summarizer
    println("--- Example 1: Text Summarizer Agent ---")
    val summarizerAgent = OneShotAgent(
      systemPrompt = """You are a concise text summarizer.
        |Given any text, provide a 1-2 sentence summary of the key points.
        |Be direct and focus on the most important information.""".stripMargin
    )

    val longText = """
      Artificial intelligence has made remarkable progress in recent years,
      with large language models demonstrating unprecedented capabilities in
      natural language understanding and generation. These models are trained
      on vast amounts of text data and can perform a wide variety of tasks,
      from answering questions to writing code. However, they also raise
      important questions about safety, bias, and the future of human-AI
      interaction. Researchers and developers are working to address these
      challenges while continuing to improve model capabilities.
    """

    val summary = summarizerAgent.chat(longText)
    println(s"Summary: $summary")
    println()

    // Example 2: Using forTask factory method
    println("--- Example 2: Sentiment Analysis Agent (forTask) ---")
    val sentimentAgent = OneShotAgent.forTask(
      taskName = "Sentiment Analysis",
      taskDescription =
        "Analyze the sentiment of the given text and classify it as positive, negative, or neutral.",
      outputFormat = Some("Return only one word: positive, negative, or neutral")
    )

    val reviews = List(
      "This product exceeded my expectations! Highly recommended.",
      "Terrible quality, broke after two days of use.",
      "It's okay, does what it's supposed to do but nothing special."
    )

    reviews.foreach { review =>
      val sentiment = sentimentAgent.chat(review)
      println(s"Review: ${review.take(50)}...")
      println(s"Sentiment: $sentiment")
      println()
    }

    // Example 3: Using execute with template variables
    println("--- Example 3: Code Explainer Agent (with templates) ---")
    val codeExplainerAgent = OneShotAgent(
      systemPrompt = """You are a code explanation expert.
        |Explain code snippets in simple terms that a beginner can understand.
        |Focus on what the code does, not how it works internally.""".stripMargin
    )

    val codeSnippet = """
      def factorial(n: Int): Int =
        if (n <= 1) 1
        else n * factorial(n - 1)
    """

    val explanation = codeExplainerAgent.execute(
      userPromptTemplate = "Explain this {language} code:\n\n{code}",
      variables = Map("language" -> "Scala", "code" -> codeSnippet)
    )
    println(s"Code explanation: $explanation")
    println()

    // Example 4: Custom model configuration
    println("--- Example 4: Custom Configuration ---")
    val customAgent = OneShotAgent(
      systemPrompt = "You are a helpful assistant that responds in haiku format.",
      model = "gpt-oss", // Model name depends on your LLM server
      maxTurns = 5
    )

    val response = customAgent.chat("Tell me about the ocean")
    println(s"Haiku response: $response")
    println()

    println("=== Example Complete ===")
  }
}
