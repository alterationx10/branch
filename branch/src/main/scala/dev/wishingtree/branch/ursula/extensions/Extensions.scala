package dev.wishingtree.branch.ursula.extensions

import scala.annotation.targetName

private[ursula] object Extensions {

  extension (s: String) {
    def split: Seq[String] = s.split(" ").toSeq
    def indented: String   =
      s.split(System.lineSeparator())
        .map(s => "\t" + s)
        .mkString(System.lineSeparator())
  }

  extension (sb: StringBuilder) {
    def newLine: StringBuilder =
      sb.append(System.lineSeparator())

    def appendLine(str: String): StringBuilder =
      sb.append(str + System.lineSeparator())
  }

  extension [A](c: Seq[A]) {
    @targetName("nonEmptyOrElseDefault")
    def :~(a: A): Seq[A]         = if c.isEmpty then Seq(a) else c
    @targetName("nonEmptyOrElseDefault")
    def :~(a: Option[A]): Seq[A] = if c.isEmpty then Seq(a).flatten else c
    @targetName("nonEmptyOrElseDefault")
    def :~(a: Seq[A]): Seq[A]    = if c.isEmpty then a else c

    def oneOfOrThrow[E <: Throwable](options: Set[A], err: E): Seq[A] = {
      if !c.foldLeft(true)((b, e) =>
          b && (options.isEmpty || options.contains(e))
        )
      then throw err
      else c
    }
  }

  extension [A](o: Option[A]) {
    @targetName("nonEmptyOrElseDefault")
    def :~(a: A): Option[A]         = o.orElse(Option(a))
    @targetName("nonEmptyOrElseDefault")
    def :~(a: Option[A]): Option[A] = o.orElse(a)

    def oneOfOrThrow[E <: Throwable](options: Set[A], err: E) =
      if (o.isDefined && options.nonEmpty) {
        val filtered = o.filter(e => options.contains(e))
        if (filtered.isEmpty) throw err else filtered
      } else o
  }
}
