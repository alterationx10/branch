package dev.alteration.branch.hollywood.tools

import dev.alteration.branch.friday.Json
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
    ) extends CallableTool[String] {
      def execute(): scala.util.Try[String] = scala.util.Success(s"$stringParam-$intParam-$boolParam")
    }

    // Create the executor
    val executor = ToolExecutor.derived[TestTool]

    // Create Json arguments
    val jsonArgs = Json.JsonObject(
      Map(
        "stringParam" -> Json.JsonString("hello"),
        "intParam"    -> Json.JsonNumber(42),
        "boolParam"   -> Json.JsonBool(true)
      )
    )

    // Execute
    val result = executor.execute(jsonArgs)

    // Verify - result should be Json
    assertEquals(result, Json.JsonString("hello-42-true"))
  }

  test("ToolExecutor should handle URL strings without quotes") {
    @schema.Tool("URL test tool")
    case class UrlTestTool(
        @Param("A URL") url: String
    ) extends CallableTool[String] {
      def execute(): scala.util.Try[String] = scala.util.Success(url)
    }

    val executor = ToolExecutor.derived[UrlTestTool]

    val jsonArgs = Json.JsonObject(
      Map(
        "url" -> Json.JsonString("https://example.com")
      )
    )

    val result = executor.execute(jsonArgs)
    assertEquals(result, Json.JsonString("https://example.com"))
  }

  test("ToolExecutor should return Json for numeric results") {
    @schema.Tool("Math tool")
    case class MathTool(
        @Param("a number") x: Int,
        @Param("a number") y: Int
    ) extends CallableTool[Int] {
      def execute(): scala.util.Try[Int] = scala.util.Success(x + y)
    }

    val executor = ToolExecutor.derived[MathTool]
    val jsonArgs = Json.JsonObject(
      Map(
        "x" -> Json.JsonNumber(10),
        "y" -> Json.JsonNumber(32)
      )
    )

    val result = executor.execute(jsonArgs)
    assertEquals(result, Json.JsonNumber(42))
  }
}
