package dev.alteration.branch.hollywood.tools.provided.json

import dev.alteration.branch.hollywood.tools.ToolRegistry
import dev.alteration.branch.friday.Json.*
import munit.FunSuite

class JsonQueryToolSpec extends FunSuite {

  val sampleJson = """{
    "users": [
      {"id": 1, "name": "Alice", "age": 30, "email": "alice@example.com"},
      {"id": 2, "name": "Bob", "age": 25},
      {"id": 3, "name": "Charlie", "age": 35, "email": "charlie@example.com"}
    ],
    "metadata": {
      "total": 3,
      "page": 1
    }
  }"""

  test("JsonQueryTool should get value at simple path") {
    val tool   = JsonQueryTool(sampleJson, "get", path = Some("metadata.total"))
    val result = tool.execute()

    assert(result.isSuccess, "Get operation should succeed")
    result.foreach { content =>
      assertEquals(content, "3", "Should extract total value")
    }
  }

  test("JsonQueryTool should get value at nested path") {
    val tool   = JsonQueryTool(sampleJson, "get", path = Some("metadata.page"))
    val result = tool.execute()

    assert(result.isSuccess, "Get operation should succeed")
    result.foreach { content =>
      assertEquals(content, "1", "Should extract page value")
    }
  }

  test("JsonQueryTool should get array element by index") {
    val tool   = JsonQueryTool(sampleJson, "get", path = Some("users.0.name"))
    val result = tool.execute()

    assert(result.isSuccess, "Get operation should succeed")
    result.foreach { content =>
      assertEquals(content, "\"Alice\"", "Should extract first user's name")
    }
  }

  test("JsonQueryTool should get all elements with wildcard") {
    val tool   = JsonQueryTool(sampleJson, "get", path = Some("users.*.name"))
    val result = tool.execute()

    assert(result.isSuccess, "Get wildcard operation should succeed")
    result.foreach { content =>
      assert(content.contains("Alice"), "Should contain Alice")
      assert(content.contains("Bob"), "Should contain Bob")
      assert(content.contains("Charlie"), "Should contain Charlie")
    }
  }

  test("JsonQueryTool should fail when path not found") {
    val tool   = JsonQueryTool(sampleJson, "get", path = Some("nonexistent"))
    val result = tool.execute()

    assert(result.isFailure, "Should fail when path not found")
  }

  test("JsonQueryTool should map array to extract field") {
    val tool   = JsonQueryTool(
      sampleJson,
      "map",
      path = Some("users"),
      field = Some("name")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Map operation should succeed")
    result.foreach { content =>
      assert(content.contains("Extracted 3 values"), "Should extract 3 names")
      assert(content.contains("Alice"), "Should contain Alice")
      assert(content.contains("Bob"), "Should contain Bob")
      assert(content.contains("Charlie"), "Should contain Charlie")
    }
  }

  test("JsonQueryTool should map array to extract numeric field") {
    val tool   = JsonQueryTool(
      sampleJson,
      "map",
      path = Some("users"),
      field = Some("id")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Map operation should succeed")
    result.foreach { content =>
      assert(content.contains("Extracted 3 values"), "Should extract 3 IDs")
      assert(content.contains("1"), "Should contain ID 1")
      assert(content.contains("2"), "Should contain ID 2")
      assert(content.contains("3"), "Should contain ID 3")
    }
  }

  test("JsonQueryTool should filter array by field existence") {
    val tool   = JsonQueryTool(
      sampleJson,
      "filter",
      path = Some("users"),
      field = Some("email")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Filter operation should succeed")
    result.foreach { content =>
      assert(
        content.contains("Filtered 3 items to 2 items"),
        "Should filter to 2 users with email"
      )
      assert(content.contains("alice@example.com"), "Should contain Alice")
      assert(content.contains("charlie@example.com"), "Should contain Charlie")
    }
  }

  test("JsonQueryTool should get keys from object") {
    val tool   = JsonQueryTool(sampleJson, "keys", path = Some("metadata"))
    val result = tool.execute()

    assert(result.isSuccess, "Keys operation should succeed")
    result.foreach { content =>
      assert(content.contains("Object has 2 key(s)"), "Should have 2 keys")
      assert(content.contains("page"), "Should contain 'page' key")
      assert(content.contains("total"), "Should contain 'total' key")
    }
  }

  test("JsonQueryTool should get values from object") {
    val tool   = JsonQueryTool(sampleJson, "values", path = Some("metadata"))
    val result = tool.execute()

    assert(result.isSuccess, "Values operation should succeed")
    result.foreach { content =>
      assert(content.contains("Object has 2 value(s)"), "Should have 2 values")
      assert(content.contains("3"), "Should contain value 3")
      assert(content.contains("1"), "Should contain value 1")
    }
  }

  test("JsonQueryTool should check if path exists") {
    val tool   =
      JsonQueryTool(sampleJson, "exists", path = Some("users.0.email"))
    val result = tool.execute()

    assert(result.isSuccess, "Exists operation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Path should exist")
    }
  }

  test("JsonQueryTool should check if path does not exist") {
    val tool   = JsonQueryTool(sampleJson, "exists", path = Some("users.1.email"))
    val result = tool.execute()

    assert(result.isSuccess, "Exists operation should succeed")
    result.foreach { content =>
      assert(content.contains("false"), "Path should not exist")
    }
  }

  test("JsonQueryTool should validate type as object") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("metadata"),
      expectedType = Some("object")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Should be an object")
    }
  }

  test("JsonQueryTool should validate type as array") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("users"),
      expectedType = Some("array")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Should be an array")
    }
  }

  test("JsonQueryTool should validate type as string") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("users.0.name"),
      expectedType = Some("string")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Should be a string")
    }
  }

  test("JsonQueryTool should validate type as number") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("users.0.age"),
      expectedType = Some("number")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Should be a number")
    }
  }

  test("JsonQueryTool should fail validation with wrong type") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("users"),
      expectedType = Some("object")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(content.contains("false"), "Should not be an object")
      assert(
        content.contains("Expected 'object' but got 'array'"),
        "Should show type mismatch"
      )
    }
  }

  test("JsonQueryTool should return type when no expected type given") {
    val tool   = JsonQueryTool(
      sampleJson,
      "validate",
      path = Some("users")
    )
    val result = tool.execute()

    assert(result.isSuccess, "Validate operation should succeed")
    result.foreach { content =>
      assert(
        content.contains("Value is of type 'array'"),
        "Should report actual type"
      )
    }
  }

  test("JsonQueryTool should fail with invalid JSON") {
    val tool   = JsonQueryTool("{invalid json}", "get")
    val result = tool.execute()

    assert(result.isFailure, "Should fail with invalid JSON")
  }

  test("JsonQueryTool should fail with unknown operation") {
    val tool   = JsonQueryTool(sampleJson, "unknown")
    val result = tool.execute()

    assert(result.isFailure, "Should fail with unknown operation")
  }

  test("JsonQueryTool should fail map operation without field") {
    val tool   = JsonQueryTool(sampleJson, "map", path = Some("users"))
    val result = tool.execute()

    assert(result.isFailure, "Should fail map without field parameter")
  }

  test("JsonQueryTool should fail filter operation without field") {
    val tool   = JsonQueryTool(sampleJson, "filter", path = Some("users"))
    val result = tool.execute()

    assert(result.isFailure, "Should fail filter without field parameter")
  }

  test("JsonQueryTool should fail exists operation without path") {
    val tool   = JsonQueryTool(sampleJson, "exists")
    val result = tool.execute()

    assert(result.isFailure, "Should fail exists without path parameter")
  }

  test("JsonQueryTool should handle deeply nested paths") {
    val nestedJson = """{"level1": {"level2": {"level3": {"value": 42}}}}"""
    val tool       = JsonQueryTool(
      nestedJson,
      "get",
      path = Some("level1.level2.level3.value")
    )
    val result     = tool.execute()

    assert(result.isSuccess, "Deep path should succeed")
    result.foreach { content =>
      assertEquals(content, "42", "Should extract deeply nested value")
    }
  }

  test("JsonQueryTool should handle empty arrays") {
    val emptyArrayJson = """{"items": []}"""
    val tool           = JsonQueryTool(
      emptyArrayJson,
      "map",
      path = Some("items"),
      field = Some("name")
    )
    val result         = tool.execute()

    assert(result.isSuccess, "Empty array map should succeed")
    result.foreach { content =>
      assert(
        content.contains("Extracted 0 values"),
        "Should extract 0 values from empty array"
      )
    }
  }

  test("JsonQueryTool should register with ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[JsonQueryTool]

    val tools = registry.getRegisteredToolNames
    assert(
      tools.contains(
        "dev.alteration.branch.hollywood.tools.provided.json.JsonQueryTool"
      ),
      s"Registry should contain JsonQueryTool. Got: $tools"
    )
  }

  test("JsonQueryTool should execute via ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[JsonQueryTool]

    val args = JsonObject(
      Map(
        "json"      -> JsonString(sampleJson),
        "operation" -> JsonString("get"),
        "path"      -> JsonString("metadata.total")
      )
    )

    val result = registry.execute(
      "dev.alteration.branch.hollywood.tools.provided.json.JsonQueryTool",
      args
    )

    assert(result.isDefined, "Execution should return a result")
    result.foreach {
      case JsonString(value) =>
        assertEquals(value, "3", "Should extract total value")
      case _                 => fail("Expected JsonString result")
    }
  }

  test("JsonQueryTool should handle boolean values") {
    val boolJson = """{"active": true, "verified": false}"""
    val tool     = JsonQueryTool(boolJson, "get", path = Some("active"))
    val result   = tool.execute()

    assert(result.isSuccess, "Boolean get should succeed")
    result.foreach { content =>
      assertEquals(content, "true", "Should extract boolean value")
    }
  }

  test("JsonQueryTool should handle null values") {
    val nullJson = """{"value": null}"""
    val tool     = JsonQueryTool(nullJson, "get", path = Some("value"))
    val result   = tool.execute()

    assert(result.isSuccess, "Null get should succeed")
    result.foreach { content =>
      assertEquals(content, "null", "Should extract null value")
    }
  }

  test("JsonQueryTool should validate null type") {
    val nullJson = """{"value": null}"""
    val tool     = JsonQueryTool(
      nullJson,
      "validate",
      path = Some("value"),
      expectedType = Some("null")
    )
    val result   = tool.execute()

    assert(result.isSuccess, "Null validation should succeed")
    result.foreach { content =>
      assert(content.contains("true"), "Should be null type")
    }
  }
}
