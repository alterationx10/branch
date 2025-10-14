package hollywood.multiagent

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.{Agent, OneShotAgent}

/** Example showing multi-agent systems where agents can communicate with each
  * other.
  *
  * The Hollywood module provides Agent.deriveAgentTool to wrap any agent as a
  * tool, enabling agent-to-agent communication. This is useful for:
  *   - Specialized agents that delegate to expert sub-agents
  *   - Hierarchical agent systems
  *   - Complex workflows requiring different expertise areas
  *
  * This example assumes you have an OpenAI-compatible LLM server running:
  *   - Default URL: http://localhost:8080
  *   - Or set LLAMA_SERVER_URL environment variable
  *   - Server must support tool/function calling
  *
  * To run: sbt "examples/runMain hollywood.multiagent.MultiAgentExample"
  */
object MultiAgentExample {

  def main(args: Array[String]): Unit = {
    // JsonEncoder instances for basic types are automatically available
    println("=== Hollywood Multi-Agent Example ===\n")

    // Example 1: Specialist agents coordinated by an orchestrator
    println("--- Example 1: Orchestrator with Specialist Agents ---")

    // Create specialist agents
    val pythonExpert = OneShotAgent(
      systemPrompt = """You are a Python programming expert.
        |Answer questions about Python concisely and accurately.
        |Focus on best practices and idiomatic Python code.""".stripMargin
    )

    val scalaExpert = OneShotAgent(
      systemPrompt = """You are a Scala programming expert.
        |Answer questions about Scala concisely and accurately.
        |Focus on functional programming and type safety.""".stripMargin
    )

    val sqlExpert = OneShotAgent(
      systemPrompt = """You are a SQL database expert.
        |Answer questions about SQL and database design concisely.
        |Focus on efficient queries and proper indexing.""".stripMargin
    )

    // Derive agent tools from the specialist agents
    val pythonTool = Agent.deriveAgentTool(
      pythonExpert,
      agentName = Some("PythonExpert"),
      description = "Ask Python programming questions to a Python expert"
    )

    val scalaTool = Agent.deriveAgentTool(
      scalaExpert,
      agentName = Some("ScalaExpert"),
      description = "Ask Scala programming questions to a Scala expert"
    )

    val sqlTool = Agent.deriveAgentTool(
      sqlExpert,
      agentName = Some("SQLExpert"),
      description = "Ask SQL and database questions to a database expert"
    )

    // Create orchestrator agent with access to all specialists
    val toolRegistry = ToolRegistry()
      .register(pythonTool)
      .register(scalaTool)
      .register(sqlTool)

    println(
      s"Specialist agents available: ${toolRegistry.getRegisteredToolNames.mkString(", ")}\n"
    )

    val orchestrator = OneShotAgent(
      systemPrompt = """You are a programming assistant that coordinates with specialist agents.
        |When a user asks a programming question, determine which specialist can best help
        |and route the question to them. You have access to Python, Scala, and SQL experts.
        |After getting their response, provide it to the user along with any additional context.""".stripMargin,
      toolRegistry = Some(toolRegistry)
    )

    val questions = List(
      "How do I write a list comprehension in Python?",
      "What's the difference between a List and a Vector in Scala?",
      "How do I create an index on a table?"
    )

    questions.foreach { question =>
      println(s"User: $question")
      val answer = orchestrator.chat(question)
      println(s"Orchestrator: $answer\n")
    }

    // Example 2: Research agent with fact-checker
    println("--- Example 2: Research Agent with Fact Checker ---")

    val researchAgent = OneShotAgent(
      systemPrompt = """You are a research agent that provides detailed information.
        |Try to be comprehensive in your answers, but stay concise.""".stripMargin
    )

    val factCheckerAgent = OneShotAgent(
      systemPrompt = """You are a fact-checking agent.
        |Given a statement or claim, assess its accuracy and provide corrections if needed.
        |Be critical but fair.""".stripMargin
    )

    val researchTool = Agent.deriveAgentTool(
      researchAgent,
      agentName = Some("Researcher"),
      description = "Get detailed research and information on a topic"
    )

    val factCheckTool = Agent.deriveAgentTool(
      factCheckerAgent,
      agentName = Some("FactChecker"),
      description = "Verify the accuracy of statements or claims"
    )

    val supervisorRegistry = ToolRegistry()
      .register(researchTool)
      .register(factCheckTool)

    val supervisorAgent = OneShotAgent(
      systemPrompt = """You are a supervisor agent that ensures high-quality, accurate information.
        |When answering questions:
        |1. First, ask the Researcher for information
        |2. Then, ask the FactChecker to verify the information
        |3. Provide the user with a final answer that incorporates both perspectives""".stripMargin,
      toolRegistry = Some(supervisorRegistry),
      maxTurns = 15 // Allow more turns for multi-step process
    )

    val researchQuery = "Tell me about the speed of light"
    println(s"User: $researchQuery")
    val verifiedAnswer = supervisorAgent.chat(researchQuery)
    println(s"Supervisor: $verifiedAnswer\n")

    // Example 3: Creative writing with editor
    println("--- Example 3: Writer and Editor Collaboration ---")

    val writerAgent = OneShotAgent(
      systemPrompt = """You are a creative writer.
        |Write engaging, creative content based on prompts.
        |Be imaginative and descriptive.""".stripMargin
    )

    val editorAgent = OneShotAgent(
      systemPrompt = """You are an editor who reviews and improves writing.
        |Suggest improvements for clarity, grammar, and style.
        |Be constructive and specific.""".stripMargin
    )

    val writerTool = Agent.deriveAgentTool(
      writerAgent,
      agentName = Some("Writer"),
      description = "Request creative writing on a given topic or prompt"
    )

    val editorTool = Agent.deriveAgentTool(
      editorAgent,
      agentName = Some("Editor"),
      description = "Get editorial feedback and improvements for a piece of text"
    )

    val publisherRegistry = ToolRegistry()
      .register(writerTool)
      .register(editorTool)

    val publisherAgent = OneShotAgent(
      systemPrompt = """You are a publisher that manages the writing process.
        |When given a writing request:
        |1. Ask the Writer to create content
        |2. Ask the Editor to review it
        |3. Present the final polished version to the user""".stripMargin,
      toolRegistry = Some(publisherRegistry),
      maxTurns = 15
    )

    val writingPrompt = "Write a haiku about programming"
    println(s"User: $writingPrompt")
    val publishedWork = publisherAgent.chat(writingPrompt)
    println(s"Publisher: $publishedWork\n")

    println("=== Example Complete ===")
    println(
      "\nNote: Agent-to-agent communication enables powerful multi-agent architectures"
    )
    println(
      "where specialized agents collaborate to solve complex problems."
    )
  }
}
