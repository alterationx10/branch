package hollywood.tools

import dev.alteration.branch.hollywood.OneShotAgent
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolRegistry}
import dev.alteration.branch.hollywood.tools.schema.{Tool, Param}
import dev.alteration.branch.friday.JsonEncoder
import scala.util.{Try, Success}

/** Example showing how to use agents with custom tools for function calling.
  *
  * Tools allow agents to interact with external systems and perform actions
  * beyond text generation. This example demonstrates:
  *   - Defining custom tools with @Tool annotation
  *   - Registering tools with an agent
  *   - How agents automatically call tools when needed
  *
  * This example assumes you have an OpenAI-compatible LLM server running:
  *   - Default URL: http://localhost:8080
  *   - Or set LLAMA_SERVER_URL environment variable
  *   - Server must support tool/function calling
  *
  * To run: sbt "examples/runMain hollywood.tools.AgentWithToolsExample"
  */
object AgentWithToolsExample {

  // Define custom tools as case classes with @Tool and @Param annotations
  // Tools are automatically converted to JSON schemas for the LLM

  /** Simple calculator tool for addition */
  @Tool("Add two numbers together")
  case class AddNumbers(
      @Param("first number") a: Double,
      @Param("second number") b: Double
  ) extends CallableTool[Double] {
    def execute(): Try[Double] = Success(a + b)
  }

  /** Simple calculator tool for multiplication */
  @Tool("Multiply two numbers together")
  case class MultiplyNumbers(
      @Param("first number") a: Double,
      @Param("second number") b: Double
  ) extends CallableTool[Double] {
    def execute(): Try[Double] = Success(a * b)
  }

  /** Tool to get the current weather (mock implementation) */
  @Tool("Get the current weather for a location")
  case class GetWeather(
      @Param("city or location name") location: String,
      @Param("temperature unit: celsius or fahrenheit") unit: String = "celsius"
  ) extends CallableTool[String] {
    def execute(): Try[String] = Success {
      // In a real implementation, this would call a weather API
      val temp = if (unit == "celsius") "22°C" else "72°F"
      s"The weather in $location is sunny with a temperature of $temp"
    }
  }

  /** Tool to search a mock database */
  @Tool("Search a database for documents")
  case class SearchDatabase(
      @Param("search query") query: String,
      @Param("maximum number of results") limit: Int = 5
  ) extends CallableTool[String] {
    def execute(): Try[String] = Success {
      // Mock database results
      val results = List(
        "Document about functional programming in Scala",
        "Article on actor systems",
        "Tutorial on HTTP clients",
        "Guide to JSON handling"
      ).take(limit)

      s"Found ${results.size} results for '$query':\n" +
        results.map(r => s"- $r").mkString("\n")
    }
  }

  def main(args: Array[String]): Unit = {
    println("=== Hollywood Agent with Tools Example ===\n")

    // JsonEncoder instances for basic types are automatically available
    import dev.alteration.branch.friday.JsonEncoder.given

    // Example 1: Agent with calculator tools
    println("--- Example 1: Calculator Agent ---")
    val calculatorRegistry = ToolRegistry()
      .register[AddNumbers]
      .register[MultiplyNumbers]

    println(
      s"Registered tools: ${calculatorRegistry.getRegisteredToolNames.mkString(", ")}\n"
    )

    val calculatorAgent = OneShotAgent(
      systemPrompt =
        "You are a helpful math assistant. Use the available tools to perform calculations.",
      toolRegistry = Some(calculatorRegistry)
    )

    val mathQuery = "What is 15.5 plus 27.3, and then multiply that result by 2?"
    println(s"User: $mathQuery")
    val mathResult = calculatorAgent.chat(mathQuery)
    println(s"Agent: $mathResult\n")

    // Example 2: Agent with weather tool
    println("--- Example 2: Weather Assistant ---")
    val weatherRegistry = ToolRegistry()
      .register[GetWeather]

    val weatherAgent = OneShotAgent(
      systemPrompt =
        "You are a weather assistant. Use the GetWeather tool to provide weather information.",
      toolRegistry = Some(weatherRegistry)
    )

    val weatherQuery = "What's the weather like in Tokyo and London?"
    println(s"User: $weatherQuery")
    val weatherResult = weatherAgent.chat(weatherQuery)
    println(s"Agent: $weatherResult\n")

    // Example 3: Agent with multiple tool types
    println("--- Example 3: Multi-Purpose Assistant ---")
    val multiRegistry = ToolRegistry()
      .register[AddNumbers]
      .register[GetWeather]
      .register[SearchDatabase]

    println(
      s"Available tools: ${multiRegistry.getRegisteredToolNames.mkString(", ")}\n"
    )

    val multiAgent = OneShotAgent(
      systemPrompt = """You are a versatile assistant with access to multiple tools.
        |Use them appropriately based on the user's request.""".stripMargin,
      toolRegistry = Some(multiRegistry)
    )

    val queries = List(
      "Search the database for 'scala tutorials'",
      "What's 100 plus 250?",
      "Check the weather in Paris"
    )

    queries.foreach { query =>
      println(s"User: $query")
      val result = multiAgent.chat(query)
      println(s"Agent: $result\n")
    }

    // Example 4: Tool with complex output
    println("--- Example 4: Research Assistant ---")
    @Tool("Analyze text content")
    case class AnalyzeText(
        @Param("text to analyze") text: String,
        @Param("type of analysis: summary or sentiment") analysisType: String = "summary"
    ) extends CallableTool[String] {
      def execute(): Try[String] = Success {
        analysisType match {
          case "summary"   =>
            s"Summary: Text is ${text.split("\\s+").length} words long"
          case "sentiment" => "Sentiment: Neutral"
          case _           => "Analysis type not recognized"
        }
      }
    }

    val analysisRegistry = ToolRegistry()
      .register[AnalyzeText]
      .register[SearchDatabase]

    val researchAgent = OneShotAgent(
      systemPrompt = """You are a research assistant that can search databases and analyze text.
        |Use the tools to help answer questions.""".stripMargin,
      toolRegistry = Some(analysisRegistry)
    )

    val researchQuery =
      "Search for 'functional programming' and analyze the sentiment of what you find"
    println(s"User: $researchQuery")
    val researchResult = researchAgent.chat(researchQuery)
    println(s"Agent: $researchResult\n")

    println("=== Example Complete ===")
    println(
      "\nNote: Tool execution happens automatically during the agent conversation loop."
    )
    println(
      "The agent decides when to call tools based on the user's request and available tools."
    )
  }
}
