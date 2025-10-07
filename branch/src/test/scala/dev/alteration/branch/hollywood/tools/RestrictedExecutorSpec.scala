package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.hollywood.tools.provided.HttpClientTool
import munit.FunSuite

import scala.util.Try

class RestrictedExecutorSpec extends FunSuite {

  test("ToolPolicy.allowAll should permit all operations") {
    val policy = ToolPolicy.allowAll[HttpClientTool]
    val tool = HttpClientTool(
      url = "https://example.com",
      method = "GET"
    )

    val result = policy.validate(tool)
    assert(result.isSuccess, "allowAll policy should permit all operations")
  }

  test("ToolPolicy.denyAll should block all operations") {
    val policy = ToolPolicy.denyAll[HttpClientTool]("Blocked for testing")
    val tool = HttpClientTool(
      url = "https://example.com",
      method = "GET"
    )

    val result = policy.validate(tool)
    assert(result.isFailure, "denyAll policy should block all operations")
    assert(
      result.failed.get.getMessage.contains("Blocked for testing"),
      "Error message should contain custom reason"
    )
  }

  test("ToolPolicy.fromValidator should use custom validation logic") {
    // Only allow GET requests to example.com
    val policy = ToolPolicy.fromValidator[HttpClientTool] { tool =>
      if (tool.method.toLowerCase != "get") {
        Try(throw new SecurityException("Only GET requests allowed"))
      } else if (!tool.url.contains("example.com")) {
        Try(throw new SecurityException("Only example.com allowed"))
      } else {
        Try(())
      }
    }

    val validTool = HttpClientTool(
      url = "https://example.com",
      method = "GET"
    )
    assert(
      policy.validate(validTool).isSuccess,
      "Valid tool should pass policy"
    )

    val invalidMethod = HttpClientTool(
      url = "https://example.com",
      method = "POST"
    )
    assert(
      policy.validate(invalidMethod).isFailure,
      "POST should be blocked"
    )

    val invalidUrl = HttpClientTool(
      url = "https://evil.com",
      method = "GET"
    )
    assert(policy.validate(invalidUrl).isFailure, "Non-example.com should be blocked")
  }

  test("RestrictedExecutor should enforce policy") {
    // Create a custom policy that blocks all POST requests
    val policy = ToolPolicy.fromValidator[HttpClientTool] { tool =>
      if (tool.method.toLowerCase == "post") {
        Try(throw new SecurityException("POST requests blocked"))
      } else {
        Try(())
      }
    }

    // Register with policy
    val restrictedRegistry = ToolRegistry().registerWithPolicy(policy)

    import dev.alteration.branch.friday.Json._

    // Try a POST request (should be blocked)
    val postArgs = JsonObject(
      Map(
        "url" -> JsonString("https://example.com"),
        "method" -> JsonString("POST")
      )
    )

    val result = restrictedRegistry.execute(
      "dev.alteration.branch.hollywood.tools.provided.HttpClientTool",
      postArgs
    )

    result match {
      case Some(JsonString(msg)) =>
        assert(
          msg.contains("Policy violation") && msg.contains("POST requests blocked"),
          s"Should block POST with policy error: $msg"
        )
      case _                     => fail("Expected policy violation error message")
    }
  }
}
