package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.tools.{schema, CallableTool, ToolRegistry}
import dev.alteration.branch.hollywood.tools.ToolRegistry.register
import dev.alteration.branch.hollywood.tools.schema.Param

object OneShotAgentExample {

  def main(args: Array[String]): Unit = {
    // Example 1: Simple sentiment analyzer
    val sentimentAnalyzer = OneShotAgent(
      systemPrompt =
        "You are a sentiment analyzer. Analyze the sentiment of the given text and respond with only: POSITIVE, NEGATIVE, or NEUTRAL."
    )

    println("=== Sentiment Analysis ===")
    println(sentimentAnalyzer.chat("I love this product! It's amazing!"))
    println(sentimentAnalyzer.chat("This is the worst experience ever."))
    println(sentimentAnalyzer.chat("The weather is cloudy today."))

    // Example 2: Code reviewer with template
    val codeReviewer = OneShotAgent(
      systemPrompt =
        """You are a code reviewer. Review the provided code and identify:
          |1. Potential bugs
          |2. Performance issues
          |3. Style violations
          |4. Suggestions for improvement
          |
          |Keep your review concise and actionable.""".stripMargin
    )

    println("\n=== Code Review ===")
    val codeToReview =
      """
        |def processUsers(users: List[User]): Unit = {
        |  for (user <- users) {
        |    println(user.name)
        |    Thread.sleep(1000)
        |  }
        |}
      """.stripMargin
    println(codeReviewer.chat(codeToReview))

    // Example 3: Using forTask for structured tasks
    val summarizer = OneShotAgent.forTask(
      taskName = "Text Summarization",
      taskDescription =
        "Summarize the given text in 2-3 sentences, capturing the main points.",
      inputFormat = Some("Raw text of any length"),
      outputFormat = Some("2-3 sentence summary")
    )

    println("\n=== Text Summarization ===")
    val longText =
      """
        |Artificial intelligence has made tremendous progress in recent years.
        |From natural language processing to computer vision, AI systems are
        |becoming increasingly capable. However, challenges remain in areas like
        |reasoning, common sense understanding, and ensuring AI safety. Researchers
        |continue to explore new architectures and training methods to address these
        |limitations.
      """.stripMargin
    println(summarizer.chat(longText))

    // Example 4: Using execute with variable substitution
    val emailGenerator = OneShotAgent(
      systemPrompt =
        "You are an email writer. Generate professional emails based on the given context."
    )

    println("\n=== Email Generation ===")
    val emailTemplate =
      """
        |Write a professional email to {recipient} about {topic}.
        |Tone: {tone}
        |Key points to include: {points}
      """.stripMargin

    println(
      emailGenerator.execute(
        emailTemplate,
        Map(
          "recipient" -> "the development team",
          "topic"     -> "the upcoming release",
          "tone"      -> "encouraging and informative",
          "points"    -> "new features, testing status, deployment timeline"
        )
      )
    )

    // Example 5: OneShotAgent wrapped as a CallableTool
    println("\n=== OneShotAgent as Tool ===")

    // Create a simple tool that uses OneShotAgent internally
    @schema.Tool("Extract contact information from text")
    case class ExtractContactTool(
        @Param("Text to extract contact information from") text: String
    ) extends CallableTool[String] {
      private val extractorAgent = OneShotAgent(
        systemPrompt =
          "Extract structured data from unstructured text. Return name, email, and phone number in a clear format."
      )

      override def execute(): String = {
        extractorAgent.chat(
          s"Extract name, email, and phone number from: $text"
        )
      }
    }

    val toolRegistry = ToolRegistry()
    toolRegistry.register[ExtractContactTool]

    val contactText =
      "My name is John Doe, you can reach me at john.doe@example.com or call 555-1234"
    val result = toolRegistry.execute(
      toolRegistry.getTools.head.function.name,
      Map("text" -> contactText)
    )
    println(result.getOrElse("Tool not found"))
  }
}
