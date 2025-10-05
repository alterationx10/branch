package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.{Agent, OneShotAgent}
import dev.alteration.branch.testkit.fixtures.LlamaServerFixture

class CallableToolSpec extends LlamaServerFixture {

  test("Agent using a simple tool") {

    // Define a simple calculator tool
    @schema.Tool("Add two numbers together")
    case class Calculator(
        @Param("a number") a: Int,
        @Param("a number") b: Int
    ) extends CallableTool[Int] {
      def execute(): scala.util.Try[Int] = scala.util.Success(a + b)
    }

    // Create a tool registry and register the calculator
    val toolRegistry = ToolRegistry()
      .register[Calculator]

    // Create an agent with the tool
    val agent = OneShotAgent(
      systemPrompt =
        "You are a helpful assistant with access to a calculator tool. Use it when asked to perform calculations.",
      toolRegistry = Some(toolRegistry)
    )

    // Test the agent using the tool
    val response = agent.chat("What is 15 plus 27?")
    assert(response.nonEmpty)
    assert(response.contains("42"))

  }

  test("Agent using multiple tools") {
    // Define calculator tools
    @schema.Tool("Add two numbers")
    case class Add(
        @Param("first number") a: Int,
        @Param("second number") b: Int
    ) extends CallableTool[Int] {
      def execute(): scala.util.Try[Int] = scala.util.Success(a + b)
    }

    @schema.Tool("Multiply two numbers")
    case class Multiply(
        @Param("first number") a: Int,
        @Param("second number") b: Int
    ) extends CallableTool[Int] {
      def execute(): scala.util.Try[Int] = scala.util.Success(a * b)
    }

    // Create a tool registry with multiple tools
    val toolRegistry = ToolRegistry()
      .register[Add]
      .register[Multiply]

    // Create an agent with multiple tools
    val agent = OneShotAgent(
      systemPrompt =
        "You are a math assistant with tools for addition and multiplication.",
      toolRegistry = Some(toolRegistry)
    )

    // Test the agent using multiple tools
    val response = agent.chat("Calculate (5 + 3) * 2")
    assert(response.nonEmpty)
    assert(response.contains("16"))
  }

  test("Agent using another agent as a tool") {
    // Create a specialized calculator agent
    val calculatorAgent = OneShotAgent(
      systemPrompt =
        "You are a calculator. Perform arithmetic operations accurately."
    )

    // Derive a tool from the calculator agent
    val calculatorTool = Agent.deriveAgentTool(
      calculatorAgent,
      agentName = Some("calculator"),
      description = "Use this tool to perform arithmetic calculations"
    )

    // Create a tool registry and register the agent tool
    val toolRegistry = ToolRegistry()
      .register(calculatorTool)

    // Create a main agent that can use the calculator agent
    val mainAgent = OneShotAgent(
      systemPrompt =
        "You are an assistant. When asked to do math, use the calculator tool.",
      toolRegistry = Some(toolRegistry)
    )

    // Test agent-to-agent communication
    val response = mainAgent.chat("What is 100 divided by 4?")
    assert(response.nonEmpty)
    assert(response.contains("25"))
  }
}
