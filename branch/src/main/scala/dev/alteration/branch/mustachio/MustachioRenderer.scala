package dev.alteration.branch.mustachio

import MustachioAST.*

/** Renderer for Mustache AST
  *
  * Takes a parsed AST and renders it with the given context, cleanly separating
  * rendering logic from parsing.
  */
private[mustachio] object MustachioRenderer {

  /** Render an AST to a string
    *
    * @param ast
    *   The parsed AST elements
    * @param context
    *   The root data context
    * @param sectionContexts
    *   Stack of section contexts for nested lookups
    * @param partials
    *   Available partial templates
    * @return
    *   Rendered output string
    */
  def render(
      ast: List[MustachioAST],
      context: Stache,
      sectionContexts: List[Stache] = List.empty,
      partials: Stache = Stache.empty
  ): String = {
    val sb = new StringBuilder()

    ast.foreach { node =>
      sb.append(renderNode(node, context, sectionContexts, partials))
    }

    sb.result()
  }

  /** Render a single AST node */
  private def renderNode(
      node: MustachioAST,
      context: Stache,
      sectionContexts: List[Stache],
      partials: Stache
  ): String = node match {
    case Text(content) =>
      content

    case Variable(name) =>
      lookupField(name, context, sectionContexts, escape = true)

    case UnescapedVariable(name) =>
      lookupField(name, context, sectionContexts, escape = false)

    case Section(name, content) =>
      renderSection(
        name,
        content,
        inverted = false,
        context,
        sectionContexts,
        partials
      )

    case InvertedSection(name, content) =>
      renderSection(
        name,
        content,
        inverted = true,
        context,
        sectionContexts,
        partials
      )

    case Comment(_) =>
      "" // Comments are not rendered

    case Partial(name, indentation) =>
      renderPartial(name, indentation, context, sectionContexts, partials)

    case Sequence(elements) =>
      render(elements, context, sectionContexts, partials)
  }

  /** Look up a field in the context stack */
  private def lookupField(
      fieldStr: String,
      context: Stache,
      sectionContexts: List[Stache],
      escape: Boolean
  ): String = {
    // Check if we should look in root context or only section contexts
    val canContext: Boolean =
      fieldStr.split("\\.").headOption.forall { f =>
        !sectionContexts.exists(c => (c ? f).isDefined)
      }

    // Get the first stache that has the field
    val sectionOrContext =
      sectionContexts
        .find(c => (c ? fieldStr).nonEmpty)
        .orElse(
          Option(context).filter(_ => canContext)
        )

    (sectionOrContext ? fieldStr)
      .map(_.strVal)
      .map(str => if escape then htmlEscape(str) else str)
      .getOrElse("")
  }

  /** HTML escape a string */
  private def htmlEscape(str: String): String =
    str
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\\\"", "&quot;")
      .replace("'", "&#39;")

  /** Render a section (normal or inverted) */
  private def renderSection(
      name: String,
      content: List[MustachioAST],
      inverted: Boolean,
      context: Stache,
      sectionContexts: List[Stache],
      partials: Stache
  ): String = {
    context ? name match {
      case Some(ctx @ Stache.Str("false")) =>
        if !inverted then ""
        else render(content, context, ctx +: sectionContexts, partials)

      case Some(ctx @ Stache.Str("true")) =>
        if !inverted then
          render(content, context, ctx +: sectionContexts, partials)
        else ""

      case Some(Stache.Null) =>
        if !inverted then ""
        else render(content, context, sectionContexts, partials)

      case Some(Stache.Arr(arr)) =>
        if !inverted && arr.nonEmpty then {
          arr.map { item =>
            render(content, item, item +: context +: sectionContexts, partials)
          }.mkString
        } else if !(inverted ^ arr.isEmpty) then {
          render(content, context, context +: sectionContexts, partials)
        } else {
          ""
        }

      case Some(ctx) =>
        if !inverted then
          render(content, context, ctx +: sectionContexts, partials)
        else ""

      case None =>
        if !inverted then {
          // Check if it's a field of the last context
          val isFieldOfLastContext =
            sectionContexts.headOption
              .flatMap(_ ? name)
              .isDefined

          if isFieldOfLastContext then
            render(
              content,
              context,
              sectionContexts.headOption
                .flatMap(_ ? name)
                .get +: sectionContexts,
              partials
            )
          else ""
        } else {
          render(content, context, sectionContexts, partials)
        }
    }
  }

  /** Render a partial template */
  private def renderPartial(
      name: String,
      indentation: String,
      context: Stache,
      sectionContexts: List[Stache],
      partials: Stache
  ): String = {
    partials ? name match {
      case Some(Stache.Str(partialTemplate)) =>
        // Apply indentation to the template before parsing/rendering
        val indentedTemplate = if indentation.nonEmpty then {
          partialTemplate
            .split("\n", -1)
            .map(line => if line.isEmpty then line else indentation + line)
            .mkString("\n")
        } else {
          partialTemplate
        }

        // Parse and render the (possibly indented) partial
        val partialAST = MustachioParser.parse(indentedTemplate)
        render(partialAST, context, sectionContexts, partials)

      case _ =>
        "" // Partial not found
    }
  }

}
