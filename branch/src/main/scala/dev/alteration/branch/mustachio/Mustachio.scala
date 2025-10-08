package dev.alteration.branch.mustachio

object Mustachio {

  private[mustachio] def htmlEscape(str: String): String =
    str
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\\\"", "&quot;")
      .replace("'", "&#39;")

  private[mustachio] def parseTagAndClear(
      stringBuilder: StringBuilder,
      delimiter: Delimiter
  ): String = {
    val tag =
      stringBuilder
        .drop(1)
        .dropRight(delimiter.close.length)
        .mkString

    stringBuilder.clear()
    tag
  }

  /** Render a mustache template with the given context and partials.
    * @return
    */
  def render(
      template: String,
      context: Stache,
      partials: Option[Stache] = Option.empty[Stache]
  ): String = {
    val ast = MustachioParser.parse(template)
    MustachioRenderer.render(
      ast,
      context,
      List.empty,
      partials.getOrElse(Stache.empty)
    )
  }

}
