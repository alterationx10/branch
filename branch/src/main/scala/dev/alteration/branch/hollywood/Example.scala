package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api._
import dev.alteration.branch.friday.Json
import java.net.http.{HttpRequest, HttpResponse}
import java.net.URI
import java.net.http.HttpClient

object Example extends App {

  val client = HttpClient.newHttpClient()

  // Build a minimal chat completions request
  val requestBody = ChatCompletionsRequest(
    messages = List(
      ChatMessage(
        role = "user",
        content = Some(Json.JsonString("Hello!"))
      )
    ),
    model = "gpt-oss"
  )

  // Encode the request to JSON
  val jsonBody = requestBody.toJson.toString

  // Create the HTTP POST request
  val httpRequest = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/v1/chat/completions"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .build()

  // Send the request and print the response
  val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
  println(s"Status: ${response.statusCode()}")
  println(s"Body: ${response.body()}")

  val resp = ChatCompletionsResponse.derived$JsonCodec.decode(response.body())
  println(resp)
  
}
