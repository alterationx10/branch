package dev.alteration.branch.spider.webview.html

import munit.FunSuite
import Attributes._

/** Tests for the Attributes helper functions.
  *
  * Tests attribute builder DSL and conditional attribute helpers.
  */
class AttributesSuite extends FunSuite {

  test("AttrBuilder := operator creates StringAttr") {
    val attr = cls := "container"
    assertEquals(attr.render, """class="container"""")
  }

  test("AttrBuilder := operator with boolean") {
    val attr = disabled := true
    assertEquals(attr.render, "disabled")
  }

  test("AttrBuilder := operator with Any type") {
    val attr = data("count") := 42
    assertEquals(attr.render, """data-count="42"""")
  }

  test("cls attribute builder") {
    val attr = cls := "btn btn-primary"
    assertEquals(attr.render, """class="btn btn-primary"""")
  }

  test("id attribute builder") {
    val attr = id := "main-content"
    assertEquals(attr.render, """id="main-content"""")
  }

  test("style attribute builder") {
    val attr = style := "color: red; font-size: 16px"
    assertEquals(attr.render, """style="color: red; font-size: 16px"""")
  }

  test("title attribute builder") {
    val attr = title := "Hover text"
    assertEquals(attr.render, """title="Hover text"""")
  }

  test("name attribute builder") {
    val attr = name := "username"
    assertEquals(attr.render, """name="username"""")
  }

  test("value attribute builder") {
    val attr = value := "default value"
    assertEquals(attr.render, """value="default value"""")
  }

  test("placeholder attribute builder") {
    val attr = placeholder := "Enter text..."
    assertEquals(attr.render, """placeholder="Enter text..."""")
  }

  test("tpe attribute builder (for type)") {
    val attr = tpe := "text"
    assertEquals(attr.render, """type="text"""")
  }

  test("href attribute builder") {
    val attr = href := "https://example.com"
    assertEquals(attr.render, """href="https://example.com"""")
  }

  test("src attribute builder") {
    val attr = src := "/images/logo.png"
    assertEquals(attr.render, """src="/images/logo.png"""")
  }

  test("alt attribute builder") {
    val attr = alt := "Logo image"
    assertEquals(attr.render, """alt="Logo image"""")
  }

  test("width and height attribute builders") {
    val w = width  := "800"
    val h = height := "600"
    assertEquals(w.render, """width="800"""")
    assertEquals(h.render, """height="600"""")
  }

  test("target attribute builder") {
    val attr = target := "_blank"
    assertEquals(attr.render, """target="_blank"""")
  }

  test("rel attribute builder") {
    val attr = rel := "noopener noreferrer"
    assertEquals(attr.render, """rel="noopener noreferrer"""")
  }

  test("action and method attribute builders") {
    val a = action := "/submit"
    val m = method := "POST"
    assertEquals(a.render, """action="/submit"""")
    assertEquals(m.render, """method="POST"""")
  }

  test("disabled boolean attribute") {
    val en  = disabled := false
    val dis = disabled := true
    assertEquals(en.render, "")
    assertEquals(dis.render, "disabled")
  }

  test("readonly boolean attribute") {
    val editable = readonly := false
    val readOnly = readonly := true
    assertEquals(editable.render, "")
    assertEquals(readOnly.render, "readonly")
  }

  test("required boolean attribute") {
    val optional = required := false
    val req      = required := true
    assertEquals(optional.render, "")
    assertEquals(req.render, "required")
  }

  test("checked boolean attribute") {
    val unchecked = checked := false
    val check     = checked := true
    assertEquals(unchecked.render, "")
    assertEquals(check.render, "checked")
  }

  test("selected boolean attribute") {
    val unselected = selected := false
    val select     = selected := true
    assertEquals(unselected.render, "")
    assertEquals(select.render, "selected")
  }

  test("autofocus boolean attribute") {
    val attr = autofocus := true
    assertEquals(attr.render, "autofocus")
  }

  test("controls boolean attribute (media)") {
    val attr = controls := true
    assertEquals(attr.render, "controls")
  }

  test("multiple boolean attribute") {
    val attr = multiple := true
    assertEquals(attr.render, "multiple")
  }

  test("ARIA attributes") {
    val label       = ariaLabel       := "Close button"
    val describedBy = ariaDescribedBy := "help-text"
    val hidden      = ariaHidden      := "true"
    val roleAttr    = role            := "button"

    assertEquals(label.render, """aria-label="Close button"""")
    assertEquals(describedBy.render, """aria-describedby="help-text"""")
    assertEquals(hidden.render, """aria-hidden="true"""")
    assertEquals(roleAttr.render, """role="button"""")
  }

  test("data attribute helper") {
    val attr = data("user-id") := "123"
    assertEquals(attr.render, """data-user-id="123"""")
  }

  test("data attribute with complex name") {
    val attr = data("item-count") := "42"
    assertEquals(attr.render, """data-item-count="42"""")
  }

  test("attr custom attribute helper") {
    val custom = attr("x-custom") := "value"
    assertEquals(custom.render, """x-custom="value"""")
  }

  test("attrWhen - condition true") {
    val attr = attrWhen(true, cls := "active")
    assertEquals(attr.render, """class="active"""")
  }

  test("attrWhen - condition false") {
    val attr = attrWhen(false, cls := "active")
    assertEquals(attr.render, "")
  }

  test("attrCond - condition true") {
    val attr = attrCond(true, cls := "enabled", cls := "disabled")
    assertEquals(attr.render, """class="enabled"""")
  }

  test("attrCond - condition false") {
    val attr = attrCond(false, cls := "enabled", cls := "disabled")
    assertEquals(attr.render, """class="disabled"""")
  }

  test("classes helper combines multiple class names") {
    val attr = classes("btn", "btn-primary", "btn-lg")
    assertEquals(attr.render, """class="btn btn-primary btn-lg"""")
  }

  test("classes helper filters empty strings") {
    val attr = classes("btn", "", "btn-primary", "")
    assertEquals(attr.render, """class="btn btn-primary"""")
  }

  test("classWhen helper - all true") {
    val attr = classWhen(
      "active"   -> true,
      "disabled" -> true,
      "loading"  -> true
    )
    assertEquals(attr.render, """class="active disabled loading"""")
  }

  test("classWhen helper - mixed conditions") {
    val attr = classWhen(
      "active"   -> true,
      "disabled" -> false,
      "loading"  -> true
    )
    assertEquals(attr.render, """class="active loading"""")
  }

  test("classWhen helper - all false") {
    val attr = classWhen(
      "active"   -> false,
      "disabled" -> false
    )
    assertEquals(attr.render, "")
  }

  test("styles helper combines multiple style properties") {
    val attr = styles(
      "color"      -> "red",
      "font-size"  -> "16px",
      "margin-top" -> "10px"
    )
    assertEquals(
      attr.render,
      """style="color: red; font-size: 16px; margin-top: 10px""""
    )
  }

  test("styles helper with single property") {
    val attr = styles("display" -> "none")
    assertEquals(attr.render, """style="display: none"""")
  }

  test("styleWhen helper - all true") {
    val attr = styleWhen(
      ("color", "red", true),
      ("display", "none", true)
    )
    assertEquals(attr.render, """style="color: red; display: none"""")
  }

  test("styleWhen helper - mixed conditions") {
    val attr = styleWhen(
      ("color", "red", true),
      ("display", "none", false),
      ("font-weight", "bold", true)
    )
    assertEquals(attr.render, """style="color: red; font-weight: bold"""")
  }

  test("styleWhen helper - all false") {
    val attr = styleWhen(
      ("color", "red", false),
      ("display", "none", false)
    )
    assertEquals(attr.render, "")
  }

  test("combining multiple attribute builders") {
    val attrs = List(
      id             := "submit-btn",
      cls            := "btn btn-primary",
      tpe            := "submit",
      disabled       := false,
      data("action") := "submit-form"
    )

    val rendered = attrs.map(_.render).filter(_.nonEmpty).mkString(" ")
    assertEquals(
      rendered,
      """id="submit-btn" class="btn btn-primary" type="submit" data-action="submit-form""""
    )
  }

  test("attribute values are escaped") {
    val attr = title := """<script>alert("xss")</script>"""
    assert(attr.render.contains("&lt;"))
    assert(attr.render.contains("&gt;"))
    assert(!attr.render.contains("<script>"))
  }
}
