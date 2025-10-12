package dev.alteration.branch.spider.webview.html

import munit.FunSuite

/** Tests for the Attr ADT.
  *
  * Tests attribute rendering and escaping.
  */
class AttrSuite extends FunSuite {

  test("StringAttr renders with key and value") {
    val attr = Attr.StringAttr("id", "main")
    assertEquals(attr.render, """id="main"""")
  }

  test("StringAttr escapes HTML special characters") {
    val attr = Attr.StringAttr("title", "<script>alert('xss')</script>")
    assertEquals(
      attr.render,
      """title="&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;""""
    )
  }

  test("StringAttr escapes ampersands") {
    val attr = Attr.StringAttr("data", "foo & bar")
    assertEquals(attr.render, """data="foo &amp; bar"""")
  }

  test("StringAttr escapes double quotes") {
    val attr = Attr.StringAttr("title", """He said "hello"""")
    assertEquals(attr.render, """title="He said &quot;hello&quot;"""")
  }

  test("StringAttr escapes single quotes") {
    val attr = Attr.StringAttr("title", "It's working")
    assertEquals(attr.render, """title="It&#39;s working"""")
  }

  test("StringAttr handles empty value") {
    val attr = Attr.StringAttr("placeholder", "")
    assertEquals(attr.render, """placeholder=""""")
  }

  test("BooleanAttr renders key when true") {
    val attr = Attr.BooleanAttr("disabled", true)
    assertEquals(attr.render, "disabled")
  }

  test("BooleanAttr renders empty string when false") {
    val attr = Attr.BooleanAttr("disabled", false)
    assertEquals(attr.render, "")
  }

  test("BooleanAttr for checked") {
    val checked   = Attr.BooleanAttr("checked", true)
    val unchecked = Attr.BooleanAttr("checked", false)

    assertEquals(checked.render, "checked")
    assertEquals(unchecked.render, "")
  }

  test("BooleanAttr for readonly") {
    val readonly    = Attr.BooleanAttr("readonly", true)
    val notReadonly = Attr.BooleanAttr("readonly", false)

    assertEquals(readonly.render, "readonly")
    assertEquals(notReadonly.render, "")
  }

  test("EmptyAttr renders empty string") {
    assertEquals(Attr.EmptyAttr.render, "")
  }

  test("Multiple attributes render correctly in element") {
    val attrs = List(
      Attr.StringAttr("id", "test"),
      Attr.StringAttr("class", "btn btn-primary"),
      Attr.BooleanAttr("disabled", true),
      Attr.StringAttr("data-value", "123")
    )

    val rendered = attrs.map(_.render).mkString(" ")
    assertEquals(
      rendered,
      """id="test" class="btn btn-primary" disabled data-value="123""""
    )
  }

  test("Attributes with special characters are properly escaped") {
    val attr = Attr.StringAttr("onclick", """alert("Click & drag")""")
    assertEquals(
      attr.render,
      """onclick="alert(&quot;Click &amp; drag&quot;)""""
    )
  }
}
