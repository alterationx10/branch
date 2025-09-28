import dev.alteration.branch.hollywood.tools.schema.{Param, ToolRegistry, ToolSchema, Tool as ToolS}
import dev.alteration.branch.hollywood.tools.{CallableTool, OllamaRequest, OllamaResponse, RequestFunction, RequestMessage, RequestToolCall}
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
  val initialRequest = OllamaRequest(
    model = "gpt-oss",
    messages = List(
      RequestMessage(
        role = "user",
        content = Some(JsonString("What's the temperature in Paris in Fahrenheit according to the WeatherService?"))
      )
    ),
    tools = Json.parse(toolJson).toOption.get, // booooo me
    stream = false
  )

  val requestBody = OllamaRequest.derived$JsonCodec.encode(initialRequest).toString

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

  val ollamaResponse = responseJson.get

  // 4. Check if there's a tool call
  ollamaResponse.message.tool_calls.headOption match {
    case Some(toolCall) =>
      val functionName = toolCall.function.name
      val args = toolCall.function.arguments

      println(s"Tool call detected: $functionName")
      println(s"Arguments: $args")

      val location = args.getOrElse("location", "")
      val unitStr = args.getOrElse("unit", "Fahrenheit")

      // 6. Execute the tool using registry
      val result = ToolRegistry.execute(functionName, args)
        .getOrElse("Tool execution failed")
      println(s"\nTool result: $result")

      // Create contextual result message
      val contextualResult = s"The current temperature in $location is $result degrees ${unitStr.toLowerCase}."

      // 7. Send result back to model
      val followUpRequest = OllamaRequest(
        model = "gpt-oss",
        messages = List(
          RequestMessage(
            role = "user",
            content = "What's the temperature in Paris in Celsius?"
          ),
          RequestMessage(
            role = "assistant",
            content = "",
            tool_calls = List(
              RequestToolCall(
                function = RequestFunction(
                  name = functionName,
                  arguments = Map(
                    "location" -> location,
                    "unit" -> unitStr
                  )
                )
              )
            )
          ),
          RequestMessage(
            role = "tool",
            content = contextualResult
          )
        ),
        stream = false
      )

      val followUpRequestBody = OllamaRequest.derived$JsonCodec.encode(followUpRequest).toString

      println("\nFollow-up Request:")
      println(followUpRequestBody)

      val followUpHttpRequest = HttpRequest
        .newBuilder()
        .uri(URI.create("http://localhost:11434/api/chat"))
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
