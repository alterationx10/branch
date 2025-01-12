package dev.wishingtree.branch.mustachio

import scala.annotation.tailrec

object Mustachio {

  def render(
      template: String,
      context: Stache,
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

    def replace(
        fieldStr: String,
        escape: Boolean
    ): String = {

      // only look up in context
      // if NO fields are found in section contexts.
      // should be able to check the head?
      val canContext: Boolean =
        fieldStr.split("\\.").headOption.forall { f =>
          !sectionContexts.exists(c => (c ? f).isDefined)
        }

      val sectionOrContext =
        sectionContexts
          .find(c => (c ? fieldStr).nonEmpty)
          .orElse(
            Option(context).filter(_ => canContext)
          )

      (sectionOrContext ? fieldStr)
        .map(_.strVal)
        .map(str => if escape then htmlEscape(str) else str)
        .getOrElse {
          println(s"Could not find $fieldStr in context")
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

              while !replaceBuilder.endsWith(s"{{/$section}}") do
                replaceBuilder.append(strIter.next())

              // TODO Need to do arbitrary nesting...
              if replaceBuilder.mkString.contains(s"{{#$section}}") then {
                replaceBuilder.append(strIter.next())
                while !replaceBuilder.endsWith(s"{{/$section}}") do
                  replaceBuilder.append(strIter.next())
              }

              val openTagIsStandalone = {
                val preceding = sb.reverse.takeWhile(_ != '\n').mkString
                val following = replaceBuilder.takeWhile(_ != '\n').mkString
                preceding.isBlank && following.isBlank
              }
              println(s"openTagIsStandalone: $openTagIsStandalone")
              // preceding: remove white space UP TO a newline
              // following: remove white space INCLUDING a newline
              if openTagIsStandalone && sb.nonEmpty then {
                val preceding = sb.reverse.takeWhile(_ != '\n').mkString
                val toRemove  = preceding.length
                if toRemove > sb.length() then sb.clear()
                else sb.setLength(sb.length - toRemove)

                val following = replaceBuilder.takeWhile(_ != '\n').mkString
                replaceBuilder.delete(0, following.length + 1)
              }

              val appendAfterRender    = new StringBuilder()
              val closeTagIsStandalone = {
                val preceding = replaceBuilder
                  .dropRight(s"{{/$section}}".length)
                  .reverse
                  .takeWhile(_ != '\n')
                  .mkString

                // TODO what about white space?
                strIter.nextOption().foreach(appendAfterRender.append)
                strIter.nextOption().foreach(appendAfterRender.append)
                val following =
                  appendAfterRender.mkString == "\r\n" ||
                    appendAfterRender.headOption.contains('\n') ||
                    appendAfterRender.isEmpty
                preceding.isBlank && following
              }
              println(s"closeTagIsStandalone: $closeTagIsStandalone")
              // preceding: remove white space UP TO a newline
              // following: remove white space INCLUDING a newline
              if closeTagIsStandalone then {
                val preceding = replaceBuilder
                  .dropRight(s"{{/$section}}".length)
                  .reverse
                  .takeWhile(_ != '\n')
                  .mkString
                val toRemove  = preceding.length
                if toRemove > replaceBuilder.length() then
                  replaceBuilder.clear()
                else replaceBuilder.setLength(replaceBuilder.length - toRemove)

                if appendAfterRender.mkString == "\r\n" then {
                  appendAfterRender.clear()
                } else if appendAfterRender.nonEmpty then {
                  appendAfterRender.deleteCharAt(0)
                }
              }

              context ? section match {
                case Some(Stache.Str("false")) =>
                  sb.append(appendAfterRender.mkString)
                case Some(Stache.Null)         =>
                  sb.append(appendAfterRender.mkString)
                case Some(Stache.Arr(arr))     =>
                  arr.foreach { item =>
                    sb.append(
                      render(
                        replaceBuilder.dropRight(5 + section.length).mkString,
                        item,
                        item +: context +: sectionContexts
                      )
                    )
                  }
                  sb.append(appendAfterRender.mkString)
                case Some(ctx)                 =>
                  sb.append(
                    render(
                      replaceBuilder.dropRight(5 + section.length).mkString,
                      context,
                      ctx +: sectionContexts
                    ) + appendAfterRender.mkString
                  )
                case None                      =>
                  // if the section is a name of a field on the current context,
                  // we need to handle that, otherwise we just skip the section
                  val isFieldOfLastContext =
                    sectionContexts.headOption
                      .flatMap(_ ? section)
                      .isDefined
                  if isFieldOfLastContext then
                    sb.append(
                      render(
                        replaceBuilder.dropRight(5 + section.length).mkString,
                        context,
                        sectionContexts.headOption
                          .flatMap(_ ? section)
                          .get +: sectionContexts
                      ) + appendAfterRender.mkString
                    )
                  else sb.append(appendAfterRender.mkString)
              }
            case Some('!') =>
              val isInSection = sectionContexts.nonEmpty
              if !isInSection then {
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
