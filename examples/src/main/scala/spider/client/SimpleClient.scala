package spider.client

import dev.alteration.branch.spider.client.*
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import scala.util.{Try, Success, Failure}

/** A simple HTTP client example using the Spider Client.
  *
  * This example demonstrates:
  * - Creating an HTTP client
  * - Making GET requests
  * - Making POST requests with JSON
  * - Handling responses
  * - Error handling
  *
  * Note: This example makes requests to httpbin.org, a public HTTP testing service.
  * Make sure you have internet connectivity to run this example.
  */
object SimpleClient {

  def main(args: Array[String]): Unit = {
    println("=== Spider HTTP Client Example ===")
    println()

    // Create a default client
    val client = Client.defaultClient

    // Example 1: Simple GET request
    println("1. Making a GET request to httpbin.org/get")
    val getRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://httpbin.org/get"))
      .GET()
      .build()

    Try(client.send(getRequest, BodyHandlers.ofString())) match {
      case Success(response) =>
        println(s"   Status: ${response.statusCode()}")
        println(s"   Body preview: ${response.body().take(200)}...")
        println()

      case Failure(error) =>
        println(s"   Error: ${error.getMessage}")
        println()
    }

    // Example 2: GET request with query parameters
    println("2. Making a GET request with query parameters")
    val paramRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://httpbin.org/get?name=Alice&city=Wonderland"))
      .GET()
      .build()

    Try(client.send(paramRequest, BodyHandlers.ofString())) match {
      case Success(response) =>
        println(s"   Status: ${response.statusCode()}")
        println(s"   Body preview: ${response.body().take(200)}...")
        println()

      case Failure(error) =>
        println(s"   Error: ${error.getMessage}")
        println()
    }

    // Example 3: POST request with JSON body
    println("3. Making a POST request with JSON body")
    val jsonBody = """{"name": "Alice", "age": 30, "city": "Wonderland"}"""
    val postRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://httpbin.org/post"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
      .build()

    Try(client.send(postRequest, BodyHandlers.ofString())) match {
      case Success(response) =>
        println(s"   Status: ${response.statusCode()}")
        println(s"   Body preview: ${response.body().take(200)}...")
        println()

      case Failure(error) =>
        println(s"   Error: ${error.getMessage}")
        println()
    }

    // Example 4: Custom client with timeout
    println("4. Creating a custom client with 5-second timeout")
    val customClient = Client.build(
      builder => builder.connectTimeout(java.time.Duration.ofSeconds(5))
    )

    val timeoutRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://httpbin.org/delay/2"))
      .timeout(java.time.Duration.ofSeconds(10))
      .GET()
      .build()

    Try(customClient.send(timeoutRequest, BodyHandlers.ofString())) match {
      case Success(response) =>
        println(s"   Status: ${response.statusCode()}")
        println(s"   Request completed within timeout")
        println()

      case Failure(error) =>
        println(s"   Error: ${error.getMessage}")
        println()
    }

    // Example 5: Handling headers
    println("5. Inspecting response headers")
    val headerRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://httpbin.org/response-headers?Custom-Header=Hello"))
      .GET()
      .build()

    Try(client.send(headerRequest, BodyHandlers.ofString())) match {
      case Success(response) =>
        println(s"   Status: ${response.statusCode()}")
        println("   Response headers:")
        response.headers().map().forEach { (name, values) =>
          println(s"     $name: ${values.get(0)}")
        }
        println()

      case Failure(error) =>
        println(s"   Error: ${error.getMessage}")
        println()
    }

    println("=== Examples completed ===")
  }
}
