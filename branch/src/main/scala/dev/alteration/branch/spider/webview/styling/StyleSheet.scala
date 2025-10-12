package dev.alteration.branch.spider.webview.styling

import java.util.concurrent.atomic.AtomicInteger

/** CSS-in-Scala styling solution for Branch WebView.
  *
  * Provides automatic scoping with unique class names to avoid CSS conflicts.
  *
  * Example:
  * {{{
  * object TodoStyles extends StyleSheet {
  *   val container = style(
  *     "max-width" -> "600px",
  *     "margin" -> "0 auto",
  *     "padding" -> "20px"
  *   )
  *
  *   val item = style(
  *     "padding" -> "10px",
  *     "border-bottom" -> "1px solid #ccc",
  *     "display" -> "flex"
  *   )
  * }
  *
  * // In render:
  * div(cls := TodoStyles.container)(...)
  * }}}
  */
abstract class StyleSheet {

  private val scopeId = StyleSheet.nextId()
  private val styles  = scala.collection.mutable.Map.empty[String, String]

  /** Define a new CSS class with the given properties.
    *
    * @param properties
    *   CSS property-value pairs
    * @return
    *   A scoped class name
    */
  protected def style(properties: (String, String)*): String = {
    val className = s"wv-${scopeId}-${styles.size}"
    val cssText   = properties
      .map { case (prop, value) => s"  $prop: $value;" }
      .mkString("\n")
    styles(className) = s".$className {\n$cssText\n}"
    className
  }

  /** Define a new CSS class with raw CSS text.
    *
    * @param cssText
    *   Raw CSS properties (without selector)
    * @return
    *   A scoped class name
    */
  protected def css(cssText: String): String = {
    val className = s"wv-${scopeId}-${styles.size}"
    styles(className) = s".$className {\n${cssText.trim}\n}"
    className
  }

  /** Get all CSS rules as a single string.
    *
    * @return
    *   CSS text for all defined styles
    */
  def toCss: String = styles.values.mkString("\n\n")

  /** Get a <style> tag with all CSS rules.
    *
    * @return
    *   HTML style tag
    */
  def toStyleTag: String = s"<style>\n${toCss}\n</style>"
}

object StyleSheet {
  private val idCounter = new AtomicInteger(0)

  private[styling] def nextId(): Int = idCounter.getAndIncrement()
}

/** CSS utilities and common patterns. */
object CSSUtils {

  /** Common color palette */
  object Colors {
    val primary   = "#667eea"
    val secondary = "#764ba2"
    val success   = "#48bb78"
    val danger    = "#f56565"
    val warning   = "#ed8936"
    val info      = "#38b2ac"
    val light     = "#f7fafc"
    val dark      = "#2d3748"
    val gray      = "#a0aec0"
  }

  /** Common spacing values */
  object Spacing {
    val xs  = "4px"
    val sm  = "8px"
    val md  = "16px"
    val lg  = "24px"
    val xl  = "32px"
    val xxl = "48px"
  }

  /** Common border radius values */
  object Radius {
    val sm   = "4px"
    val md   = "8px"
    val lg   = "12px"
    val full = "9999px"
  }

  /** Common shadow definitions */
  object Shadows {
    val sm = "0 1px 2px rgba(0,0,0,0.05)"
    val md = "0 4px 6px rgba(0,0,0,0.1)"
    val lg = "0 10px 15px rgba(0,0,0,0.1)"
    val xl = "0 20px 25px rgba(0,0,0,0.15)"
  }

  /** Generate flexbox properties */
  def flex(
      direction: String = "row",
      justify: String = "flex-start",
      align: String = "stretch",
      gap: String = "0"
  ): Seq[(String, String)] = Seq(
    "display"         -> "flex",
    "flex-direction"  -> direction,
    "justify-content" -> justify,
    "align-items"     -> align,
    "gap"             -> gap
  )

  /** Generate grid properties */
  def grid(
      columns: String = "1fr",
      rows: String = "auto",
      gap: String = "0"
  ): Seq[(String, String)] = Seq(
    "display"               -> "grid",
    "grid-template-columns" -> columns,
    "grid-template-rows"    -> rows,
    "gap"                   -> gap
  )

  /** Generate absolute positioning */
  def absolute(
      top: Option[String] = None,
      right: Option[String] = None,
      bottom: Option[String] = None,
      left: Option[String] = None
  ): Seq[(String, String)] = {
    val base      = Seq("position" -> "absolute")
    val positions = Seq(
      top.map("top" -> _),
      right.map("right" -> _),
      bottom.map("bottom" -> _),
      left.map("left" -> _)
    ).flatten
    base ++ positions
  }

  /** Generate transition properties */
  def transition(
      property: String = "all",
      duration: String = "200ms",
      timing: String = "ease"
  ): (String, String) = {
    "transition" -> s"$property $duration $timing"
  }
}

/** Extension class to add CSS helper methods. */
extension (s: String) {

  /** Combine multiple class names */
  def +(other: String): String = s"$s $other"
}
