package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.tools.ToolRegistry.register

import scala.io.StdIn

object AgentExample extends App {

  val toolRegistry = ToolRegistry()
  toolRegistry.register[FactorialTool]
  toolRegistry.register[RandomNumberTool]
  toolRegistry.register[PrimeCheckTool]

  
  val agent = ConversationalAgent(toolRegistry = Some(toolRegistry))

  var continue = true
  while (continue) {
    print("> ")
    val input = StdIn.readLine()

    if (input != null && input.trim.startsWith("quit")) {
      continue = false
      println("Goodbye!")
    } else if (input != null && input.trim.nonEmpty) {
      val response = agent.chat(input)
      println(s"Agent: $response")
    }
  }

}
