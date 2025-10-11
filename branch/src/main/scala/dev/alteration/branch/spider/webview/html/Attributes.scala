package dev.alteration.branch.spider.webview.html

import Attr._

/** Type-safe HTML attribute builders.
  *
  * Provides a DSL for creating attributes with the `:=` operator:
  *
  * {{{
  * div(
  *   cls := "container",
  *   id := "main",
  *   style := "color: red"
  * )(...)
  * }}}
  *
  * All attribute values are automatically escaped for security.
  */
object Attributes {

  /** Attribute builder with `:=` operator.
    *
    * @param key
    *   The attribute name
    */
  case class AttrBuilder(key: String) {

    /** Create a string attribute.
      *
      * @param value
      *   The attribute value
      */
    def :=(value: String): Attr = StringAttr(key, value)

    /** Create a boolean attribute.
      *
      * @param value
      *   Whether the attribute is present
      */
    def :=(value: Boolean): Attr = BooleanAttr(key, value)

    /** Create an attribute from any value (calls toString).
      *
      * @param value
      *   The value to convert to a string
      */
    def :=(value: Any): Attr = StringAttr(key, value.toString)
  }

  // === Standard HTML Attributes ===

  /** The `class` attribute. Use `cls` instead of `class` (reserved keyword). */
  val cls = AttrBuilder("class")

  /** The `id` attribute. */
  val id = AttrBuilder("id")

  /** The `style` attribute (inline styles). */
  val style = AttrBuilder("style")

  /** The `title` attribute (tooltip). */
  val title = AttrBuilder("title")

  /** The `name` attribute (form fields). */
  val name = AttrBuilder("name")

  /** The `value` attribute (form fields). */
  val value = AttrBuilder("value")

  /** The `placeholder` attribute (form fields). */
  val placeholder = AttrBuilder("placeholder")

  /** The `type` attribute (input, button, etc.). */
  val tpe = AttrBuilder("type")

  /** The `href` attribute (links). */
  val href = AttrBuilder("href")

  /** The `src` attribute (images, scripts). */
  val src = AttrBuilder("src")

  /** The `alt` attribute (images). */
  val alt = AttrBuilder("alt")

  /** The `width` attribute. */
  val width = AttrBuilder("width")

  /** The `height` attribute. */
  val height = AttrBuilder("height")

  /** The `target` attribute (links). */
  val target = AttrBuilder("target")

  /** The `rel` attribute (links). */
  val rel = AttrBuilder("rel")

  /** The `action` attribute (forms). */
  val action = AttrBuilder("action")

  /** The `method` attribute (forms). */
  val method = AttrBuilder("method")

  /** The `enctype` attribute (forms). */
  val enctype = AttrBuilder("enctype")

  // === Boolean Attributes ===

  /** The `disabled` attribute. */
  val disabled = AttrBuilder("disabled")

  /** The `readonly` attribute. */
  val readonly = AttrBuilder("readonly")

  /** The `required` attribute. */
  val required = AttrBuilder("required")

  /** The `checked` attribute (checkboxes, radio buttons). */
  val checked = AttrBuilder("checked")

  /** The `selected` attribute (option elements). */
  val selected = AttrBuilder("selected")

  /** The `autofocus` attribute. */
  val autofocus = AttrBuilder("autofocus")

  /** The `autoplay` attribute (media). */
  val autoplay = AttrBuilder("autoplay")

  /** The `controls` attribute (media). */
  val controls = AttrBuilder("controls")

  /** The `loop` attribute (media). */
  val loop = AttrBuilder("loop")

  /** The `muted` attribute (media). */
  val muted = AttrBuilder("muted")

  /** The `multiple` attribute (select, file input). */
  val multiple = AttrBuilder("multiple")

  // === ARIA Attributes ===

  /** ARIA label (accessibility). */
  val ariaLabel = AttrBuilder("aria-label")

  /** ARIA described by (accessibility). */
  val ariaDescribedBy = AttrBuilder("aria-describedby")

  /** ARIA hidden (accessibility). */
  val ariaHidden = AttrBuilder("aria-hidden")

  /** ARIA role (accessibility). */
  val role = AttrBuilder("role")

  // === Data Attributes ===

  /** Create a custom data attribute.
    *
    * {{{
    * div(data("user-id") := "123")
    * // Renders: <div data-user-id="123">
    * }}}
    */
  def data(key: String): AttrBuilder = AttrBuilder(s"data-$key")

  /** Create a custom attribute.
    *
    * {{{
    * div(attr("my-custom-attr") := "value")
    * }}}
    */
  def attr(key: String): AttrBuilder = AttrBuilder(key)

  // === Conditional Attributes ===

  /** Conditionally include an attribute.
    *
    * {{{
    * div(attrWhen(isActive, cls := "active"))
    * }}}
    */
  def attrWhen(condition: Boolean, attr: => Attr): Attr = {
    if (condition) attr else EmptyAttr
  }

  /** Conditionally choose between two attributes.
    *
    * {{{
    * div(attrCond(isActive, cls := "active", cls := "inactive"))
    * }}}
    */
  def attrCond(condition: Boolean, ifTrue: => Attr, ifFalse: => Attr): Attr = {
    if (condition) ifTrue else ifFalse
  }

  // === Class Name Helpers ===

  /** Combine multiple class names.
    *
    * {{{
    * div(classes("btn", "btn-primary", "disabled"))
    * // Renders: class="btn btn-primary disabled"
    * }}}
    */
  def classes(names: String*): Attr = {
    cls := names.filter(_.nonEmpty).mkString(" ")
  }

  /** Conditionally include class names.
    *
    * {{{
    * div(classWhen(
    *   "active" -> isActive,
    *   "disabled" -> isDisabled,
    *   "loading" -> isLoading
    * ))
    * }}}
    */
  def classWhen(conditions: (String, Boolean)*): Attr = {
    val activeClasses = conditions.collect {
      case (name, true) => name
    }
    if (activeClasses.isEmpty) EmptyAttr
    else cls := activeClasses.mkString(" ")
  }

  // === Style Helpers ===

  /** Combine multiple inline styles.
    *
    * {{{
    * div(styles(
    *   "color" -> "red",
    *   "font-size" -> "16px",
    *   "margin" -> "10px"
    * ))
    * // Renders: style="color: red; font-size: 16px; margin: 10px"
    * }}}
    */
  def styles(properties: (String, String)*): Attr = {
    val styleString = properties
      .map { case (key, value) => s"$key: $value" }
      .mkString("; ")
    style := styleString
  }

  /** Conditionally include style properties.
    *
    * {{{
    * div(styleWhen(
    *   ("color", "red", isError),
    *   ("display", "none", isHidden)
    * ))
    * }}}
    */
  def styleWhen(properties: (String, String, Boolean)*): Attr = {
    val activeStyles = properties.collect {
      case (key, value, true) => (key, value)
    }
    if (activeStyles.isEmpty) EmptyAttr
    else styles(activeStyles*)
  }
}
