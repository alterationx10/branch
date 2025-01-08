package dev.wishingtree.branch.mustachio

import scala.annotation.tailrec

object Mustachio {

  def render(template: String, context: Stache): String = {

    val templateIterator = template.iterator

    val sb: StringBuilder = new StringBuilder()
    val replaceBuilder    = new StringBuilder()

    def htmlEscape(str: String): String =
      str
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\\\"", "&quot;")
        .replace("'", "&#39;")

    def replace(
        fieldStr: String,
        stache: Stache,
        escape: Boolean
    ): String = {
      fieldStr.trim
        .split("\\.")
        .foldLeft(Option(stache))((s, field) => s ? field)
        .map(_.strVal)
        .map(str => if escape then htmlEscape(str) else str)
        .getOrElse("")
    }

    @tailrec
    def loop(strIter: Iterator[Char]): String =
      if !strIter.hasNext then sb.result()
      else {
        sb.append(strIter.next())
        if sb.takeRight(2).mkString == "{{" then {
          sb.setLength(sb.length - 2)
          while replaceBuilder.takeRight(2).mkString != "}}" do
            replaceBuilder.append(strIter.next())

          replaceBuilder.headOption match {
            case Some('{') =>
              strIter.next() // This should be '}'. TODO Validate later...
              sb.append(
                replace(
                  replaceBuilder.drop(1).dropRight(2).mkString,
                  context,
                  false
                )
              )
            case Some('&') =>
              sb.append(
                replace(
                  replaceBuilder.drop(1).dropRight(2).mkString,
                  context,
                  false
                )
              )
            case Some('#') =>
              val section = replaceBuilder.drop(1).dropRight(2).mkString
              replaceBuilder.clear()
              while !replaceBuilder.endsWith(s"{{/$section}}") do
                replaceBuilder.append(strIter.next())
              sb.append(
                // the getOrElse probably is an edge case waiting to happen...
                render(
                  replaceBuilder.dropRight(5 + section.length).mkString,
                  context ? section getOrElse (Stache.Str(""))
                )
              )
            case Some('!') =>
            // Lots of `\` parsing here :-(
            // \n
            // \r\n
            // Also need to strip preceding whitespace :_(
            case Some(_)   =>
              sb.append(
                replace(replaceBuilder.dropRight(2).mkString, context, true)
              )
            case None      =>
              throw new Exception("Unexpected error parsing template")
          }

          replaceBuilder.clear()
        }

        loop(strIter)
      }

    loop(templateIterator)
  }

}
