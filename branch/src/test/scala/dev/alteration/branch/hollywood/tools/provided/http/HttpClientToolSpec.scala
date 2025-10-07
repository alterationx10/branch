package dev.alteration.branch.hollywood.tools.provided.http

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.tools.provided.http.HttpClientTool
import munit.FunSuite

class HttpClientToolSpec extends FunSuite {

  test("HttpClientTool should execute a GET request") {
    val tool   = HttpClientTool(url = "https://httpbin.org/get", method = "GET")
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(
        body.contains("httpbin.org"),
        "Response should contain httpbin.org"
      )
    }
  }

  test("HttpClientTool should execute a POST request with body") {
    val tool   = HttpClientTool(
      url = "https://httpbin.org/post",
      method = "POST",
      body = Some("test data")
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(body.contains("test data"), "Response should contain posted data")
    }
  }

  test("HttpClientTool should include custom headers") {
    val headers = """{"X-Custom-Header": "test-value"}"""
    val tool    = HttpClientTool(
      url = "https://httpbin.org/headers",
      method = "GET",
      headers = Some(headers)
    )
    val result  = tool.execute()
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(
        body.contains("X-Custom-Header"),
        "Response should contain custom header"
      )
    }
  }

  test("HttpClientTool should execute a PUT request") {
    val tool   = HttpClientTool(
      url = "https://httpbin.org/put",
      method = "PUT",
      body = Some("""{"key": "value"}""")
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
  }

  test("HttpClientTool should execute a DELETE request") {
    val tool   = HttpClientTool(
      url = "https://httpbin.org/delete",
      method = "DELETE"
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
  }

  test("HttpClientTool should register with ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[HttpClientTool]

    val tools = registry.getRegisteredToolNames
    assert(
      tools.contains(
        "dev.alteration.branch.hollywood.tools.provided.HttpClientTool"
      ),
      s"Registry should contain HttpClientTool. Got: $tools"
    )
  }

  test("HttpClientTool should execute via ToolRegistry") {
    import dev.alteration.branch.friday.Json.*

    val registry = ToolRegistry()
    registry.register[HttpClientTool]

    val args = JsonObject(
      Map(
        "url"    -> JsonString("https://httpbin.org/get"),
        "method" -> JsonString("GET")
      )
    )

    val result = registry.execute(
      "dev.alteration.branch.hollywood.tools.provided.HttpClientTool",
      args
    )
    assert(result.isDefined, "Execution should return a result")
    result.foreach {
      case JsonString(value) =>
        assert(
          value.contains("httpbin.org"),
          "Result should contain httpbin.org"
        )
      case _                 => fail("Expected JsonString result")
    }
  }

  test("HttpClientTool should default to GET when method is unknown") {
    val tool   = HttpClientTool(
      url = "https://httpbin.org/get",
      method = "INVALID"
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed with default GET method")
  }
}
