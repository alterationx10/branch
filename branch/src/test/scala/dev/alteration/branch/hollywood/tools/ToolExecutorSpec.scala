package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.{Json, JsonCodec}
import dev.alteration.branch.hollywood.tools.schema.Param
import munit.FunSuite

class ToolExecutorSpec extends FunSuite {

  test("ToolExecutor should decode Json arguments correctly") {
    // Define a test tool
    @schema.Tool("Test tool with multiple parameter types")
    case class TestTool(
        @Param("a string parameter") stringParam: String,
        @Param("an integer parameter") intParam: Int,
        @Param("a boolean parameter") boolParam: Boolean
    ) extends CallableTool[String] derives JsonCodec {
      def execute(): String = s"$stringParam-$intParam-$boolParam"
    }

    // Create the executor
    val executor = ToolExecutor.derived[TestTool]

    // Create Json arguments
    val jsonArgs = Json.JsonObject(Map(
      "stringParam" -> Json.JsonString("hello"),
      "intParam" -> Json.JsonNumber(42),
      "boolParam" -> Json.JsonBool(true)
    ))

    // Execute
    val result = executor.execute(jsonArgs)

    // Verify
    assertEquals(result, "hello-42-true")
  }

  test("ToolExecutor should handle URL strings without quotes") {
    @schema.Tool("URL test tool")
    case class UrlTestTool(
        @Param("A URL") url: String
    ) extends CallableTool[String] derives JsonCodec {
      def execute(): String = url
    }

    val executor = ToolExecutor.derived[UrlTestTool]

    val jsonArgs = Json.JsonObject(Map(
      "url" -> Json.JsonString("https://example.com")
    ))

    val result = executor.execute(jsonArgs)
    assertEquals(result, "https://example.com")
  }
}
