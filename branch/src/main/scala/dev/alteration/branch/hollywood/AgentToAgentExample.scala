package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.tools.ToolRegistry

/** Example demonstrating agent-to-agent communication where a coordinator agent
  * collaborates with specialized agents to solve a complex task.
  */
object AgentToAgentExample {

  def main(args: Array[String]): Unit = {
    println("=== Agent-to-Agent Communication Example ===\n")

    // Create specialized agents
    val mathAgent = OneShotAgent.forTask(
      taskName = "Mathematics Expert",
      taskDescription =
        "You are a mathematics expert. Answer math questions with clear explanations and show your work.",
      outputFormat = Some("Provide the answer followed by a brief explanation")
    )

    val writerAgent = OneShotAgent.forTask(
      taskName = "Technical Writer",
      taskDescription =
        "You are a technical writer. Transform technical information into clear, accessible prose.",
      outputFormat =
        Some("Write in a friendly, educational tone suitable for a blog post")
    )

    // Derive tools from the specialized agents
    val (mathToolSchema, mathToolExecutor) = Agent.deriveAgentTool(
      mathAgent,
      agentName = Some("math_expert"),
      description =
        "Consult a mathematics expert for calculations and mathematical explanations"
    )

    val (writerToolSchema, writerToolExecutor) = Agent.deriveAgentTool(
      writerAgent,
      agentName = Some("writer"),
      description =
        "Consult a technical writer to polish and improve written content"
    )

    // Create a coordinator agent with access to both specialized agents
    val coordinatorRegistry = ToolRegistry()
      .register(mathToolSchema, mathToolExecutor)
      .register(writerToolSchema, writerToolExecutor)

    val coordinator = new ConversationalAgent(
      toolRegistry = Some(coordinatorRegistry),
      maxTurns = 20,
      onTurn = Some((turn, msg) => {
        println(s"--- Turn $turn ---")
        msg.content.foreach(c => println(s"Content: ${c.take(100)}..."))
        msg.tool_calls.foreach { calls =>
          calls.foreach { call =>
            println(
              s"Tool Call: ${call.function.name}(${call.function.arguments.take(50)}...)"
            )
          }
        }
        println()
      })
    )

    // Example 1: Multi-agent collaboration
    println("Example 1: Coordinator orchestrating multiple agents\n")
    val task1 =
      """I need to write a blog post explaining the Fibonacci sequence.
        |First, ask the math expert to explain what the Fibonacci sequence is and calculate the first 10 numbers.
        |Then, ask the writer to turn that explanation into an engaging blog post introduction.""".stripMargin

    val result1 = coordinator.chat(task1)
    println(s"Final Result:\n$result1\n")

    println("\n" + "=" * 60 + "\n")

    // Example 2: Iterative refinement
    println("Example 2: Iterative agent collaboration\n")
    val task2 =
      """I need help with a math problem: If a train travels 120 miles in 2 hours, and then 180 miles in 3 hours, what is its average speed?
        |First ask the math expert to solve it, then ask the writer to explain the solution in a way a 10-year-old could understand.""".stripMargin

    val result2 = coordinator.chat(task2)
    println(s"Final Result:\n$result2\n")

    println("\n" + "=" * 60 + "\n")

    // Example 3: Back-and-forth conversation
    println("Example 3: Multi-turn conversation with follow-ups\n")
    coordinator.chat(
      "Ask the math expert to explain what prime numbers are and give examples"
    )
    val result3 = coordinator.chat(
      "Now ask the writer to create a fun story that teaches kids about prime numbers using the examples the math expert provided"
    )
    println(s"Final Result:\n$result3\n")
  }
}
