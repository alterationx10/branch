package dev.wishingtree.branch.mustachio

import java.util.regex.Pattern

private[mustachio] case class Delimiter(open: String, close: String) {

  def closing(str: String): String =
    s"$open/$str$close"

  def section(str: String): String =
    s"$open#$str$close"

  def inversion(str: String): String =
    s"$open^$str$close"

  def partial(str: String): String =
    s"$open>$str$close"

  def comment(str: String): String =
    s"$open!$str$close"

  lazy val isDefault: Boolean =
    Delimiter.default.open.equals(open) &&
      Delimiter.default.close.equals(close)
}

private[mustachio] object Delimiter {
  val default: Delimiter = Delimiter("{{", "}}")

  def replaceDefaultWith(content: String, newDelimiter: Delimiter): String =
    content
      .replaceAll(Pattern.quote(default.open), newDelimiter.open)
      .replaceAll(Pattern.quote(default.close), newDelimiter.close)

}
