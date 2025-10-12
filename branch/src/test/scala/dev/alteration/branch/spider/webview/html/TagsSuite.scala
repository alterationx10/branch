package dev.alteration.branch.spider.webview.html

import munit.FunSuite
import Tags._
import Attributes._

/** Tests for the Tags helper functions.
  *
  * Tests various HTML element creation helpers and utilities.
  */
class TagsSuite extends FunSuite {

  test("div creates a div element") {
    val result = div()(text("Hello"))
    assertEquals(result.render, "<div>Hello</div>")
  }

  test("div with attributes") {
    val result = div(cls := "container", id := "main")(text("Content"))
    assertEquals(
      result.render,
      """<div class="container" id="main">Content</div>"""
    )
  }

  test("span creates a span element") {
    val result = span()(text("Text"))
    assertEquals(result.render, "<span>Text</span>")
  }

  test("nested elements") {
    val result = div()(
      span()(text("First")),
      span()(text("Second"))
    )
    assertEquals(
      result.render,
      "<div><span>First</span><span>Second</span></div>"
    )
  }

  test("heading tags h1 through h6") {
    assertEquals(h1()(text("Title")).render, "<h1>Title</h1>")
    assertEquals(h2()(text("Title")).render, "<h2>Title</h2>")
    assertEquals(h3()(text("Title")).render, "<h3>Title</h3>")
    assertEquals(h4()(text("Title")).render, "<h4>Title</h4>")
    assertEquals(h5()(text("Title")).render, "<h5>Title</h5>")
    assertEquals(h6()(text("Title")).render, "<h6>Title</h6>")
  }

  test("paragraph tag") {
    val result = p()(text("Paragraph text"))
    assertEquals(result.render, "<p>Paragraph text</p>")
  }

  test("strong and em tags") {
    val result = p()(
      text("This is "),
      strong()(text("bold")),
      text(" and "),
      em()(text("italic"))
    )
    assertEquals(
      result.render,
      "<p>This is <strong>bold</strong> and <em>italic</em></p>"
    )
  }

  test("code and pre tags") {
    val result = pre()(code()(text("val x = 42")))
    assertEquals(result.render, "<pre><code>val x = 42</code></pre>")
  }

  test("br tag (self-closing)") {
    val result = br()
    assertEquals(result.render, "<br />")
  }

  test("hr tag (self-closing)") {
    val result = hr()
    assertEquals(result.render, "<hr />")
  }

  test("list tags - ul and li") {
    val result = ul()(
      li()(text("Item 1")),
      li()(text("Item 2")),
      li()(text("Item 3"))
    )
    assertEquals(
      result.render,
      "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>"
    )
  }

  test("list tags - ol and li") {
    val result = ol()(
      li()(text("First")),
      li()(text("Second"))
    )
    assertEquals(result.render, "<ol><li>First</li><li>Second</li></ol>")
  }

  test("definition list - dl, dt, dd") {
    val result = dl()(
      dt()(text("Term")),
      dd()(text("Definition"))
    )
    assertEquals(result.render, "<dl><dt>Term</dt><dd>Definition</dd></dl>")
  }

  test("form and input tags") {
    val result = form()(
      input(tpe := "text", name     := "username"),
      input(tpe := "password", name := "password")
    )
    assertEquals(
      result.render,
      """<form><input type="text" name="username" /><input type="password" name="password" /></form>"""
    )
  }

  test("button tag") {
    val result = button(tpe := "submit")(text("Submit"))
    assertEquals(result.render, """<button type="submit">Submit</button>""")
  }

  test("label tag") {
    val result = label(attr("for") := "email")(text("Email:"))
    assertEquals(result.render, """<label for="email">Email:</label>""")
  }

  test("textarea tag") {
    val result = textarea(name := "message")(text("Default text"))
    assertEquals(
      result.render,
      """<textarea name="message">Default text</textarea>"""
    )
  }

  test("select and option tags") {
    val result = select(name := "color")(
      option(value := "red")(text("Red")),
      option(value := "blue", selected := true)(text("Blue"))
    )
    assertEquals(
      result.render,
      """<select name="color"><option value="red">Red</option><option value="blue" selected>Blue</option></select>"""
    )
  }

  test("table tags") {
    val result = table()(
      thead()(
        tr()(
          th()(text("Name")),
          th()(text("Age"))
        )
      ),
      tbody()(
        tr()(
          td()(text("Alice")),
          td()(text("30"))
        ),
        tr()(
          td()(text("Bob")),
          td()(text("25"))
        )
      )
    )
    assertEquals(
      result.render,
      "<table><thead><tr><th>Name</th><th>Age</th></tr></thead><tbody><tr><td>Alice</td><td>30</td></tr><tr><td>Bob</td><td>25</td></tr></tbody></table>"
    )
  }

  test("anchor tag") {
    val result = a(href := "https://example.com")(text("Link"))
    assertEquals(result.render, """<a href="https://example.com">Link</a>""")
  }

  test("img tag (self-closing)") {
    val result = img(src := "photo.jpg", alt := "A photo")
    assertEquals(result.render, """<img src="photo.jpg" alt="A photo" />""")
  }

  test("semantic elements") {
    assertEquals(
      section()(text("Content")).render,
      "<section>Content</section>"
    )
    assertEquals(
      article()(text("Article")).render,
      "<article>Article</article>"
    )
    assertEquals(header()(text("Header")).render, "<header>Header</header>")
    assertEquals(footer()(text("Footer")).render, "<footer>Footer</footer>")
    assertEquals(main()(text("Main")).render, "<main>Main</main>")
    assertEquals(aside()(text("Aside")).render, "<aside>Aside</aside>")
    assertEquals(nav()(text("Nav")).render, "<nav>Nav</nav>")
  }

  test("text helper creates Text node") {
    val result = text("Hello, World!")
    assertEquals(result.render, "Hello, World!")
  }

  test("text helper with Any converts to string") {
    val result = text(42)
    assertEquals(result.render, "42")
  }

  test("text helper escapes HTML") {
    val result = text("<script>")
    assertEquals(result.render, "&lt;script&gt;")
  }

  test("raw helper creates Raw HTML") {
    val result = raw("<strong>Bold</strong>")
    assertEquals(result.render, "<strong>Bold</strong>")
  }

  test("empty helper creates Empty node") {
    val result = empty
    assertEquals(result.render, "")
  }

  test("when helper - condition true") {
    val result = when(true)(div()(text("Visible")))
    assertEquals(result.render, "<div>Visible</div>")
  }

  test("when helper - condition false") {
    val result = when(false)(div()(text("Hidden")))
    assertEquals(result.render, "")
  }

  test("cond helper - condition true") {
    val result = cond(true)(text("Yes"))(text("No"))
    assertEquals(result.render, "Yes")
  }

  test("cond helper - condition false") {
    val result = cond(false)(text("Yes"))(text("No"))
    assertEquals(result.render, "No")
  }

  test("implicit conversion - String to Html") {
    import Tags.given
    val html: Html = "Hello"
    assertEquals(html.render, "Hello")
  }

  test("implicit conversion - Int to Html") {
    import Tags.given
    val html: Html = 42
    assertEquals(html.render, "42")
  }

  test("implicit conversion - Boolean to Html") {
    import Tags.given
    val html: Html = true
    assertEquals(html.render, "true")
  }

  test("complex nested structure") {
    val result = div(cls := "app")(
      header()(
        h1()(text("My App")),
        nav()(
          a(href := "/")(text("Home")),
          a(href := "/about")(text("About"))
        )
      ),
      main()(
        article()(
          h2()(text("Article Title")),
          p()(text("Article content here"))
        )
      ),
      footer()(
        text("© 2025")
      )
    )

    assert(result.render.contains("My App"))
    assert(result.render.contains("Article Title"))
    assert(result.render.contains("© 2025"))
  }

  test("fieldset and legend") {
    val result = fieldset()(
      legend()(text("Personal Info")),
      label()(text("Name")),
      input(tpe := "text")
    )
    assertEquals(
      result.render,
      """<fieldset><legend>Personal Info</legend><label>Name</label><input type="text" /></fieldset>"""
    )
  }

  test("figure and figcaption") {
    val result = figure()(
      img(src := "image.jpg"),
      figcaption()(text("Image caption"))
    )
    assertEquals(
      result.render,
      """<figure><img src="image.jpg" /><figcaption>Image caption</figcaption></figure>"""
    )
  }

  test("blockquote") {
    val result = blockquote()(text("Quote text"))
    assertEquals(result.render, "<blockquote>Quote text</blockquote>")
  }

  test("video and source tags") {
    val result = video(controls := true)(
      source(src := "video.mp4", tpe := "video/mp4")
    )
    assertEquals(
      result.render,
      """<video controls><source src="video.mp4" type="video/mp4" /></video>"""
    )
  }
}
