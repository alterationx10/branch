package dev.alteration.branch.spider.webview.html

import munit.FunSuite
import WebViewAttributes._

/** Tests for WebView-specific HTML attributes.
  *
  * Tests the WebView event attribute builders and convenience helpers.
  */
class WebViewAttributesSuite extends FunSuite {

  test("wvClick creates click attribute") {
    val attr = wvClick := "increment"
    assertEquals(attr.render, """wv-click="increment"""")
  }

  test("wvChange creates change attribute") {
    val attr = wvChange := "update-name"
    assertEquals(attr.render, """wv-change="update-name"""")
  }

  test("wvSubmit creates submit attribute") {
    val attr = wvSubmit := "save-form"
    assertEquals(attr.render, """wv-submit="save-form"""")
  }

  test("wvInput creates input attribute") {
    val attr = wvInput := "search"
    assertEquals(attr.render, """wv-input="search"""")
  }

  test("wvFocus creates focus attribute") {
    val attr = wvFocus := "field-focused"
    assertEquals(attr.render, """wv-focus="field-focused"""")
  }

  test("wvBlur creates blur attribute") {
    val attr = wvBlur := "field-blurred"
    assertEquals(attr.render, """wv-blur="field-blurred"""")
  }

  test("wvKeydown creates keydown attribute") {
    val attr = wvKeydown := "handle-key"
    assertEquals(attr.render, """wv-keydown="handle-key"""")
  }

  test("wvKeyup creates keyup attribute") {
    val attr = wvKeyup := "key-released"
    assertEquals(attr.render, """wv-keyup="key-released"""")
  }

  test("wvMouseenter creates mouseenter attribute") {
    val attr = wvMouseenter := "show-tooltip"
    assertEquals(attr.render, """wv-mouseenter="show-tooltip"""")
  }

  test("wvMouseleave creates mouseleave attribute") {
    val attr = wvMouseleave := "hide-tooltip"
    assertEquals(attr.render, """wv-mouseleave="hide-tooltip"""")
  }

  test("wvDebounce creates debounce attribute") {
    val attr = wvDebounce := "300"
    assertEquals(attr.render, """wv-debounce="300"""")
  }

  test("wvThrottle creates throttle attribute") {
    val attr = wvThrottle := "1000"
    assertEquals(attr.render, """wv-throttle="1000"""")
  }

  test("wvValue creates value attribute") {
    val attr = wvValue := "item-id-123"
    assertEquals(attr.render, """wv-value="item-id-123"""")
  }

  test("wvTarget creates target attribute") {
    val attr = wvTarget := "element-id"
    assertEquals(attr.render, """wv-target="element-id"""")
  }

  test("wvIgnore creates ignore attribute") {
    val attr  = wvIgnore := true
    assertEquals(attr.render, """wv-ignore""")
    val attr2 = wvIgnore := "true"
    assertEquals(attr2.render, """wv-ignore="true"""")
  }

  test("wvAttr creates custom WebView attribute") {
    val attr = wvAttr("custom") := "event-name"
    assertEquals(attr.render, """wv-custom="event-name"""")
  }

  test("wvClickTarget helper creates click and target attributes") {
    val attrs = wvClickTarget("delete-item", "item-123")
    assertEquals(attrs.length, 2)

    val rendered = attrs.map(_.render).mkString(" ")
    assert(rendered.contains("""wv-click="delete-item""""))
    assert(rendered.contains("""wv-target="item-123""""))
  }

  test("wvClickValue helper creates click and value attributes") {
    val attrs = wvClickValue("set-filter", "active")
    assertEquals(attrs.length, 2)

    val rendered = attrs.map(_.render).mkString(" ")
    assert(rendered.contains("""wv-click="set-filter""""))
    assert(rendered.contains("""wv-value="active""""))
  }

  test("wvDebounceInput helper creates input and debounce attributes") {
    val attrs = wvDebounceInput("search", 300)
    assertEquals(attrs.length, 2)

    val rendered = attrs.map(_.render).mkString(" ")
    assert(rendered.contains("""wv-input="search""""))
    assert(rendered.contains("""wv-debounce="300""""))
  }

  test("wvThrottleClick helper creates click and throttle attributes") {
    val attrs = wvThrottleClick("track-hover", 1000)
    assertEquals(attrs.length, 2)

    val rendered = attrs.map(_.render).mkString(" ")
    assert(rendered.contains("""wv-click="track-hover""""))
    assert(rendered.contains("""wv-throttle="1000""""))
  }

  test("typed event - case object") {
    case object Increment
    val attr = wvClick := Increment
    assertEquals(attr.render, """wv-click="Increment"""")
  }

  test("typed event - case class") {
    case class SetCount(n: Int)
    val attr = wvClick := SetCount(42)
    assertEquals(attr.render, """wv-click="SetCount"""")
  }

  test("WebView attributes escape special characters") {
    val attr = wvClick := """<script>alert("xss")</script>"""
    val html = attr.render

    assert(!html.contains("<script>"))
    assert(html.contains("&lt;script&gt;"))
  }

  test("Multiple WebView attributes on element") {

    val button = Tags.button(
      wvClick             := "submit",
      wvTarget            := "form-1",
      Attributes.cls      := "btn",
      Attributes.disabled := false
    )(Html.Text("Submit"))

    val html = button.render
    assert(html.contains("""wv-click="submit""""))
    assert(html.contains("""wv-target="form-1""""))
    assert(html.contains("""class="btn""""))
  }

  test("Debounced search input pattern") {

    val searchInput = Tags.input(
      Attributes.tpe         := "text",
      Attributes.placeholder := "Search...",
      wvInput                := "search",
      wvDebounce             := "500"
    )

    val html = searchInput.render
    assert(html.contains("""wv-input="search""""))
    assert(html.contains("""wv-debounce="500""""))
  }

  test("Button with target value pattern") {

    val deleteBtn = Tags.button(
      wvClick        := "delete-todo",
      wvTarget       := "todo-42",
      Attributes.cls := "btn-danger"
    )(Html.Text("Delete"))

    val html = deleteBtn.render
    assert(html.contains("""wv-click="delete-todo""""))
    assert(html.contains("""wv-target="todo-42""""))
  }
}
