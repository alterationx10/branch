package dev.alteration.branch.spider.webview.html

import munit.FunSuite

/** Tests for the core Html ADT.
  *
  * Tests rendering, escaping, and composition of Html elements.
  */
class HtmlSuite extends FunSuite {

  test("Element renders with tag name") {
    val elem = Html.Element("div", Nil, Nil)
    assertEquals(elem.render, "<div></div>")
  }

  test("Element renders with attributes") {
    val elem = Html.Element(
      "div",
      List(Attr.StringAttr("id", "main")),
      Nil
    )
    assertEquals(elem.render, """<div id="main"></div>""")
  }

  test("Element renders with multiple attributes") {
    val elem = Html.Element(
      "div",
      List(
        Attr.StringAttr("id", "main"),
        Attr.StringAttr("class", "container")
      ),
      Nil
    )
    assertEquals(elem.render, """<div id="main" class="container"></div>""")
  }

  test("Element renders with children") {
    val elem = Html.Element(
      "div",
      Nil,
      List(Html.Text("Hello"))
    )
    assertEquals(elem.render, "<div>Hello</div>")
  }

  test("Element renders with nested children") {
    val elem = Html.Element(
      "div",
      Nil,
      List(
        Html.Element("span", Nil, List(Html.Text("Hello"))),
        Html.Element("span", Nil, List(Html.Text("World")))
      )
    )
    assertEquals(elem.render, "<div><span>Hello</span><span>World</span></div>")
  }

  test("Self-closing tags render correctly without children") {
    val br = Html.Element("br", Nil, Nil)
    assertEquals(br.render, "<br />")

    val hr = Html.Element("hr", Nil, Nil)
    assertEquals(hr.render, "<hr />")

    val img = Html.Element("img", List(Attr.StringAttr("src", "test.png")), Nil)
    assertEquals(img.render, """<img src="test.png" />""")
  }

  test("Self-closing tags render with closing tag if children provided") {
    val div = Html.Element("div", Nil, List(Html.Text("content")))
    assertEquals(div.render, "<div>content</div>")
  }

  test("Text escapes HTML special characters") {
    val text = Html.Text("<script>alert('xss')</script>")
    assertEquals(
      text.render,
      "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
    )
  }

  test("Text escapes ampersands") {
    val text = Html.Text("foo & bar")
    assertEquals(text.render, "foo &amp; bar")
  }

  test("Text escapes quotes") {
    val text = Html.Text("""He said "hello"""")
    assertEquals(text.render, "He said &quot;hello&quot;")
  }

  test("Text escapes single quotes") {
    val text = Html.Text("It's working")
    assertEquals(text.render, "It&#39;s working")
  }

  test("Raw HTML is not escaped") {
    val raw = Html.Raw("<strong>Bold</strong>")
    assertEquals(raw.render, "<strong>Bold</strong>")
  }

  test("Raw HTML allows dangerous content (use with caution)") {
    val raw = Html.Raw("<script>alert('xss')</script>")
    assertEquals(raw.render, "<script>alert('xss')</script>")
  }

  test("Empty renders to empty string") {
    assertEquals(Html.Empty.render, "")
  }

  test("Fragment renders multiple children without wrapper") {
    val fragment = Html.Fragment(
      List(
        Html.Element("p", Nil, List(Html.Text("First"))),
        Html.Element("p", Nil, List(Html.Text("Second")))
      )
    )
    assertEquals(fragment.render, "<p>First</p><p>Second</p>")
  }

  test("Fragment with empty list renders to empty string") {
    val fragment = Html.Fragment(Nil)
    assertEquals(fragment.render, "")
  }

  test("Complex nested structure renders correctly") {
    val html = Html.Element(
      "div",
      List(Attr.StringAttr("class", "container")),
      List(
        Html.Element(
          "h1",
          Nil,
          List(Html.Text("Title"))
        ),
        Html.Element(
          "p",
          Nil,
          List(
            Html.Text("This is "),
            Html.Element("strong", Nil, List(Html.Text("important"))),
            Html.Text(" text.")
          )
        ),
        Html.Element("br", Nil, Nil),
        Html.Raw("<span>Raw HTML</span>")
      )
    )

    val expected =
      """<div class="container"><h1>Title</h1><p>This is <strong>important</strong> text.</p><br /><span>Raw HTML</span></div>"""
    assertEquals(html.render, expected)
  }

  test("Empty attribute list renders element without attributes") {
    val elem = Html.Element("div", List(Attr.EmptyAttr), Nil)
    assertEquals(elem.render, "<div></div>")
  }

  test("Mixed empty and non-empty attributes render correctly") {
    val elem = Html.Element(
      "div",
      List(
        Attr.StringAttr("id", "test"),
        Attr.EmptyAttr,
        Attr.StringAttr("class", "foo")
      ),
      Nil
    )
    assertEquals(elem.render, """<div id="test"  class="foo"></div>""")
  }
}
