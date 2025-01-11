package dev.wishingtree.branch.mustachio

import scala.annotation.tailrec

object Mustachio {

  def render(
      template: String,
      context: Stache,
      sections: List[String] = List.empty,
      sectionContexts: List[Stache] = List.empty
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

    // given a.b.c should give a list of a.b.c, a.b, a
    def fieldStack(fieldStr: String): List[String] =
      fieldStr
        .split("\\.")
        .foldLeft(List.empty[String]) { (acc, field) =>
          val next =
            (acc.lastOption.getOrElse("") + "." + field).stripPrefix(".")
          acc :+ next
        }
        .reverse

    // Searched up the stack for the first context that
    // we can look in.
    def findContext(fieldStr: String): Stache = {

      fieldStack(sections.mkString("."))
        .map(section => context ? section)
        .find { c =>
          fieldStack(fieldStr)
            .map(s => c ? s)
            .exists(_.nonEmpty)
        }
        .flatten
        .getOrElse(context)

    }

    def replace(
        fieldStr: String,
        escape: Boolean
    ): String = {

      val _field =
        if fieldStr == "." then sections.lastOption.getOrElse(".")
        else fieldStr

      val subContext =
        sectionContexts.find(c => (c ? _field).nonEmpty)

      // independent sections
      val otherContext =
        sections
          .map(context ? _)
          .find(c => (c ? _field).nonEmpty)
          .flatten

      val lastSectionOfLastContext =
        sections.lastOption.flatMap { section =>
          sectionContexts.headOption.flatMap { ctx =>
            ctx ? section
          }
        }

      // This has gotten out of control
      (findContext(_field) ? _field)   // hierarchical sections
        .orElse(subContext ? _field)   // visited sections
        .orElse(otherContext ? _field) // independent sections
        .orElse(
          lastSectionOfLastContext ? _field
        )                              // last section of last context
        .map(_.strVal)
        .map(str => if escape then htmlEscape(str) else str)
        .getOrElse {
          println(s"sections: $sections")
          println(s"Could not find $fieldStr in context")
          lastSectionOfLastContext.foreach(_.prettyPrint())
          sectionContexts.foreach(_.prettyPrint())
          ""
        }
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

              val maybeNewline =
                strIter
                  .nextOption()
                  .filterNot(_ == '\n')
                  .map(_.toString)
                  .getOrElse("")

              replaceBuilder.append(maybeNewline)

              while !replaceBuilder.endsWith(s"{{/$section}}") do
                replaceBuilder.append(strIter.next())

              // TODO Need to do arbitrary nesting...
              if replaceBuilder.mkString.contains(s"{{#$section}}") then {
                replaceBuilder.append(strIter.next())
                while !replaceBuilder.endsWith(s"{{/$section}}") do
                  replaceBuilder.append(strIter.next())
              }

              val maybeNewLineAgain =
                strIter
                  .nextOption()
                  .filterNot(_ == '\n')
                  .map(_.toString)
                  .getOrElse("")

              context ? section match {
                case Some(Stache.Str("false")) => sb.append(maybeNewLineAgain)
                case Some(Stache.Null)         => sb.append(maybeNewLineAgain)
                case Some(Stache.Arr(arr))     =>
                  arr.foreach { item =>
                    sb.append(
                      render(
                        replaceBuilder.dropRight(5 + section.length).mkString,
                        item,
                        sections :+ section,
                        context +: sectionContexts
                      )
                    )
                  }
                  sb.append(maybeNewLineAgain)
                case Some(ctx)                 =>
                  sb.append(
                    render(
                      replaceBuilder.dropRight(5 + section.length).mkString,
                      context,
                      sections :+ section,
                      ctx +: sectionContexts
                    ) + maybeNewLineAgain
                  )
                case None                      =>
                  sb.append(
                    render(
                      replaceBuilder.dropRight(5 + section.length).mkString,
                      context,
                      sections :+ section,
                      // The trick is to look in the last context, but this is a hack for now
                      (sectionContexts.head ? section).get +: sectionContexts
                    ) + maybeNewLineAgain
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
