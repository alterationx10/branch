package dev.alteration.branch.hollywood.tools.provided.regex

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.hollywood.tools.provided.regex.RegexTool
import munit.FunSuite

class RegexToolSpec extends FunSuite {

  test("RegexTool should match a pattern") {
    val tool   = RegexTool("match", "^hello.*world$", "hello beautiful world")
    val result = tool.execute()

    assert(result.isSuccess, "Match operation should succeed")
    result.foreach { content =>
      assertEquals(content, "true", "Pattern should match")
    }
  }

  test("RegexTool should not match when pattern doesn't match") {
    val tool   = RegexTool("match", "^world", "hello world")
    val result = tool.execute()

    assert(result.isSuccess, "Match operation should succeed")
    result.foreach { content =>
      assertEquals(content, "false", "Pattern should not match")
    }
  }

  test("RegexTool should find all occurrences") {
    val tool   = RegexTool("find_all", "\\d+", "I have 3 apples and 42 oranges")
    val result = tool.execute()

    assert(result.isSuccess, "Find all operation should succeed")
    result.foreach { content =>
      assert(content.contains("Found 2 match(es)"), "Should find 2 matches")
      assert(content.contains("1. 3"), "Should find '3'")
      assert(content.contains("2. 42"), "Should find '42'")
    }
  }

  test("RegexTool should report no matches when pattern not found") {
    val tool   = RegexTool("find_all", "\\d+", "no numbers here")
    val result = tool.execute()

    assert(result.isSuccess, "Find all operation should succeed")
    result.foreach { content =>
      assert(content.contains("No matches found"), "Should report no matches")
    }
  }

  test("RegexTool should replace patterns") {
    val tool   = RegexTool(
      "replace",
      "\\d+",
      "I have 3 apples",
      replacement = Some("X")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Replace operation should succeed")
    result.foreach { content =>
      assert(
        content.contains("I have X apples"),
        "Should replace number with X"
      )
    }
  }

  test("RegexTool should fail replace without replacement text") {
    val tool   = RegexTool("replace", "\\d+", "I have 3 apples")
    val result = tool.execute()

    assert(result.isFailure, "Replace without replacement should fail")
  }

  test("RegexTool should extract groups from matches") {
    val tool   =
      RegexTool("extract", "(\\w+)@(\\w+\\.\\w+)", "Email: user@example.com")
    val result = tool.execute()

    assert(result.isSuccess, "Extract operation should succeed")
    result.foreach { content =>
      assert(content.contains("Found 1 match(es)"), "Should find 1 match")
      assert(
        content.contains("Group 0: user@example.com"),
        "Should extract full match"
      )
      assert(content.contains("Group 1: user"), "Should extract username")
      assert(content.contains("Group 2: example.com"), "Should extract domain")
    }
  }

  test("RegexTool should extract multiple groups from multiple matches") {
    val tool   = RegexTool(
      "extract",
      "(\\d+)\\s*(apples|oranges)",
      "I have 3 apples and 42 oranges"
    )
    val result = tool.execute()

    assert(result.isSuccess, "Extract operation should succeed")
    result.foreach { content =>
      assert(content.contains("Found 2 match(es)"), "Should find 2 matches")
      assert(content.contains("3 apples"), "Should find '3 apples'")
      assert(content.contains("42 oranges"), "Should find '42 oranges'")
    }
  }

  test("RegexTool should split text by pattern") {
    val tool   = RegexTool("split", ",\\s*", "apple, banana, cherry")
    val result = tool.execute()

    assert(result.isSuccess, "Split operation should succeed")
    result.foreach { content =>
      assert(
        content.contains("Split into 3 part(s)"),
        "Should split into 3 parts"
      )
      assert(content.contains("1. apple"), "Should have 'apple'")
      assert(content.contains("2. banana"), "Should have 'banana'")
      assert(content.contains("3. cherry"), "Should have 'cherry'")
    }
  }

  test("RegexTool should report when split doesn't occur") {
    val tool   = RegexTool("split", "xyz", "no split here")
    val result = tool.execute()

    assert(result.isSuccess, "Split operation should succeed")
    result.foreach { content =>
      assert(
        content.contains("No split occurred"),
        "Should report no split occurred"
      )
    }
  }

  test("RegexTool should support case-insensitive matching") {
    val tool   = RegexTool(
      "find_all",
      "hello",
      "Hello HELLO hello",
      caseInsensitive = Some(true)
    )
    val result = tool.execute()

    assert(result.isSuccess, "Case-insensitive match should succeed")
    result.foreach { content =>
      assert(content.contains("Found 3 match(es)"), "Should find 3 matches")
    }
  }

  test("RegexTool should support multiline mode") {
    val tool   = RegexTool(
      "find_all",
      "^line",
      "line 1\nline 2\nline 3",
      multiline = Some(true)
    )
    val result = tool.execute()

    assert(result.isSuccess, "Multiline match should succeed")
    result.foreach { content =>
      assert(content.contains("Found 3 match(es)"), "Should find 3 line starts")
    }
  }

  test("RegexTool should support dotall mode") {
    val tool   = RegexTool(
      "find_all",
      "start.*end",
      "start\nmiddle\nend",
      dotall = Some(true)
    )
    val result = tool.execute()

    assert(result.isSuccess, "Dotall match should succeed")
    result.foreach { content =>
      assert(
        content.contains("Found 1 match(es)"),
        "Should match across newlines"
      )
    }
  }

  test("RegexTool should fail with invalid operation") {
    val tool   = RegexTool("invalid", "test", "text")
    val result = tool.execute()

    assert(result.isFailure, "Should fail with invalid operation")
  }

  test("RegexTool should fail with invalid regex pattern") {
    val tool   = RegexTool("match", "[invalid", "text")
    val result = tool.execute()

    assert(result.isFailure, "Should fail with invalid regex")
  }

  test("RegexTool should handle complex email extraction") {
    val tool   = RegexTool(
      "find_all",
      "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
      "Contact us at info@example.com or support@test.org"
    )
    val result = tool.execute()

    assert(result.isSuccess, "Email extraction should succeed")
    result.foreach { content =>
      assert(content.contains("Found 2 match(es)"), "Should find 2 emails")
      assert(content.contains("info@example.com"), "Should find first email")
      assert(content.contains("support@test.org"), "Should find second email")
    }
  }

  test("RegexTool should register with ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[RegexTool]

    val tools = registry.getRegisteredToolNames
    assert(
      tools.contains(
        "dev.alteration.branch.hollywood.tools.provided.regex.RegexTool"
      ),
      s"Registry should contain RegexTool. Got: $tools"
    )
  }

  test("RegexTool should execute via ToolRegistry") {
    import dev.alteration.branch.friday.Json.*

    val registry = ToolRegistry()
    registry.register[RegexTool]

    val args = JsonObject(
      Map(
        "operation" -> JsonString("find_all"),
        "pattern"   -> JsonString("\\d+"),
        "text"      -> JsonString("Numbers: 1, 2, 3")
      )
    )

    val result = registry.execute(
      "dev.alteration.branch.hollywood.tools.provided.regex.RegexTool",
      args
    )

    assert(result.isDefined, "Execution should return a result")
    result.foreach {
      case JsonString(value) =>
        assert(value.contains("Found 3 match(es)"), "Should find 3 numbers")
      case _                 => fail("Expected JsonString result")
    }
  }

  test("RegexTool should handle URL replacement") {
    val tool   = RegexTool(
      "replace",
      "https?://[^\\s]+",
      "Visit https://example.com for more info",
      replacement = Some("[LINK]")
    )
    val result = tool.execute()

    assert(result.isSuccess, "URL replacement should succeed")
    result.foreach { content =>
      assert(
        content.contains("Visit [LINK] for more info"),
        "Should replace URL with [LINK]"
      )
    }
  }
}
