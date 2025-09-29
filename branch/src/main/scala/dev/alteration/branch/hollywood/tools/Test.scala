package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.hollywood.tools.schema.{
  Param,
  Tool as ToolS,
  ToolRegistry,
  ToolSchema
}
import dev.alteration.branch.hollywood.tools.{
  CallableTool,
  OllamaRequest,
  OllamaResponse,
  RequestFunction,
  RequestMessage,
  RequestToolCall,
  Tool
}
import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.JsonString

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
) extends CallableTool[Double] {
  override def execute(): Double = 72.0
}

@ToolS("Convert temperature from the TemperatureUnit to Kelvin")
case class KelvinService(
    @Param("The temperature value") temperature: Double,
    @Param("The TemperatureUnit") unit: TemperatureUnit
) extends CallableTool[Double] {

  override def execute(): Double = unit match {
    case Celsius    => temperature - 273.15
    case Fahrenheit => (temperature - 273.15) * 1.8 + 32
  }
}

object SimpleToolExample extends App {

  val client = HttpClient.newHttpClient()

  given Conversion[String, TemperatureUnit] = {
    case "Celsius"    => Celsius
    case "Fahrenheit" => Fahrenheit
  }

  // Register tools
  ToolRegistry.register[WeatherService]
  ToolRegistry.register[KelvinService]

  // 1. Get tool definitions from registry
  val toolDefinitions = ToolRegistry.getFunctionDefinitions
  val tools           = toolDefinitions.map(fd => Tool("function", fd))

  // 2. Build initial request
  val initialRequest = OllamaRequest(
    model = "gpt-oss",
    messages = List(
      RequestMessage(
        role = "user",
        content = Some(
          "What's the temperature in Paris in Fahrenheit according to the WeatherService?"
        )
      )
    ),
    tools = Some(tools),
    stream = false
  )

  val requestBody =
    OllamaRequest.derived$JsonCodec.encode(initialRequest).toString

  println("Request:")
  println(requestBody)
  println("\n---\n")

  // 3. Call Ollama
  val request = HttpRequest
    .newBuilder()
    .uri(URI.create("http://localhost:8080/api/chat"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build()

  val response = client.send(request, HttpResponse.BodyHandlers.ofString())

  println("Response:")
  println(response.body())
  println("\n---\n")

  val responseJson = OllamaResponse.derived$JsonCodec.decode(response.body())
  println("First Response:")
  println(responseJson)
  println("\n---\n")

  val ollamaResponse = responseJson.get

  // 4. Check if there's a tool call
  ollamaResponse.message.tool_calls.flatMap(_.headOption) match {
    case Some(toolCall) =>
      val functionName = toolCall.function.name
      val arguments    = toolCall.function.arguments

      println(s"Tool call detected: $functionName")
      println(s"Arguments: $arguments")

      // Arguments are now a JSON object, not a string
      val argsMap = arguments.objVal.map { case (k, v) =>
        k -> (v match {
          case Json.JsonString(s) => s
          case other              => other.toString.stripPrefix("\"").stripSuffix("\"")
        })
      }

      val location = argsMap.getOrElse("location", "")
      val unitStr  = argsMap.getOrElse("unit", "Fahrenheit")

      // 6. Execute the tool using registry
      val result = ToolRegistry
        .execute(functionName, argsMap)
        .getOrElse("Tool execution failed")
      println(s"\nTool result: $result")

      // Create contextual result message
      val contextualResult =
        s"The current temperature in $location is $result degrees ${unitStr.toLowerCase}."

      // 7. Send result back to model
      val followUpRequest = OllamaRequest(
        model = "gpt-oss",
        messages = List(
          RequestMessage(
            role = "user",
            content = Some(
              "What's the temperature in Paris in Fahrenheit according to the WeatherService?"
            )
          ),
          RequestMessage(
            role = "assistant",
            tool_calls = Some(
              List(
                RequestToolCall(
                  function = RequestFunction(
                    name = functionName,
                    arguments = arguments
                  )
                )
              )
            )
          ),
          RequestMessage(
            role = "tool",
            content = Some(contextualResult),
            tool_call_id = Some("call_generated_id")
          )
        ),
        stream = false
      )

      val followUpRequestBody =
        OllamaRequest.derived$JsonCodec.encode(followUpRequest).toString

      println("\nFollow-up Request:")
      println(followUpRequestBody)

      val followUpHttpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:8080/api/chat"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(followUpRequestBody))
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
