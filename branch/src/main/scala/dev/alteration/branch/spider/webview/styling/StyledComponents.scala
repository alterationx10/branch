package dev.alteration.branch.spider.webview.styling

import dev.alteration.branch.spider.webview.html.{Attr, Attributes, Html}

/** Styled components for Branch WebView.
  *
  * Inspired by styled-components, provides a clean API for creating styled HTML
  * elements.
  *
  * Example:
  * {{{
  * object TodoComponents {
  *   val Container = styled.div(
  *     "max-width" -> "600px",
  *     "margin" -> "0 auto",
  *     "padding" -> "20px",
  *     "background" -> "#fff"
  *   )
  *
  *   val Button = styled.button(
  *     "padding" -> "10px 20px",
  *     "background" -> CSSUtils.Colors.primary,
  *     "color" -> "white",
  *     "border" -> "none",
  *     "border-radius" -> CSSUtils.Radius.md,
  *     "cursor" -> "pointer"
  *   )
  * }
  *
  * // Usage:
  * TodoComponents.Container(id := "main")(
  *   h1(Html.Text("My App")),
  *   TodoComponents.Button(wvClick := "submit")(
  *     Html.Text("Submit")
  *   )
  * )
  * }}}
  */
object styled {

  private val registry = StyledComponentRegistry()

  /** Create a styled div */
  def div(properties: (String, String)*): StyledComponent = {
    StyledComponent("div", properties, registry)
  }

  /** Create a styled span */
  def span(properties: (String, String)*): StyledComponent = {
    StyledComponent("span", properties, registry)
  }

  /** Create a styled button */
  def button(properties: (String, String)*): StyledComponent = {
    StyledComponent("button", properties, registry)
  }

  /** Create a styled input */
  def input(properties: (String, String)*): StyledComponent = {
    StyledComponent("input", properties, registry)
  }

  /** Create a styled a (anchor) */
  def a(properties: (String, String)*): StyledComponent = {
    StyledComponent("a", properties, registry)
  }

  /** Create a styled p */
  def p(properties: (String, String)*): StyledComponent = {
    StyledComponent("p", properties, registry)
  }

  /** Create a styled h1 */
  def h1(properties: (String, String)*): StyledComponent = {
    StyledComponent("h1", properties, registry)
  }

  /** Create a styled h2 */
  def h2(properties: (String, String)*): StyledComponent = {
    StyledComponent("h2", properties, registry)
  }

  /** Create a styled h3 */
  def h3(properties: (String, String)*): StyledComponent = {
    StyledComponent("h3", properties, registry)
  }

  /** Create a styled ul */
  def ul(properties: (String, String)*): StyledComponent = {
    StyledComponent("ul", properties, registry)
  }

  /** Create a styled li */
  def li(properties: (String, String)*): StyledComponent = {
    StyledComponent("li", properties, registry)
  }

  /** Create a styled section */
  def section(properties: (String, String)*): StyledComponent = {
    StyledComponent("section", properties, registry)
  }

  /** Create a styled article */
  def article(properties: (String, String)*): StyledComponent = {
    StyledComponent("article", properties, registry)
  }

  /** Create a styled header */
  def header(properties: (String, String)*): StyledComponent = {
    StyledComponent("header", properties, registry)
  }

  /** Create a styled footer */
  def footer(properties: (String, String)*): StyledComponent = {
    StyledComponent("footer", properties, registry)
  }

  /** Create a styled nav */
  def nav(properties: (String, String)*): StyledComponent = {
    StyledComponent("nav", properties, registry)
  }

  /** Get all CSS from the registry as a style tag */
  def toStyleTag: String = registry.toStyleTag
}

/** A styled component that can be used like a regular HTML element. */
case class StyledComponent(
    tagName: String,
    properties: Seq[(String, String)],
    registry: StyledComponentRegistry
) {

  private val className = registry.register(tagName, properties)

  /** Apply attributes and children to create the styled element.
    *
    * @param attrs
    *   Additional HTML attributes
    * @return
    *   A function that accepts children and returns Html
    */
  def apply(attrs: Attr*)(children: Html*): Html = {
    // For now, just add the class - combining with existing classes would require
    // parsing the attr list which is more complex
    val finalAttrs = (Attributes.cls := className) :: attrs.toList

    Html.Element(tagName, finalAttrs, children.toList)
  }

  /** Create element without additional attributes */
  def apply(children: Html*): Html = {
    apply()(children*)
  }
}

/** Registry for styled components to track and generate CSS. */
class StyledComponentRegistry {
  import java.util.concurrent.atomic.AtomicInteger

  private val idCounter = new AtomicInteger(0)
  private val styles    = scala.collection.mutable.Map.empty[String, String]

  def register(tagName: String, properties: Seq[(String, String)]): String = {
    val className = s"sc-${idCounter.getAndIncrement()}"
    val cssText   = properties
      .map { case (prop, value) => s"  $prop: $value;" }
      .mkString("\n")
    styles(className) = s".$className {\n$cssText\n}"
    className
  }

  def toCss: String = styles.values.mkString("\n\n")

  def toStyleTag: String = s"<style>\n${toCss}\n</style>"
}

/** Theme support for consistent styling across components. */
trait Theme {
  def colors: Map[String, String]
  def spacing: Map[String, String]
  def typography: Map[String, String]
}

/** Default theme based on Tailwind CSS colors */
object DefaultTheme extends Theme {
  val colors = Map(
    "primary"   -> "#667eea",
    "secondary" -> "#764ba2",
    "success"   -> "#48bb78",
    "danger"    -> "#f56565",
    "warning"   -> "#ed8936",
    "info"      -> "#38b2ac",
    "light"     -> "#f7fafc",
    "dark"      -> "#2d3748",
    "gray"      -> "#a0aec0"
  )

  val spacing = Map(
    "xs"  -> "4px",
    "sm"  -> "8px",
    "md"  -> "16px",
    "lg"  -> "24px",
    "xl"  -> "32px",
    "xxl" -> "48px"
  )

  val typography = Map(
    "xs"   -> "0.75rem",
    "sm"   -> "0.875rem",
    "base" -> "1rem",
    "lg"   -> "1.125rem",
    "xl"   -> "1.25rem",
    "2xl"  -> "1.5rem",
    "3xl"  -> "1.875rem",
    "4xl"  -> "2.25rem"
  )
}
