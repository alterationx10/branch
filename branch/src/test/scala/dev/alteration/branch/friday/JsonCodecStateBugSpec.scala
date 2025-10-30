package dev.alteration.branch.friday

import munit.FunSuite

/** Test suite to reproduce the state-sharing/caching bug in JsonCodec
  *
  * The bug occurs when using `derives JsonCodec` on case classes - subsequent
  * decode calls may return values from previous decode operations due to
  * singleton decoder instances caching state.
  */
class JsonCodecStateBugSpec extends FunSuite {

  // Nested case classes to test deep decoder instances
  case class ChatMessage(role: String, content: String) derives JsonCodec
  case class Choice(index: Int, message: ChatMessage, finishReason: String)
      derives JsonCodec
  case class ChatCompletionsResponse(id: String, choices: List[Choice])
      derives JsonCodec

  test("derived codec should not cache values between decode calls") {
    val json1 = """{
      "id": "chatcmpl-first",
      "choices": [
        {
          "index": 0,
          "message": {"role": "assistant", "content": "First response"},
          "finishReason": "stop"
        }
      ]
    }"""

    val json2 = """{
      "id": "chatcmpl-second",
      "choices": [
        {
          "index": 1,
          "message": {"role": "user", "content": "Second response"},
          "finishReason": "length"
        }
      ]
    }"""

    // First decode using the derived codec from the companion object
    val decoded1 = ChatCompletionsResponse.derived$JsonCodec.decode(json1).get
    assertEquals(decoded1.id, "chatcmpl-first")
    assertEquals(decoded1.choices.head.index, 0)
    assertEquals(decoded1.choices.head.message.role, "assistant")
    assertEquals(decoded1.choices.head.message.content, "First response")
    assertEquals(decoded1.choices.head.finishReason, "stop")

    // Second decode using the same derived codec - this is where the bug manifests
    val decoded2 = ChatCompletionsResponse.derived$JsonCodec.decode(json2).get

    // BUG: If state is cached, decoded2 might contain values from decoded1
    assertEquals(
      decoded2.id,
      "chatcmpl-second",
      "ID should be from second JSON, not cached from first"
    )
    assertEquals(
      decoded2.choices.head.index,
      1,
      "Index should be 1, not cached 0"
    )
    assertEquals(
      decoded2.choices.head.message.role,
      "user",
      "Role should be 'user', not cached 'assistant'"
    )
    assertEquals(
      decoded2.choices.head.message.content,
      "Second response",
      "Content should be from second JSON"
    )
    assertEquals(
      decoded2.choices.head.finishReason,
      "length",
      "Finish reason should be 'length', not cached 'stop'"
    )
  }

  test("fresh codec instances should always work correctly") {
    val json1 = """{
      "id": "chatcmpl-first",
      "choices": [
        {
          "index": 0,
          "message": {"role": "assistant", "content": "First response"},
          "finishReason": "stop"
        }
      ]
    }"""

    val json2 = """{
      "id": "chatcmpl-second",
      "choices": [
        {
          "index": 1,
          "message": {"role": "user", "content": "Second response"},
          "finishReason": "length"
        }
      ]
    }"""

    // Create fresh codec instances for each decode (the workaround)
    import dev.alteration.branch.friday.JsonCodec
    val decoded1 = JsonCodec.derived[ChatCompletionsResponse].decode(json1).get
    assertEquals(decoded1.id, "chatcmpl-first")
    assertEquals(decoded1.choices.head.index, 0)

    val decoded2 = JsonCodec.derived[ChatCompletionsResponse].decode(json2).get
    assertEquals(decoded2.id, "chatcmpl-second")
    assertEquals(decoded2.choices.head.index, 1)
  }

  test("multiple rapid decodes with derived codec should not share state") {
    // Simple case class to test rapid decoding
    case class SimpleData(value: String, count: Int) derives JsonCodec

    val jsons = List(
      """{"value": "first", "count": 1}""",
      """{"value": "second", "count": 2}""",
      """{"value": "third", "count": 3}""",
      """{"value": "fourth", "count": 4}""",
      """{"value": "fifth", "count": 5}"""
    )

    val expected = List(
      ("first", 1),
      ("second", 2),
      ("third", 3),
      ("fourth", 4),
      ("fifth", 5)
    )

    // Decode all using the derived codec
    val results =
      jsons.map(json => SimpleData.derived$JsonCodec.decode(json).get)

    // Check each result matches its expected value (no cross-contamination)
    results.zip(expected).zipWithIndex.foreach {
      case ((result, (expValue, expCount)), idx) =>
        assertEquals(result.value, expValue, s"Result $idx value mismatch")
        assertEquals(result.count, expCount, s"Result $idx count mismatch")
    }
  }

  test("nested collections should not cache between decodes") {
    case class Item(name: String, quantity: Int) derives JsonCodec
    case class Order(orderId: String, items: List[Item]) derives JsonCodec

    val json1 = """{
      "orderId": "order-1",
      "items": [
        {"name": "apple", "quantity": 5},
        {"name": "banana", "quantity": 3}
      ]
    }"""

    val json2 = """{
      "orderId": "order-2",
      "items": [
        {"name": "orange", "quantity": 7}
      ]
    }"""

    val decoded1 = Order.derived$JsonCodec.decode(json1).get
    assertEquals(decoded1.orderId, "order-1")
    assertEquals(decoded1.items.length, 2)
    assertEquals(decoded1.items(0).name, "apple")
    assertEquals(decoded1.items(1).name, "banana")

    val decoded2 = Order.derived$JsonCodec.decode(json2).get
    assertEquals(decoded2.orderId, "order-2")
    assertEquals(
      decoded2.items.length,
      1,
      "Should have 1 item, not cached 2 from previous decode"
    )
    assertEquals(
      decoded2.items(0).name,
      "orange",
      "Item name should be 'orange', not cached from previous"
    )
    assertEquals(decoded2.items(0).quantity, 7, "Quantity should be 7")
  }
}
