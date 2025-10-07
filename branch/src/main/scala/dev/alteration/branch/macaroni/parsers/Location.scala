package dev.alteration.branch.macaroni.parsers

case class Location(input: String, offset: Int = 0) {
  private lazy val prefix = input.substring(0, math.min(offset + 1, input.length))

  lazy val line: Int = prefix.count(_ == '\n') + 1

  lazy val col: Int = prefix.lastIndexOf('\n') match {
    case -1        => offset + 1
    case lineStart => offset - lineStart
  }

  def toError(msg: String): ParseError =
    ParseError(List((this, msg)))

  def advanceBy(n: Int) = copy(offset = offset + n)

  def remaining: String = input.substring(offset)

  def slice(n: Int) = input.substring(offset, offset + n)

  def currentLine: String =
    if input.length > 1
    then {
      val itr = input.linesIterator.drop(line - 1)
      if (itr.hasNext) itr.next() else ""
    } else ""

  def columnCaret = (" " * (col - 1)) + "^"
}
