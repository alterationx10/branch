package dev.alteration.branch.spider.webview

import dev.alteration.branch.friday.Json
import munit.FunSuite

/** Tests for WebViewProtocol.
  *
  * Tests client/server message parsing and serialization.
  */
class WebViewProtocolSuite extends FunSuite {

  import WebViewProtocol._

  // === Client Messages ===

  test("parse ClientReady message") {
    val json   = Json.obj("type" -> Json.JsonString("ready"))
    val result = parseClientMessage(json)

    assertEquals(result, Some(ClientReady))
  }

  test("parse Event message with all fields") {
    val json   = Json.obj(
      "type"   -> Json.JsonString("event"),
      "event"  -> Json.JsonString("increment"),
      "target" -> Json.JsonString("button-1"),
      "value"  -> Json.JsonString("42")
    )
    val result = parseClientMessage(json)

    assertEquals(
      result,
      Some(Event("increment", "button-1", Some(Json.JsonString("42"))))
    )
  }

  test("parse Event message without value") {
    val json   = Json.obj(
      "type"   -> Json.JsonString("event"),
      "event"  -> Json.JsonString("click"),
      "target" -> Json.JsonString("btn")
    )
    val result = parseClientMessage(json)

    assert(result.isDefined)
    result match {
      case Some(Event(event, target, value)) =>
        assertEquals(event, "click")
        assertEquals(target, "btn")
        // value will be present but might be JsonNull or similar
      case _                                 => fail("Expected Event message")
    }
  }

  test("parse Event message returns None on missing event field") {
    val json   = Json.obj(
      "type"   -> Json.JsonString("event"),
      "target" -> Json.JsonString("btn")
    )
    val result = parseClientMessage(json)

    assertEquals(result, None)
  }

  test("parse Event message returns None on missing target field") {
    val json   = Json.obj(
      "type"  -> Json.JsonString("event"),
      "event" -> Json.JsonString("click")
    )
    val result = parseClientMessage(json)

    assertEquals(result, None)
  }

  test("parse Heartbeat message (ping)") {
    val json   = Json.obj("type" -> Json.JsonString("ping"))
    val result = parseClientMessage(json)

    assertEquals(result, Some(Heartbeat))
  }

  test("parse Heartbeat message (heartbeat)") {
    val json   = Json.obj("type" -> Json.JsonString("heartbeat"))
    val result = parseClientMessage(json)

    assertEquals(result, Some(Heartbeat))
  }

  test("parse unknown message type returns None") {
    val json   = Json.obj("type" -> Json.JsonString("unknown"))
    val result = parseClientMessage(json)

    assertEquals(result, None)
  }

  test("parse message with missing type field returns None") {
    val json   = Json.obj("data" -> Json.JsonString("value"))
    val result = parseClientMessage(json)

    assertEquals(result, None)
  }

  test("parseClientMessage from string") {
    val jsonStr = """{"type":"ready"}"""
    val result  = parseClientMessage(jsonStr)

    assertEquals(result, Some(ClientReady))
  }

  test("parseClientMessage from invalid JSON string returns None") {
    val jsonStr = "{invalid json"
    val result  = parseClientMessage(jsonStr)

    assertEquals(result, None)
  }

  // === Server Messages ===

  test("ReplaceHtml toJson with default target") {
    val msg  = ReplaceHtml("<div>Content</div>")
    val json = msg.toJson

    assertEquals((json ? "type").strOpt, Some("replace"))
    assertEquals((json ? "html").strOpt, Some("<div>Content</div>"))
    assertEquals((json ? "target").strOpt, Some("root"))
  }

  test("ReplaceHtml toJson with custom target") {
    val msg  = ReplaceHtml("<span>Text</span>", target = "sidebar")
    val json = msg.toJson

    assertEquals((json ? "type").strOpt, Some("replace"))
    assertEquals((json ? "html").strOpt, Some("<span>Text</span>"))
    assertEquals((json ? "target").strOpt, Some("sidebar"))
  }

  test("PatchHtml toJson") {
    val msg  = PatchHtml("<p>Updated</p>", "content")
    val json = msg.toJson

    assertEquals((json ? "type").strOpt, Some("patch"))
    assertEquals((json ? "html").strOpt, Some("<p>Updated</p>"))
    assertEquals((json ? "target").strOpt, Some("content"))
  }

  test("HeartbeatResponse toJson") {
    val json = HeartbeatResponse.toJson

    assertEquals((json ? "type").strOpt, Some("pong"))
  }

  test("Error toJson") {
    val msg  = Error("Something went wrong")
    val json = msg.toJson

    assertEquals((json ? "type").strOpt, Some("error"))
    assertEquals((json ? "message").strOpt, Some("Something went wrong"))
  }

  test("ReplaceHtml with HTML special characters") {
    val msg  = ReplaceHtml("""<div class="test">Content & "quotes"</div>""")
    val json = msg.toJson

    val htmlField = (json ? "html").strOpt
    assert(htmlField.isDefined)
    assert(htmlField.get.contains("&"))
    assert(htmlField.get.contains("\""))
  }

  test("Error message with special characters") {
    val msg  = Error("""Error: <script>alert("xss")</script>""")
    val json = msg.toJson

    val messageField = (json ? "message").strOpt
    assert(messageField.isDefined)
    // The message should be preserved as-is in JSON
    assert(messageField.get.contains("<script>"))
  }

  // === Round-trip Tests ===

  test("ClientReady round-trip") {
    val original = ClientReady
    val json     = Json.obj("type" -> Json.JsonString("ready"))
    val parsed   = parseClientMessage(json)

    assertEquals(parsed, Some(original))
  }

  test("Event round-trip") {
    val original = Event("click", "button-1", Some(Json.JsonString("data")))
    val json     = Json.obj(
      "type"   -> Json.JsonString("event"),
      "event"  -> Json.JsonString("click"),
      "target" -> Json.JsonString("button-1"),
      "value"  -> Json.JsonString("data")
    )
    val parsed   = parseClientMessage(json)

    assertEquals(parsed, Some(original))
  }

  test("Heartbeat round-trip") {
    val original = Heartbeat
    val json     = Json.obj("type" -> Json.JsonString("ping"))
    val parsed   = parseClientMessage(json)

    assertEquals(parsed, Some(original))
  }

  // === Edge Cases ===

  test("Event with complex JSON value") {
    val json   = Json.obj(
      "type"   -> Json.JsonString("event"),
      "event"  -> Json.JsonString("submit"),
      "target" -> Json.JsonString("form"),
      "value"  -> Json.obj(
        "username" -> Json.JsonString("alice"),
        "password" -> Json.JsonString("secret")
      )
    )
    val result = parseClientMessage(json)

    assert(result.isDefined)
    result match {
      case Some(Event(event, target, Some(value: Json.JsonObject))) =>
        assertEquals(event, "submit")
        assertEquals(target, "form")
        assertEquals((value ? "username").strOpt, Some("alice"))
      case _                                                        =>
        fail("Expected Event with JsonObject value")
    }
  }

  test("ReplaceHtml with empty HTML") {
    val msg  = ReplaceHtml("")
    val json = msg.toJson

    assertEquals((json ? "html").strOpt, Some(""))
  }

  test("PatchHtml with empty target throws or handles gracefully") {
    val msg  = PatchHtml("<p>Content</p>", "")
    val json = msg.toJson

    assertEquals((json ? "target").strOpt, Some(""))
  }
}
