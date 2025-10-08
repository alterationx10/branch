package dev.alteration.branch.mustachio

private[mustachio] case class Delimiter(open: String, close: String)

private[mustachio] object Delimiter {
  val default: Delimiter = Delimiter("{{", "}}")
}
