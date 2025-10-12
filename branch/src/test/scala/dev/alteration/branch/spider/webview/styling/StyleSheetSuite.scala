package dev.alteration.branch.spider.webview.styling

import munit.FunSuite

/** Tests for StyleSheet.
  *
  * Tests CSS-in-Scala styling with automatic scoping.
  */
class StyleSheetSuite extends FunSuite {

  test("StyleSheet generates unique class names") {
    object TestStyles1 extends StyleSheet {
      val container = style("padding" -> "20px")
    }

    object TestStyles2 extends StyleSheet {
      val container = style("padding" -> "20px")
    }

    // Class names should be different for different stylesheets
    assertNotEquals(TestStyles1.container, TestStyles2.container)
  }

  test("StyleSheet class names follow wv-{scopeId}-{index} pattern") {
    object TestStyles extends StyleSheet {
      val style1 = style("color" -> "red")
      val style2 = style("color" -> "blue")
    }

    assert(TestStyles.style1.startsWith("wv-"))
    assert(TestStyles.style2.startsWith("wv-"))
    assertNotEquals(TestStyles.style1, TestStyles.style2)
  }

  test("style creates CSS with properties") {
    object TestStyles extends StyleSheet {
      val button = style(
        "padding"    -> "10px 20px",
        "background" -> "#667eea",
        "color"      -> "white"
      )
    }

    val css = TestStyles.toCss

    assert(css.contains(TestStyles.button))
    assert(css.contains("padding: 10px 20px;"))
    assert(css.contains("background: #667eea;"))
    assert(css.contains("color: white;"))
  }

  test("css creates CSS from raw text") {
    object TestStyles extends StyleSheet {
      val custom = css("""
        display: flex;
        justify-content: center;
        align-items: center;
      """)
    }

    val cssText = TestStyles.toCss

    assert(cssText.contains(TestStyles.custom))
    assert(cssText.contains("display: flex;"))
    assert(cssText.contains("justify-content: center;"))
  }

  test("toCss generates complete CSS") {
    object TestStyles extends StyleSheet {
      val red   = style("color" -> "red")
      val blue  = style("color" -> "blue")
      val green = style("color" -> "green")
    }

    val css = TestStyles.toCss

    assert(css.contains(s".${TestStyles.red}"))
    assert(css.contains(s".${TestStyles.blue}"))
    assert(css.contains(s".${TestStyles.green}"))
  }

  test("toStyleTag wraps CSS in style tag") {
    object TestStyles extends StyleSheet {
      val container = style("padding" -> "20px")
    }

    val tag = TestStyles.toStyleTag

    assert(tag.startsWith("<style>"))
    assert(tag.endsWith("</style>"))
    assert(tag.contains(TestStyles.container))
    assert(tag.contains("padding: 20px;"))
  }

  test("multiple styles are separated correctly in toCss") {
    object TestStyles extends StyleSheet {
      val style1 = style("color" -> "red")
      val style2 = style("color" -> "blue")
    }

    val css = TestStyles.toCss

    // Should have two separate CSS rules
    val ruleCount = css.split("\\}").length - 1
    assertEquals(ruleCount, 2)
  }

  test("empty StyleSheet generates empty CSS") {
    object EmptyStyles extends StyleSheet {
      // No styles defined
    }

    val css = EmptyStyles.toCss
    assertEquals(css, "")
  }

  test("CSSUtils Colors are defined") {
    import CSSUtils.Colors._

    assertEquals(primary, "#667eea")
    assertEquals(secondary, "#764ba2")
    assertEquals(success, "#48bb78")
    assertEquals(danger, "#f56565")
    assertEquals(warning, "#ed8936")
    assertEquals(info, "#38b2ac")
  }

  test("CSSUtils Spacing is defined") {
    import CSSUtils.Spacing._

    assertEquals(xs, "4px")
    assertEquals(sm, "8px")
    assertEquals(md, "16px")
    assertEquals(lg, "24px")
    assertEquals(xl, "32px")
    assertEquals(xxl, "48px")
  }

  test("CSSUtils Radius is defined") {
    import CSSUtils.Radius._

    assertEquals(sm, "4px")
    assertEquals(md, "8px")
    assertEquals(lg, "12px")
    assertEquals(full, "9999px")
  }

  test("CSSUtils flex helper generates flexbox properties") {
    val props = CSSUtils.flex(
      direction = "column",
      justify = "space-between",
      align = "center",
      gap = "16px"
    )

    val propsMap = props.toMap
    assertEquals(propsMap.get("display"), Some("flex"))
    assertEquals(propsMap.get("flex-direction"), Some("column"))
    assertEquals(propsMap.get("justify-content"), Some("space-between"))
    assertEquals(propsMap.get("align-items"), Some("center"))
    assertEquals(propsMap.get("gap"), Some("16px"))
  }

  test("CSSUtils grid helper generates grid properties") {
    val props = CSSUtils.grid(
      columns = "1fr 1fr 1fr",
      rows = "auto",
      gap = "20px"
    )

    val propsMap = props.toMap
    assertEquals(propsMap.get("display"), Some("grid"))
    assertEquals(propsMap.get("grid-template-columns"), Some("1fr 1fr 1fr"))
    assertEquals(propsMap.get("grid-template-rows"), Some("auto"))
    assertEquals(propsMap.get("gap"), Some("20px"))
  }

  test("CSSUtils absolute helper generates positioning") {
    val props = CSSUtils.absolute(
      top = Some("10px"),
      right = Some("20px"),
      bottom = Some("30px"),
      left = Some("40px")
    )

    val propsMap = props.toMap
    assertEquals(propsMap.get("position"), Some("absolute"))
    assertEquals(propsMap.get("top"), Some("10px"))
    assertEquals(propsMap.get("right"), Some("20px"))
    assertEquals(propsMap.get("bottom"), Some("30px"))
    assertEquals(propsMap.get("left"), Some("40px"))
  }

  test("CSSUtils absolute with partial positioning") {
    val props = CSSUtils.absolute(top = Some("0"), left = Some("0"))

    val propsMap = props.toMap
    assertEquals(propsMap.get("position"), Some("absolute"))
    assertEquals(propsMap.get("top"), Some("0"))
    assertEquals(propsMap.get("left"), Some("0"))
    assertEquals(propsMap.get("right"), None)
    assertEquals(propsMap.get("bottom"), None)
  }

  test("CSSUtils transition helper") {
    val transition = CSSUtils.transition(
      property = "opacity",
      duration = "300ms",
      timing = "ease-in-out"
    )

    assertEquals(transition._1, "transition")
    assertEquals(transition._2, "opacity 300ms ease-in-out")
  }

  test("String withClass extension combines class names") {
    val combined = "btn".withClass("btn-primary")
    assertEquals(combined, "btn btn-primary")
  }

  test("String withClass extension chains multiple classes") {
    val combined = "btn".withClass("btn-primary").withClass("btn-lg")
    assertEquals(combined, "btn btn-primary btn-lg")
  }

  test("StyleSheet with CSSUtils integration") {
    object AppStyles extends StyleSheet {
      import CSSUtils._

      val button = style(
        "padding"    -> Spacing.md,
        "background" -> Colors.primary,
        "color"      -> "white",
        "border-radius" -> Radius.md
      )
    }

    val css = AppStyles.toCss

    assert(css.contains("padding: 16px;"))
    assert(css.contains("background: #667eea;"))
    assert(css.contains("border-radius: 8px;"))
  }

  test("StyleSheet with flex helper") {
    object LayoutStyles extends StyleSheet {
      val flexContainer = style(CSSUtils.flex(
        direction = "row",
        justify = "center",
        gap = "10px"
      )*)
    }

    val css = LayoutStyles.toCss

    assert(css.contains("display: flex;"))
    assert(css.contains("flex-direction: row;"))
    assert(css.contains("justify-content: center;"))
  }

  test("Multiple StyleSheets don't interfere with each other") {
    object Styles1 extends StyleSheet {
      val red = style("color" -> "red")
    }

    object Styles2 extends StyleSheet {
      val blue = style("color" -> "blue")
    }

    val css1 = Styles1.toCss
    val css2 = Styles2.toCss

    assert(css1.contains("color: red;"))
    assert(!css1.contains("color: blue;"))
    assert(css2.contains("color: blue;"))
    assert(!css2.contains("color: red;"))
  }
}
