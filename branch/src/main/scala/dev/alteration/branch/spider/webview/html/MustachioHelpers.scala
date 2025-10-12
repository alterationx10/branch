package dev.alteration.branch.spider.webview.html

import dev.alteration.branch.mustachio.{Mustachio, Stache}
import dev.alteration.branch.friday.Json

/** Helpers for integrating Mustachio templates with WebView HTML DSL.
  *
  * Provides functions to render Mustachio templates and convert them to Html
  * types for use in WebViews.
  *
  * Example:
  * {{{
  * import MustachioHelpers._
  *
  * def render(state: TodoState): String =
  *   div(
  *     mustache("""
  *       <h1>{{title}}</h1>
  *       <p>You have {{count}} items</p>
  *     """, Map(
  *       "title" -> state.title,
  *       "count" -> state.items.length
  *     )),
  *     ul(
  *       state.items.map(item => li(item.text))
  *     )
  *   ).render
  * }}}
  */
object MustachioHelpers {

  /** Render a Mustachio template with the given context.
    *
    * The result is wrapped in Html.Raw to preserve the HTML structure. Note:
    * This does NOT escape HTML in the template, so be careful with
    * user-generated content. Use {{escaped}} syntax in templates for safety.
    *
    * @param template
    *   The Mustachio template string
    * @param context
    *   The context map (will be converted to Stache)
    * @param partials
    *   Optional partials map (will be converted to Stache)
    * @return
    *   An Html.Raw node containing the rendered template
    */
  def mustache(
      template: String,
      context: Map[String, Any] = Map.empty,
      partials: Map[String, String] = Map.empty
  ): Html = {
    val stacheContext  = contextToStache(context)
    val stachePartials = partialsToStache(partials)

    val rendered = Mustachio.render(
      template,
      stacheContext,
      Some(stachePartials)
    )

    Html.Raw(rendered)
  }

  /** Render a Mustachio template from a file.
    *
    * The template file should be in the resources directory.
    *
    * @param templatePath
    *   The path to the template file (e.g., "templates/todo.mustache")
    * @param context
    *   The context map (will be converted to Stache)
    * @param partials
    *   Optional partials map (will be converted to Stache)
    * @return
    *   An Html.Raw node containing the rendered template
    */
  def mustacheFile(
      templatePath: String,
      context: Map[String, Any] = Map.empty,
      partials: Map[String, String] = Map.empty
  ): Html = {
    val template = loadTemplate(templatePath)
    mustache(template, context, partials)
  }

  /** Convert a context map to a Stache object.
    *
    * This handles common Scala types and converts them to Stache
    * representation:
    *   - String -> Stache.Str
    *   - Int/Long/Double -> Stache.Str (formatted as string)
    *   - Boolean -> Stache.Str ("true"/"false")
    *   - List/Seq -> Stache.Arr
    *   - Map -> Stache.Obj
    *   - None/null -> Stache.Null
    *   - Some(value) -> converted value
    *
    * @param context
    *   The context map
    * @return
    *   A Stache.Obj representing the context
    */
  def contextToStache(context: Map[String, Any]): Stache = {
    Stache.Obj(context.map { case (key, value) =>
      key -> toStache(value)
    })
  }

  /** Convert a value to Stache representation. */
  private def toStache(value: Any): Stache = value match {
    case null           => Stache.Null
    case None           => Stache.Null
    case Some(v)        => toStache(v)
    case s: String      => Stache.Str(s)
    case i: Int         => Stache.Str(i.toString)
    case l: Long        => Stache.Str(l.toString)
    case d: Double      => Stache.Str(d.toString)
    case f: Float       => Stache.Str(f.toString)
    case b: Boolean     => Stache.Str(b.toString)
    case list: List[?]  => Stache.Arr(list.map(toStache))
    case seq: Seq[?]    => Stache.Arr(seq.toList.map(toStache))
    case map: Map[?, ?] =>
      val stringMap = map.map { case (k, v) =>
        k.toString -> toStache(v)
      }
      Stache.Obj(stringMap)
    case json: Json     => Stache.fromJson(json)
    case other          => Stache.Str(other.toString)
  }

  /** Convert a partials map to Stache representation. */
  private def partialsToStache(partials: Map[String, String]): Stache = {
    Stache.Obj(partials.map { case (key, template) =>
      key -> Stache.Str(template)
    })
  }

  /** Load a template from resources.
    *
    * @param path
    *   The path to the template file
    * @return
    *   The template content as a string
    */
  private def loadTemplate(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    if (stream == null) {
      throw new RuntimeException(s"Template not found: $path")
    }
    try {
      scala.io.Source.fromInputStream(stream).mkString
    } finally {
      stream.close()
    }
  }

  /** Extension methods for easier Mustachio integration. */
  extension (template: String) {

    /** Render this string as a Mustachio template.
      *
      * @param context
      *   The context map
      * @param partials
      *   Optional partials
      * @return
      *   Rendered HTML
      */
    def renderMustache(
        context: Map[String, Any] = Map.empty,
        partials: Map[String, String] = Map.empty
    ): Html = {
      mustache(template, context, partials)
    }
  }
}
