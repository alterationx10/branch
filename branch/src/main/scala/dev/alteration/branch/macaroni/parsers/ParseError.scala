package dev.alteration.branch.macaroni.parsers

case class ParseError(stack: List[(Location, String)] = List.empty) {
  def push(loc: Location, msg: String): ParseError =
    copy(stack = (loc, msg) :: stack)

  def label(s: String): ParseError =
    ParseError(latestLoc.map((_, s)).toList)

  def latest: Option[(Location, String)] =
    stack.lastOption

  def latestLoc: Option[Location] =
    latest map (_._1)

  override def toString =
    if stack.isEmpty then "no error message"
    else {
      val collapsed = collapseStack(stack)
      val context   =
        collapsed.lastOption.map("\n\n" + _._1.currentLine).getOrElse("") +
          collapsed.lastOption.map("\n" + _._1.columnCaret).getOrElse("")
      collapsed
        .map((loc, msg) => s"${formatLoc(loc)} $msg")
        .mkString("\n") + context
    }

  def collapseStack(s: List[(Location, String)]): List[(Location, String)] =
    s.groupBy(_._1)
      .view
      .mapValues(_.map(_._2).mkString("; "))
      .toList
      .sortBy(_._1.offset)

  def formatLoc(l: Location): String = s"${l.line}.${l.col}"
}
