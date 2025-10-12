package dev.alteration.branch.spider.webview.html

import Html._

/** Type-safe helpers for creating HTML elements.
  *
  * This provides a fluent, Scalatags-inspired API for constructing HTML:
  *
  * {{{
  * div(cls := "container")(
  *   h1("Welcome!"),
  *   p("This is ", strong("important"), " text"),
  *   button(wvClick := "submit")("Click me")
  * )
  * }}}
  *
  * All text content is automatically escaped for XSS protection.
  */
object Tags {

  // === Container Elements ===

  def div(attrs: Attr*)(children: Html*): Html =
    Element("div", attrs.toList, children.toList)

  def span(attrs: Attr*)(children: Html*): Html =
    Element("span", attrs.toList, children.toList)

  def section(attrs: Attr*)(children: Html*): Html =
    Element("section", attrs.toList, children.toList)

  def article(attrs: Attr*)(children: Html*): Html =
    Element("article", attrs.toList, children.toList)

  def header(attrs: Attr*)(children: Html*): Html =
    Element("header", attrs.toList, children.toList)

  def footer(attrs: Attr*)(children: Html*): Html =
    Element("footer", attrs.toList, children.toList)

  def main(attrs: Attr*)(children: Html*): Html =
    Element("main", attrs.toList, children.toList)

  def aside(attrs: Attr*)(children: Html*): Html =
    Element("aside", attrs.toList, children.toList)

  def nav(attrs: Attr*)(children: Html*): Html =
    Element("nav", attrs.toList, children.toList)

  // === Text Elements ===

  def h1(attrs: Attr*)(children: Html*): Html =
    Element("h1", attrs.toList, children.toList)

  def h2(attrs: Attr*)(children: Html*): Html =
    Element("h2", attrs.toList, children.toList)

  def h3(attrs: Attr*)(children: Html*): Html =
    Element("h3", attrs.toList, children.toList)

  def h4(attrs: Attr*)(children: Html*): Html =
    Element("h4", attrs.toList, children.toList)

  def h5(attrs: Attr*)(children: Html*): Html =
    Element("h5", attrs.toList, children.toList)

  def h6(attrs: Attr*)(children: Html*): Html =
    Element("h6", attrs.toList, children.toList)

  def p(attrs: Attr*)(children: Html*): Html =
    Element("p", attrs.toList, children.toList)

  def strong(attrs: Attr*)(children: Html*): Html =
    Element("strong", attrs.toList, children.toList)

  def em(attrs: Attr*)(children: Html*): Html =
    Element("em", attrs.toList, children.toList)

  def code(attrs: Attr*)(children: Html*): Html =
    Element("code", attrs.toList, children.toList)

  def pre(attrs: Attr*)(children: Html*): Html =
    Element("pre", attrs.toList, children.toList)

  def br(attrs: Attr*): Html =
    Element("br", attrs.toList, Nil)

  def hr(attrs: Attr*): Html =
    Element("hr", attrs.toList, Nil)

  // === Lists ===

  def ul(attrs: Attr*)(children: Html*): Html =
    Element("ul", attrs.toList, children.toList)

  def ol(attrs: Attr*)(children: Html*): Html =
    Element("ol", attrs.toList, children.toList)

  def li(attrs: Attr*)(children: Html*): Html =
    Element("li", attrs.toList, children.toList)

  def dl(attrs: Attr*)(children: Html*): Html =
    Element("dl", attrs.toList, children.toList)

  def dt(attrs: Attr*)(children: Html*): Html =
    Element("dt", attrs.toList, children.toList)

  def dd(attrs: Attr*)(children: Html*): Html =
    Element("dd", attrs.toList, children.toList)

  // === Forms ===

  def form(attrs: Attr*)(children: Html*): Html =
    Element("form", attrs.toList, children.toList)

  def input(attrs: Attr*): Html =
    Element("input", attrs.toList, Nil)

  def textarea(attrs: Attr*)(children: Html*): Html =
    Element("textarea", attrs.toList, children.toList)

  def button(attrs: Attr*)(children: Html*): Html =
    Element("button", attrs.toList, children.toList)

  def label(attrs: Attr*)(children: Html*): Html =
    Element("label", attrs.toList, children.toList)

  def select(attrs: Attr*)(children: Html*): Html =
    Element("select", attrs.toList, children.toList)

  def option(attrs: Attr*)(children: Html*): Html =
    Element("option", attrs.toList, children.toList)

  def fieldset(attrs: Attr*)(children: Html*): Html =
    Element("fieldset", attrs.toList, children.toList)

  def legend(attrs: Attr*)(children: Html*): Html =
    Element("legend", attrs.toList, children.toList)

  // === Tables ===

  def table(attrs: Attr*)(children: Html*): Html =
    Element("table", attrs.toList, children.toList)

  def thead(attrs: Attr*)(children: Html*): Html =
    Element("thead", attrs.toList, children.toList)

  def tbody(attrs: Attr*)(children: Html*): Html =
    Element("tbody", attrs.toList, children.toList)

  def tfoot(attrs: Attr*)(children: Html*): Html =
    Element("tfoot", attrs.toList, children.toList)

  def tr(attrs: Attr*)(children: Html*): Html =
    Element("tr", attrs.toList, children.toList)

  def th(attrs: Attr*)(children: Html*): Html =
    Element("th", attrs.toList, children.toList)

  def td(attrs: Attr*)(children: Html*): Html =
    Element("td", attrs.toList, children.toList)

  // === Links & Media ===

  def a(attrs: Attr*)(children: Html*): Html =
    Element("a", attrs.toList, children.toList)

  def img(attrs: Attr*): Html =
    Element("img", attrs.toList, Nil)

  def video(attrs: Attr*)(children: Html*): Html =
    Element("video", attrs.toList, children.toList)

  def audio(attrs: Attr*)(children: Html*): Html =
    Element("audio", attrs.toList, children.toList)

  def source(attrs: Attr*): Html =
    Element("source", attrs.toList, Nil)

  // === Semantic Elements ===

  def figure(attrs: Attr*)(children: Html*): Html =
    Element("figure", attrs.toList, children.toList)

  def figcaption(attrs: Attr*)(children: Html*): Html =
    Element("figcaption", attrs.toList, children.toList)

  def time(attrs: Attr*)(children: Html*): Html =
    Element("time", attrs.toList, children.toList)

  def mark(attrs: Attr*)(children: Html*): Html =
    Element("mark", attrs.toList, children.toList)

  def blockquote(attrs: Attr*)(children: Html*): Html =
    Element("blockquote", attrs.toList, children.toList)

  // === Convenience Helpers ===

  /** Create a text node (automatically escaped).
    *
    * @param value
    *   The text content
    */
  def text(value: String): Html = Text(value)

  /** Create a text node from any value (calls toString).
    *
    * @param value
    *   The value to convert to text
    */
  def text(value: Any): Html = Text(value.toString)

  /** Create raw HTML (NOT escaped - use with caution!).
    *
    * @param html
    *   The raw HTML string
    */
  def raw(html: String): Html = Raw(html)

  /** Empty HTML node. */
  def empty: Html = Empty

  /** Conditionally render HTML.
    *
    * @param condition
    *   Whether to render the content
    * @param content
    *   The content to render if true
    */
  def when(condition: Boolean)(content: => Html): Html = {
    if (condition) content else Empty
  }

  /** Conditionally render HTML with an else branch.
    *
    * @param condition
    *   Whether to render the if branch
    * @param ifTrue
    *   Content to render if true
    * @param ifFalse
    *   Content to render if false
    */
  def cond(condition: Boolean)(ifTrue: => Html)(ifFalse: => Html): Html = {
    if (condition) ifTrue else ifFalse
  }

  // === Implicit Conversions ===

  /** Automatically convert strings to Text nodes. */
  given Conversion[String, Html] = Text(_)

  /** Automatically convert numbers to Text nodes. */
  given Conversion[Int, Html]     = i => Text(i.toString)
  given Conversion[Long, Html]    = l => Text(l.toString)
  given Conversion[Double, Html]  = d => Text(d.toString)
  given Conversion[Float, Html]   = f => Text(f.toString)
  given Conversion[Boolean, Html] = b => Text(b.toString)
}
