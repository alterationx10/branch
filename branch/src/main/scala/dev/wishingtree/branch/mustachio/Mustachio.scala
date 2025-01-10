package dev.wishingtree.branch.mustachio

import scala.annotation.tailrec

object Mustachio {

  def render(
      template: String,
      context: Stache,
      subContext: Option[Stache] = Option.empty
  ): String = {

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
        escape: Boolean
    ): String = {
      val fields = fieldStr.trim.split("\\.")
      fields
        .foldLeft(subContext)((s, field) => s ? field)
        .orElse(
          fields.foldLeft(Option(context))((s, field) => s ? field)
        )
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
                  false
                )
              )
            case Some('&') =>
              sb.append(
                replace(
                  replaceBuilder.drop(1).dropRight(2).mkString,
                  false
                )
              )
            case Some('#') =>
              val section = replaceBuilder.drop(1).dropRight(2).mkString
              replaceBuilder.clear()
              while !replaceBuilder.endsWith(s"{{/$section}}") do
                replaceBuilder.append(strIter.next())
              context ? section match {
                case Some(Stache.Str("false")) => ()
                case Some(Stache.Str("null"))  => ()
                case Some(Stache.Arr(arr))     =>
                  arr.foreach { item =>
                    sb.append(
                      render(
                        replaceBuilder.dropRight(5 + section.length).mkString,
                        context,
                        Some(item)
                      )
                    )
                  }
                case _                         =>
                  sb.append(
                    // TODO the getOrElse probably is an edge case waiting to happen...
                    render(
                      replaceBuilder.dropRight(5 + section.length).mkString,
                      context,
                      context ? section
                    )
                  )
              }
            case Some('!') =>
              val preceding = sb.reverse.takeWhile(_ != '\n').mkString
              if preceding.isBlank then
                sb.setLength(sb.length - preceding.length)
              strIter.nextOption() match {
                case Some('\n') =>
                  if preceding.isBlank then () // Do nothing
                  else sb.append('\n')
                case Some('\r') =>
                  strIter.nextOption() match {
                    case Some('\n') => // Do nothing
                    case Some(c)    => sb.append('\r').append(c)
                    case _          => ()
                  }
                case Some(c)    => sb.append(c)
                case _          => ()
              }
            case Some(_)   =>
              sb.append(
                replace(replaceBuilder.dropRight(2).mkString, true)
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
