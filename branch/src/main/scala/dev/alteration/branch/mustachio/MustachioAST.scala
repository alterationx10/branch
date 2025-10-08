package dev.alteration.branch.mustachio

/** AST representation of a mustache template
  *
  * This represents the parsed structure of a mustache template, separating
  * parsing concerns from rendering.
  */
private[mustachio] enum MustachioAST {

  /** Literal text content */
  case Text(content: String)

  /** Variable interpolation {{name}} - HTML escaped */
  case Variable(name: String)

  /** Unescaped variable {{{name}}} or {{&name}} */
  case UnescapedVariable(name: String)

  /** Section {{#name}}...{{/name}} */
  case Section(name: String, content: List[MustachioAST])

  /** Inverted section {{^name}}...{{/name}} */
  case InvertedSection(name: String, content: List[MustachioAST])

  /** Comment {{!comment}} - removed during rendering */
  case Comment(content: String)

  /** Partial {{>name}} with indentation info for standalone handling */
  case Partial(name: String, indentation: String)

  /** Sequence of elements */
  case Sequence(elements: List[MustachioAST])
}

private[mustachio] object MustachioAST {

  /** Flatten nested sequences for cleaner AST */
  def flatten(ast: MustachioAST): List[MustachioAST] = ast match {
    case Sequence(elements)             => elements.flatMap(flatten)
    case Section(name, content)         => List(Section(name, content.flatMap(flatten)))
    case InvertedSection(name, content) =>
      List(InvertedSection(name, content.flatMap(flatten)))
    case other                          => List(other)
  }

}
