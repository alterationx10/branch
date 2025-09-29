package dev.alteration.branch.hollywood

import dev.alteration.branch.hollywood.api.*
import dev.alteration.branch.hollywood.api.ChatCompletionsResponse.derived$JsonCodec
import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.tools.{CallableTool, ToolRegistry, schema}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.util.Random

@schema.Tool("Calculate factorial of a number")
case class FactorialTool(
    @Param("The number to calculate factorial for") n: Int
) extends CallableTool[Long] {

  override def execute(): Long = {
    if (n < 0) throw new IllegalArgumentException("Factorial is not defined for negative numbers")
    if (n == 0 || n == 1) 1L
    else (2L to n.toLong).product
  }
}

@schema.Tool("Generate a random number between min and max (inclusive)")
case class RandomNumberTool(
    @Param("Minimum value") min: Int,
    @Param("Maximum value") max: Int
) extends CallableTool[Int] {

  override def execute(): Int = {
    Random.between(min, max + 1)
  }
}

@schema.Tool("Check if a number is prime")
case class PrimeCheckTool(
    @Param("The number to check for primality") number: Int
) extends CallableTool[Boolean] {

  override def execute(): Boolean = {
    if (number <= 1) false
    else if (number <= 3) true
    else if (number % 2 == 0 || number % 3 == 0) false
    else {
      var i = 5
      while (i * i <= number) {
        if (number % i == 0 || number % (i + 2) == 0) return false
        i += 6
      }
      true
    }
  }
}

object MultiTurnExample extends App {

  ToolRegistry.register[FactorialTool]
  ToolRegistry.register[RandomNumberTool]
  ToolRegistry.register[PrimeCheckTool]

  val client = HttpClient.newHttpClient()

  // Keep track of conversation messages
  var conversationMessages = List(
    ChatMessage(
      role = "user",
      content = Some(
        "Using the tools you have: 1. get a random number 2. get the factorial of it 3. check if the factorial is a prime number. Report the results"
      )
    )
  )

}