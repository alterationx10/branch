package dev.alteration.branch.spider.webview.html

import munit.FunSuite
import Components._
import Attributes._

/** Tests for the Components helper functions.
  *
  * Tests reusable UI component helpers for forms, inputs, buttons, and lists.
  */
class ComponentsSuite extends FunSuite {

  test("textInput creates input with correct attributes") {
    val input = textInput("username", "john_doe", "update-username")
    val html  = input.render

    assert(html.contains("""type="text""""))
    assert(html.contains("""name="username""""))
    assert(html.contains("""id="username""""))
    assert(html.contains("""value="john_doe""""))
    assert(html.contains("""wv-change="update-username""""))
  }

  test("textInput with placeholder") {
    val input = textInput(
      "email",
      "",
      "update-email",
      placeholder = Some("Enter your email")
    )
    val html  = input.render

    assert(html.contains("""placeholder="Enter your email""""))
  }

  test("textInput with extra attributes") {
    val input = textInput(
      "search",
      "",
      "search",
      extraAttrs = Seq(required := true, autofocus := true)
    )
    val html  = input.render

    assert(html.contains("required"))
    assert(html.contains("autofocus"))
  }

  test("emailInput creates email input") {
    val input = emailInput("email", "test@example.com", "update-email")
    val html  = input.render

    assert(html.contains("""type="email""""))
    assert(html.contains("""value="test@example.com""""))
  }

  test("passwordInput creates password input") {
    val input = passwordInput("password", "", "update-password")
    val html  = input.render

    assert(html.contains("""type="password""""))
    assert(html.contains("""name="password""""))
  }

  test("numberInput creates number input") {
    val input = numberInput("age", "25", "update-age")
    val html  = input.render

    assert(html.contains("""type="number""""))
    assert(html.contains("""value="25""""))
  }

  test("numberInput with min, max, and step") {
    val input = numberInput(
      "quantity",
      "5",
      "update-quantity",
      min = Some(1),
      max = Some(10),
      step = Some(1)
    )
    val html  = input.render

    assert(html.contains("""min="1""""))
    assert(html.contains("""max="10""""))
    assert(html.contains("""step="1""""))
  }

  test("textArea creates textarea") {
    val textarea = textArea("message", "Hello, World!", "update-message")
    val html     = textarea.render

    assert(html.contains("<textarea"))
    assert(html.contains("""name="message""""))
    assert(html.contains("Hello, World!"))
    assert(html.contains("""wv-change="update-message""""))
  }

  test("textArea with custom rows") {
    val textarea = textArea("bio", "", "update-bio", rows = 5)
    val html     = textarea.render

    assert(html.contains("""rows="5""""))
  }

  test("textArea with placeholder") {
    val textarea = textArea(
      "comment",
      "",
      "update-comment",
      placeholder = Some("Leave a comment")
    )
    val html     = textarea.render

    assert(html.contains("""placeholder="Leave a comment""""))
  }

  test("checkbox creates checkbox input") {
    val cb   = checkbox("agree", true, "toggle-agree")
    val html = cb.render

    assert(html.contains("""type="checkbox""""))
    assert(html.contains("""name="agree""""))
    assert(html.contains("checked"))
    assert(html.contains("""wv-click="toggle-agree""""))
  }

  test("checkbox with label") {
    val cb   = checkbox(
      "newsletter",
      false,
      "toggle-newsletter",
      labelText = Some("Subscribe to newsletter")
    )
    val html = cb.render

    assert(html.contains("<label"))
    assert(html.contains("Subscribe to newsletter"))
  }

  test("checkbox unchecked") {
    val cb   = checkbox("terms", false, "toggle-terms")
    val html = cb.render

    assert(!html.contains("checked"))
  }

  test("radio creates radio button") {
    val r    = radio("size", "medium", true, "select-size")
    val html = r.render

    assert(html.contains("""type="radio""""))
    assert(html.contains("""name="size""""))
    assert(html.contains("""value="medium""""))
    assert(html.contains("checked"))
  }

  test("radio with label") {
    val r    = radio(
      "color",
      "red",
      false,
      "select-color",
      labelText = Some("Red")
    )
    val html = r.render

    assert(html.contains("<label"))
    assert(html.contains("Red"))
  }

  test("selectDropdown creates select element") {
    val options = List(
      ("us", "United States"),
      ("uk", "United Kingdom"),
      ("ca", "Canada")
    )
    val select  = selectDropdown("country", options, "us", "update-country")
    val html    = select.render

    assert(html.contains("<select"))
    assert(html.contains("""name="country""""))
    assert(html.contains("""wv-change="update-country""""))
    assert(
      html.contains("""<option value="us" selected>United States</option>""")
    )
    assert(html.contains("""<option value="uk" >United Kingdom</option>"""))
    assert(html.contains("""<option value="ca" >Canada</option>"""))
  }

  test("selectDropdown with no selection") {
    val options = List(("a", "Option A"), ("b", "Option B"))
    val select  = selectDropdown("choice", options, "c", "update-choice")
    val html    = select.render

    // No option should be selected since "c" doesn't match
    assert(!html.contains("selected"))
  }

  test("submitButton creates submit button") {
    val button = submitButton("Save Changes")
    val html   = button.render

    assert(html.contains("""type="submit""""))
    assert(html.contains("Save Changes"))
  }

  test("submitButton with extra attributes") {
    val button = submitButton("Submit", extraAttrs = Seq(disabled := true))
    val html   = button.render

    assert(html.contains("disabled"))
  }

  test("clickButton creates button with click event") {
    val button = clickButton("Increment", "increment")
    val html   = button.render

    assert(html.contains("""type="button""""))
    assert(html.contains("""wv-click="increment""""))
    assert(html.contains("Increment"))
  }

  test("clickButton with extra attributes") {
    val button = clickButton(
      "Delete",
      "delete",
      extraAttrs = Seq(cls := "btn-danger")
    )
    val html   = button.render

    assert(html.contains("""class="btn-danger""""))
  }

  test("targetButton creates button with target value") {
    val button = targetButton("Delete", "delete-item", "item-123")
    val html   = button.render

    assert(html.contains("""wv-click="delete-item""""))
    assert(html.contains("""wv-target="item-123""""))
    assert(html.contains("Delete"))
  }

  test("keyedList renders list of items") {
    val items = List("Apple", "Banana", "Cherry")
    val list  = keyedList(
      items,
      (item, idx) => Html.Element("div", Nil, List(Html.Text(s"$idx: $item")))
    )
    val html  = list.render

    assert(html.contains("0: Apple"))
    assert(html.contains("1: Banana"))
    assert(html.contains("2: Cherry"))
  }

  test("keyedList with custom container") {
    val items = List(1, 2, 3)
    val list  = keyedList(
      items,
      (item, _) => Html.Element("span", Nil, List(Html.Text(item.toString))),
      containerTag = "section",
      containerAttrs = Seq(cls := "items")
    )
    val html  = list.render

    assert(html.contains("<section"))
    assert(html.contains("""class="items""""))
    assert(html.contains("<span>1</span>"))
  }

  test("unorderedList renders ul with li items") {
    val items = List("First", "Second", "Third")
    val list  = unorderedList(
      items,
      item => Html.Text(item)
    )
    val html  = list.render

    assert(html.contains("<ul>"))
    assert(html.contains("<li>First</li>"))
    assert(html.contains("<li>Second</li>"))
    assert(html.contains("<li>Third</li>"))
    assert(html.contains("</ul>"))
  }

  test("unorderedList with extra attributes") {
    val items = List("A", "B")
    val list  = unorderedList(
      items,
      item => Html.Text(item),
      extraAttrs = Seq(cls := "my-list")
    )
    val html  = list.render

    assert(html.contains("""<ul class="my-list">"""))
  }

  test("orderedList renders ol with li items") {
    val items = List("First", "Second", "Third")
    val list  = orderedList(
      items,
      item => Html.Text(item)
    )
    val html  = list.render

    assert(html.contains("<ol>"))
    assert(html.contains("<li>First</li>"))
    assert(html.contains("</ol>"))
  }

  test("container creates div with max-width and padding") {
    val c    = container(Html.Text("Content"))(
      maxWidth = Some("800px"),
      padding = Some("20px")
    )
    val html = c.render

    assert(html.contains("<div"))
    assert(html.contains("max-width: 800px"))
    assert(html.contains("padding: 20px"))
    assert(html.contains("margin: 0 auto"))
  }

  test("container without styling") {
    val c    = container(Html.Text("Content"))()
    val html = c.render

    assert(html.contains("<div"))
    assert(html.contains("Content"))
  }

  test("flexContainer creates flex layout") {
    val flex = flexContainer(
      Html.Text("Item 1"),
      Html.Text("Item 2")
    )(
      direction = "row",
      gap = Some("16px"),
      justifyContent = Some("space-between"),
      alignItems = Some("center")
    )
    val html = flex.render

    assert(html.contains("display: flex"))
    assert(html.contains("flex-direction: row"))
    assert(html.contains("gap: 16px"))
    assert(html.contains("justify-content: space-between"))
    assert(html.contains("align-items: center"))
  }

  test("flexContainer with column direction") {
    val flex = flexContainer(Html.Text("A"))(direction = "column")
    val html = flex.render

    assert(html.contains("flex-direction: column"))
  }

  test("form inputs escape user values") {
    val malicious = """<script>alert("xss")</script>"""
    val input     = textInput("name", malicious, "update")
    val html      = input.render

    assert(!html.contains("<script>"))
    assert(html.contains("&lt;script&gt;"))
  }

  test("textarea escapes content") {
    val malicious = """<script>alert("xss")</script>"""
    val textarea  = textArea("comment", malicious, "update")
    val html      = textarea.render

    assert(!html.contains("<script>alert"))
    assert(html.contains("&lt;script&gt;"))
  }
}
