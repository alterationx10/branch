import dev.alteration.branch.hollywood.tools.schema.{Param, ToolRegistry, ToolSchema, Tool as ToolS}
import dev.alteration.branch.hollywood.tools.{OllamaResponse, Tool}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

// Tool service
sealed trait TemperatureUnit
case object Celsius    extends TemperatureUnit
case object Fahrenheit extends TemperatureUnit

@ToolS("Get temperature in specific unit")
case class WeatherService(
    @Param("The location") location: String,
    @Param("Temperature unit") unit: TemperatureUnit
) extends Tool[Double] {
  override def execute(): Double = 72.0
}

object SimpleToolExample extends App {

  val client         = HttpClient.newHttpClient()

  given Conversion[String,TemperatureUnit] = {
    case "Celsius" => Celsius
    case "Fahrenheit" => Fahrenheit
  }

  // Register tools
  ToolRegistry.register[WeatherService]

  // 1. Get tool schemas from registry
  val toolJson = ToolRegistry.getSchemasJson

  // 2. Build initial request
  val requestBody = s"""{
    "model": "gpt-oss",
    "messages": [
      {
        "role": "user",
        "content": "What's the temperature in Paris in Celsius?"
      }
    ],
    "tools": $toolJson,
    "stream": false
  }"""

  println("Request:")
  println(requestBody)
  println("\n---\n")

  // 3. Call Ollama
  val request = HttpRequest
    .newBuilder()
    .uri(URI.create("http://localhost:11434/api/chat"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build()

  val response = client.send(request, HttpResponse.BodyHandlers.ofString())

  val responseJson = OllamaResponse.derived$JsonCodec.decode(response.body())
  println("First Response:")
  println(responseJson)
  println("\n---\n")

  responseJson.get

  // 4. Check if there's a tool call
  val toolCallPattern =
    """"function":\s*\{\s*"name":\s*"([^"]+)",\s*"arguments":\s*\{([^}]+)\}""".r

  toolCallPattern.findFirstMatchIn(response.body()) match {
    case Some(m) =>
      val functionName = m.group(1)
      val arguments    = m.group(2)

      println(s"Tool call detected: $functionName")
      println(s"Arguments: $arguments")

      // 5. Parse arguments
      val locationPattern = """"location":\s*"([^"]+)"""".r
      val unitPattern     = """"unit":\s*"([^"]+)"""".r

      val location = locationPattern
        .findFirstMatchIn(arguments)
        .map(_.group(1))
        .getOrElse("")
      val unitStr  = unitPattern
        .findFirstMatchIn(arguments)
        .map(_.group(1))
        .getOrElse("Fahrenheit")

      val args = Map(
        "location" -> location,
        "unit" -> unitStr
      )

      // 6. Execute the tool using registry
      val result = ToolRegistry.execute(functionName, args)
        .getOrElse("Tool execution failed")
      println(s"\nTool result: $result")

      // Create contextual result message
      val contextualResult = s"The current temperature in $location is $result degrees ${unitStr.toLowerCase}."

      // 7. Send result back to model
      val followUpRequest = s"""{
        "model": "gpt-oss",
        "messages": [
          {
            "role": "user",
            "content": "What's the temperature in Paris in Celsius?"
          },
          {
            "role": "assistant",
            "content": "",
            "tool_calls": [
              {
                "function": {
                  "name": "$functionName",
                  "arguments": {
                    "location": "$location",
                    "unit": "$unitStr"
                  }
                }
              }
            ]
          },
          {
            "role": "tool",
            "content": "$contextualResult"
          }
        ],
        "stream": false
      }"""

      println("\nFollow-up Request:")
      println(followUpRequest)

      val followUpHttpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:11434/api/chat"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(followUpRequest))
        .build()

      val finalResponse =
        client.send(followUpHttpRequest, HttpResponse.BodyHandlers.ofString())

      println("\n---\n")
      println("Final Response:")
      println(finalResponse.body())

    case None =>
      println("No tool call detected")
  }
}
