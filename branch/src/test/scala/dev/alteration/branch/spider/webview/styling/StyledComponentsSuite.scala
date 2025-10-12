package dev.alteration.branch.spider.webview.styling

import dev.alteration.branch.spider.webview.html.{Attributes, Html, WebViewAttributes}
import munit.FunSuite

/** Tests for StyledComponents.
  *
  * Tests styled component creation and usage.
  */
class StyledComponentsSuite extends FunSuite {

  test("styled.div creates a div with scoped class") {
    val StyledDiv = styled.div("color" -> "red")
    val html      = StyledDiv(Html.Text("Content"))

    val rendered = html.render
    assert(rendered.contains("<div"))
    assert(rendered.contains("class=\"sc-"))
    assert(rendered.contains("Content"))
  }

  test("styled.button creates a button with styles") {
    val StyledButton = styled.button(
      "padding"    -> "10px 20px",
      "background" -> "#667eea"
    )
    val html         = StyledButton(Html.Text("Click me"))

    val rendered = html.render
    assert(rendered.contains("<button"))
    assert(rendered.contains("class=\"sc-"))
    assert(rendered.contains("Click me"))
  }

  test("styled.span creates a span with styles") {
    val StyledSpan = styled.span("font-weight" -> "bold")
    val html       = StyledSpan(Html.Text("Bold text"))

    assert(html.render.contains("<span"))
    assert(html.render.contains("Bold text"))
  }

  test("styled components can have additional attributes") {
    val StyledDiv = styled.div("color" -> "blue")
    val html      = StyledDiv(Attributes.id := "main")(Html.Text("Content"))

    val rendered = html.render
    assert(rendered.contains("id=\"main\""))
    assert(rendered.contains("class=\"sc-"))
  }

  test("styled components work with WebView attributes") {
    val StyledButton = styled.button("background" -> "green")
    val html         = StyledButton(WebViewAttributes.wvClick := "handleClick")(
      Html.Text("Submit")
    )

    val rendered = html.render
    assert(rendered.contains("wv-click=\"handleClick\""))
    assert(rendered.contains("class=\"sc-"))
  }

  test("styled components generate unique class names") {
    val Component1 = styled.div("color" -> "red")
    val Component2 = styled.div("color" -> "blue")

    val html1 = Component1(Html.Text("A"))
    val html2 = Component2(Html.Text("B"))

    val class1 = html1.render.split("class=\"")(1).split("\"")(0)
    val class2 = html2.render.split("class=\"")(1).split("\"")(0)

    assertNotEquals(class1, class2)
  }

  test("styled.toStyleTag generates CSS") {
    // Create some styled components
    val _Button = styled.button("background" -> "blue")
    val _Div    = styled.div("color" -> "red")

    val styleTag = styled.toStyleTag

    assert(styleTag.startsWith("<style>"))
    assert(styleTag.endsWith("</style>"))
    assert(styleTag.contains("background: blue;"))
    assert(styleTag.contains("color: red;"))
  }

  test("styled components with no children") {
    val StyledInput = styled.input("border" -> "1px solid #ccc")
    val html        = StyledInput()

    val rendered = html.render
    assert(rendered.contains("<input"))
    assert(rendered.contains("class=\"sc-"))
  }

  test("styled components with multiple children") {
    val StyledDiv = styled.div("padding" -> "20px")
    val html      = StyledDiv(
      Html.Text("First"),
      Html.Text(" "),
      Html.Text("Second")
    )

    val rendered = html.render
    assert(rendered.contains("First"))
    assert(rendered.contains("Second"))
  }

  test("styled components with nested elements") {
    val Container = styled.div("max-width" -> "800px")
    val Title     = styled.h1("color" -> "#333")

    val html = Container(
      Title(Html.Text("Page Title"))
    )

    val rendered = html.render
    assert(rendered.contains("<div"))
    assert(rendered.contains("<h1"))
    assert(rendered.contains("Page Title"))
  }

  test("styled.p creates paragraph") {
    val Paragraph = styled.p("line-height" -> "1.6")
    val html      = Paragraph(Html.Text("Text"))

    assert(html.render.contains("<p"))
  }

  test("styled.h1, h2, h3 create headings") {
    val H1 = styled.h1("font-size" -> "2rem")
    val H2 = styled.h2("font-size" -> "1.5rem")
    val H3 = styled.h3("font-size" -> "1.25rem")

    assert(H1(Html.Text("Title")).render.contains("<h1"))
    assert(H2(Html.Text("Title")).render.contains("<h2"))
    assert(H3(Html.Text("Title")).render.contains("<h3"))
  }

  test("styled.a creates link") {
    val Link = styled.a(
      "color"           -> "#667eea",
      "text-decoration" -> "none"
    )
    val html = Link(Attributes.href := "https://example.com")(
      Html.Text("Link")
    )

    val rendered = html.render
    assert(rendered.contains("<a"))
    assert(rendered.contains("href=\"https://example.com\""))
  }

  test("styled.ul and styled.li for lists") {
    val List     = styled.ul("list-style" -> "none")
    val ListItem = styled.li("padding" -> "5px")

    val html = List(
      ListItem(Html.Text("Item 1")),
      ListItem(Html.Text("Item 2"))
    )

    val rendered = html.render
    assert(rendered.contains("<ul"))
    assert(rendered.contains("<li"))
    assert(rendered.contains("Item 1"))
    assert(rendered.contains("Item 2"))
  }

  test("styled.section, article, header, footer, nav") {
    val Section = styled.section("padding" -> "20px")
    val Article = styled.article("margin" -> "10px")
    val Header  = styled.header("background" -> "#f0f0f0")
    val Footer  = styled.footer("text-align" -> "center")
    val Nav     = styled.nav("display" -> "flex")

    assert(Section(Html.Text("S")).render.contains("<section"))
    assert(Article(Html.Text("A")).render.contains("<article"))
    assert(Header(Html.Text("H")).render.contains("<header"))
    assert(Footer(Html.Text("F")).render.contains("<footer"))
    assert(Nav(Html.Text("N")).render.contains("<nav"))
  }

  test("StyledComponent with CSSUtils integration") {
    val Button = styled.button(
      "padding"    -> CSSUtils.Spacing.md,
      "background" -> CSSUtils.Colors.primary,
      "border-radius" -> CSSUtils.Radius.md
    )

    val styleTag = styled.toStyleTag
    assert(styleTag.contains("padding: 16px;"))
    assert(styleTag.contains("background: #667eea;"))
    assert(styleTag.contains("border-radius: 8px;"))
  }

  test("DefaultTheme colors are defined") {
    assertEquals(DefaultTheme.colors.get("primary"), Some("#667eea"))
    assertEquals(DefaultTheme.colors.get("secondary"), Some("#764ba2"))
    assertEquals(DefaultTheme.colors.get("success"), Some("#48bb78"))
    assertEquals(DefaultTheme.colors.get("danger"), Some("#f56565"))
  }

  test("DefaultTheme spacing is defined") {
    assertEquals(DefaultTheme.spacing.get("xs"), Some("4px"))
    assertEquals(DefaultTheme.spacing.get("sm"), Some("8px"))
    assertEquals(DefaultTheme.spacing.get("md"), Some("16px"))
    assertEquals(DefaultTheme.spacing.get("lg"), Some("24px"))
  }

  test("DefaultTheme typography is defined") {
    assertEquals(DefaultTheme.typography.get("xs"), Some("0.75rem"))
    assertEquals(DefaultTheme.typography.get("sm"), Some("0.875rem"))
    assertEquals(DefaultTheme.typography.get("base"), Some("1rem"))
    assertEquals(DefaultTheme.typography.get("lg"), Some("1.125rem"))
  }

  test("StyledComponent with flexbox layout") {
    val FlexContainer = styled.div(CSSUtils.flex(
      direction = "row",
      justify = "space-between",
      align = "center",
      gap = "10px"
    )*)

    val styleTag = styled.toStyleTag
    assert(styleTag.contains("display: flex;"))
    assert(styleTag.contains("flex-direction: row;"))
    assert(styleTag.contains("justify-content: space-between;"))
  }

  test("StyledComponent CSS rules are properly formatted") {
    // Create a component to generate CSS
    val TestComponent = styled.div(
      "color"   -> "red",
      "padding" -> "10px"
    )

    // Generate the element to register the style
    val _html = TestComponent(Html.Text("Test"))

    val css = styled.toStyleTag

    // Should have proper CSS syntax
    assert(css.contains("{"))
    assert(css.contains("}"))
    assert(css.contains(";"))
  }

  test("Multiple styled components in single render") {
    val Card   = styled.div(
      "border"        -> "1px solid #ccc",
      "border-radius" -> "8px",
      "padding"       -> "16px"
    )
    val Title  = styled.h2("margin" -> "0 0 10px 0")
    val Text   = styled.p("color" -> "#666")

    val html = Card(
      Title(Html.Text("Card Title")),
      Text(Html.Text("Card content"))
    )

    val rendered = html.render
    assert(rendered.contains("Card Title"))
    assert(rendered.contains("Card content"))
    assert(rendered.contains("class=\"sc-"))
  }
}
