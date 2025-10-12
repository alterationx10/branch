package dev.alteration.branch.spider.webview.html

/** Branch HTML DSL - Type-safe HTML construction for WebViews.
  *
  * This package provides a Scalatags-inspired DSL for building HTML in a
  * type-safe, composable way with automatic XSS protection.
  *
  * ==Quick Start==
  *
  * {{{
  * import dev.alteration.branch.spider.webview.html._
  *
  * def render(count: Int): Html =
  *   div(cls := "container")(
  *     h1("Counter"),
  *     p(s"Count: $count"),
  *     button(wvClick := "increment")("+"),
  *     button(wvClick := "decrement")("-")
  *   )
  *
  * val htmlString = render(42).render
  * }}}
  *
  * ==Key Features==
  *
  *   - '''Type Safety''': Catch HTML errors at compile time
  *   - '''Automatic Escaping''': XSS protection built-in
  *   - '''Composability''': Build complex UIs from simple parts
  *   - '''WebView Integration''': First-class support for wv-* attributes
  *   - '''Zero Overhead''': Compiles to efficient string operations
  *
  * ==Main Types==
  *
  *   - [[Html]] - Core HTML ADT (Element, Text, Raw, Empty)
  *   - [[Attr]] - HTML attributes with automatic escaping
  *   - [[Tags]] - Element constructors (div, button, input, etc.)
  *   - [[Attributes]] - Attribute builders (cls, id, style, etc.)
  *   - [[WebViewAttributes]] - WebView event attributes (wvClick, wvChange,
  *     etc.)
  *
  * ==Examples==
  *
  * '''Basic Elements:'''
  * {{{
  * div(cls := "container")(
  *   h1("Hello, World!"),
  *   p("This is ", strong("important"), " text")
  * )
  * }}}
  *
  * '''Forms:'''
  * {{{
  * form(wvSubmit := "save")(
  *   input(tpe := "text", wvChange := "update-name", value := name),
  *   button(tpe := "submit")("Save")
  * )
  * }}}
  *
  * '''Conditional Rendering:'''
  * {{{
  * div(
  *   when(showHeader) { h1("Welcome!") },
  *   when(isLoading) { p("Loading...") },
  *   cond(hasItems)(
  *     ul(items.map(item => li(item.name)))
  *   )(
  *     p("No items found")
  *   )
  * )
  * }}}
  *
  * '''Lists:'''
  * {{{
  * ul(cls := "todo-list")(
  *   todos.map { todo =>
  *     li(key := todo.id)(
  *       span(todo.text),
  *       button(wvClick := "delete", wvTarget := todo.id)("×")
  *     )
  *   }
  * )
  * }}}
  *
  * '''Dynamic Classes & Styles:'''
  * {{{
  * div(
  *   classWhen(
  *     "active" -> isActive,
  *     "disabled" -> isDisabled
  *   ),
  *   styleWhen(
  *     ("color", "red", isError),
  *     ("display", "none", isHidden)
  *   )
  * )
  * }}}
  */

// Re-export all public APIs for convenient access

// Core types (already visible from imports)
// Html and Attr traits are accessible via:
// import dev.alteration.branch.spider.webview.html.{Html, Attr}

// Export Html constructors
export Html.{Element, Empty, Fragment, Raw, Text}

// Export Attr constructors
export Attr.{BooleanAttr, EmptyAttr, StringAttr}

// Element constructors
export Tags._

// Standard attributes
export Attributes._

// WebView-specific attributes
export WebViewAttributes._

// Component helpers
export Components._

// === Additional Conveniences ===

/** Convert a list of Html elements to a single Html element.
  *
  * Useful for flattening nested structures.
  *
  * {{{
  * fragment(
  *   h1("Title"),
  *   p("Paragraph 1"),
  *   p("Paragraph 2")
  * )
  * }}}
  */
def fragment(children: Html*): Html =
  Html.Element("div", Nil, children.toList)

/** Render multiple elements without a wrapper.
  *
  * Creates a "virtual" container that doesn't render its own tag.
  */
def fragments(children: Html*): Html =
  Html.Fragment(children.toList)

/** Create an HTML key attribute (for list optimization).
  *
  * While not used by the current implementation, this reserves the attribute
  * for future morphing/diffing optimizations.
  */
val key = Attributes.AttrBuilder("data-key")

/** Create an HTML element from raw HTML string.
  *
  * @param htmlString
  *   Raw HTML (not escaped)
  */
def unsafeHtml(htmlString: String): Html = Html.Raw(htmlString)

/** Escape HTML string manually (usually automatic).
  *
  * @param text
  *   Text to escape
  */
def escape(text: String): String =
  text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

// === Helper Extensions ===

extension (html: Html) {

  /** Render this HTML to a string. */
  def asString: String = html.render

  /** Wrap this HTML in a container element.
    *
    * {{{
    * p("Hello").wrapIn("div", cls := "wrapper")
    * // Produces: <div class="wrapper"><p>Hello</p></div>
    * }}}
    */
  def wrapIn(tag: String, attrs: Attr*): Html =
    Html.Element(tag, attrs.toList, List(html))
}

extension (attrs: List[Attr]) {

  /** Add an attribute to a list of attributes. */
  def +(attr: Attr): List[Attr] = attrs :+ attr

  /** Add multiple attributes to a list. */
  def ++(newAttrs: List[Attr]): List[Attr] = attrs ++ newAttrs
}

extension (htmlList: List[Html]) {

  /** Join HTML elements with a separator.
    *
    * {{{
    * List(span("A"), span("B"), span("C")).joinHtml(text(" | "))
    * // Produces: <span>A</span> | <span>B</span> | <span>C</span>
    * }}}
    */
  def joinHtml(separator: Html): Html =
    if htmlList.isEmpty then Html.Empty
    else {
      val result = htmlList.flatMap(h => List(h, separator)).dropRight(1)
      fragments(result*)
    }
}
