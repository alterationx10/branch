package dev.alteration.branch.spider.webview.html

/** Core ADT for representing HTML in a type-safe way.
  *
  * This forms the foundation of Branch's HTML DSL, providing:
  *   - Type-safe HTML construction
  *   - Automatic escaping
  *   - Composable elements
  *   - Zero runtime overhead (compiles to strings)
  *
  * Example:
  * {{{
  * div(cls := "container")(
  *   h1("Hello, World!"),
  *   p("This is type-safe HTML")
  * )
  * }}}
  */
sealed trait Html {

  /** Render this HTML to a string.
    *
    * This is the final output that gets sent to the browser.
    */
  def render: String
}

object Html {

  /** Create an HTML element with attributes and children.
    *
    * @param tag
    *   The HTML tag name (e.g., "div", "span", "button")
    * @param attrs
    *   Attributes for this element
    * @param children
    *   Child elements
    */
  case class Element(
      tag: String,
      attrs: List[Attr],
      children: List[Html]
  ) extends Html {

    def render: String = {
      val attrString =
        if (attrs.isEmpty) ""
        else " " + attrs.map(_.render).mkString(" ")

      val childrenString = children.map(_.render).mkString

      // Self-closing tags
      if (isSelfClosing(tag) && children.isEmpty) {
        s"<$tag$attrString />"
      } else {
        s"<$tag$attrString>$childrenString</$tag>"
      }
    }

    private def isSelfClosing(tag: String): Boolean = {
      Set("br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "param", "source", "track", "wbr")
        .contains(tag)
    }
  }

  /** Text content (automatically escaped).
    *
    * @param value
    *   The text to display
    */
  case class Text(value: String) extends Html {
    def render: String = escapeHtml(value)
  }

  /** Raw HTML (NOT escaped - use with caution!).
    *
    * Only use this when you absolutely need to inject raw HTML. Prefer Text for
    * user-generated content.
    *
    * @param html
    *   The raw HTML string
    */
  case class Raw(html: String) extends Html {
    def render: String = html
  }

  /** Empty HTML (renders to nothing). */
  case object Empty extends Html {
    def render: String = ""
  }

  /** Fragment of multiple HTML elements without a wrapper.
    *
    * Useful for grouping elements without adding an extra container element.
    *
    * @param children
    *   The child elements
    */
  case class Fragment(children: List[Html]) extends Html {
    def render: String = children.map(_.render).mkString
  }

  /** Escape HTML special characters to prevent XSS attacks.
    *
    * This is automatically applied to all Text nodes.
    */
  private def escapeHtml(text: String): String = {
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }
}

/** HTML attribute.
  *
  * Attributes are key-value pairs that get rendered in HTML tags.
  */
sealed trait Attr {

  /** Render this attribute to a string (e.g., "class=\"foo\""). */
  def render: String
}

object Attr {

  /** Standard HTML attribute with a string value.
    *
    * @param key
    *   The attribute name (e.g., "class", "id", "href")
    * @param value
    *   The attribute value
    */
  case class StringAttr(key: String, value: String) extends Attr {
    def render: String = s"""$key="${escapeAttrValue(value)}""""

    private def escapeAttrValue(value: String): String = {
      value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
    }
  }

  /** Boolean HTML attribute (e.g., checked, disabled, readonly).
    *
    * @param key
    *   The attribute name
    * @param value
    *   Whether the attribute is present
    */
  case class BooleanAttr(key: String, value: Boolean) extends Attr {
    def render: String = {
      if (value) key
      else ""
    }
  }

  /** Empty attribute (renders nothing). Used for conditional attributes. */
  case object EmptyAttr extends Attr {
    def render: String = ""
  }
}
