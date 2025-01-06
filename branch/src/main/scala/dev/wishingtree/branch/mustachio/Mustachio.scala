package dev.wishingtree.branch.mustachio

import scala.annotation.{tailrec, targetName}



object Mustachio {

  def render(template: String, context: Stache): String = {

    var counter          = 0
    val templateIterator = template.iterator

    val sb: StringBuilder = new StringBuilder()
    val replaceBuilder    = new StringBuilder()

    def replace(fieldStr: String, stache: Stache): String = {
      fieldStr.trim
        .split("\\.")
        .foldLeft(Option(stache))((s, field) => s ? field)
        .map(_.strVal)
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
          val _replace = replaceBuilder.dropRight(2)
          sb.append(replace(_replace.mkString, context))
          replaceBuilder.clear()
        }

        loop(strIter)
      }

    loop(templateIterator)
  }

}
