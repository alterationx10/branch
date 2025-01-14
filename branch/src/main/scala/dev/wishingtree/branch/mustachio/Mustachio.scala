package dev.wishingtree.branch.mustachio

import dev.wishingtree.branch.mustachio.Stache.Str

import scala.annotation.tailrec

object Mustachio {

  private[mustachio] def htmlEscape(str: String): String =
    str
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\\\"", "&quot;")
      .replace("'", "&#39;")

  def render(
      template: String,
      context: Stache,
      partials: Option[Stache] = Option.empty[Stache]
  ): String =
    internalRender(
      template,
      context,
      List.empty,
      partials.getOrElse(Stache.empty),
      Delimiter.default
    )

  private[mustachio] def internalRender(
      template: String,
      context: Stache,
      sectionContexts: List[Stache],
      partials: Stache,
      renderDelimiter: Delimiter
  ): String = {

    val templateIterator = template.iterator

    val sb: StringBuilder = new StringBuilder()
    val replaceBuilder    = new StringBuilder()

    def replace(
        fieldStr: String,
        escape: Boolean
    ): String = {

      // We should only look up in the root context if no fields are found in the visited contexts.
      // E.g. If we're looking for a.b.c, then both b and a have to not exist.
      val canContext: Boolean =
        fieldStr.split("\\.").headOption.forall { f =>
          !sectionContexts.exists(c => (c ? f).isDefined)
        }

      // Get the first stache that has the field, if any.
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
          ""
        }
    }

    def handleSection(
        strIter: Iterator[Char],
        section: String,
        negated: Boolean,
        delimiter: Delimiter
    ): Unit = {
      while !replaceBuilder.endsWith(delimiter.closing(section)) do {
        replaceBuilder.append(strIter.next())
      }

      // Handle nested sections
      val delimiterSection = delimiter.section(section)
      val inversionSection = delimiter.inversion(section)
      val nestedCount      =
        replaceBuilder.mkString
          .sliding(delimiterSection.length)
          .count(_ == delimiterSection) +
          replaceBuilder.mkString
            .sliding(inversionSection.length)
            .count(_ == inversionSection)

      for (_ <- 0 until nestedCount) do {
        replaceBuilder.append(strIter.next())
        while !replaceBuilder.endsWith(delimiter.closing(section)) do {
          replaceBuilder.append(strIter.next())
        }
      }

      // handle standalone removal, etc...

      val openTagIsStandalone = {
        val preceding = sb.reverse.takeWhile(_ != '\n').mkString
        val following = replaceBuilder.takeWhile(_ != '\n').mkString
        preceding.isBlank && following.isBlank
      }

      // preceding: remove white space UP TO a newline
      // following: remove white space INCLUDING a newline
      if openTagIsStandalone then {
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
          .dropRight(delimiter.closing(section).length)
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

      // preceding: remove white space UP TO a newline
      // following: remove white space INCLUDING a newline
      if closeTagIsStandalone then {
        val preceding = replaceBuilder
          .dropRight(delimiter.closing(section).length)
          .reverse
          .takeWhile(_ != '\n')
          .mkString
        val toRemove  = preceding.length
        if toRemove > replaceBuilder.length() then replaceBuilder.clear()
        else replaceBuilder.setLength(replaceBuilder.length - toRemove)

        if appendAfterRender.mkString == "\r\n" then {
          appendAfterRender.clear()
        } else if appendAfterRender.nonEmpty then {
          appendAfterRender.deleteCharAt(0)
        }
      }

      context ? section match {
        case Some(ctx @ Stache.Str("false")) =>
          if !negated then sb.append(appendAfterRender.mkString)
          else
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                ctx +: sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
        case Some(ctx @ Stache.Str("true"))  =>
          if !negated then
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                ctx +: sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
          else sb.append(appendAfterRender.mkString)
        case Some(Stache.Null)               =>
          if !negated then sb.append(appendAfterRender.mkString)
          else
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
        case Some(Stache.Arr(arr))           =>
          if !negated && arr.nonEmpty then {
            arr.foreach { item =>
              sb.append(
                internalRender(
                  replaceBuilder
                    .dropRight(delimiter.closing(section).length)
                    .mkString,
                  item,
                  item +: context +: sectionContexts,
                  partials,
                  delimiter
                )
              )
            }
            sb.append(appendAfterRender.mkString)
          } else if !(negated ^ arr.isEmpty) then {
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                context +: sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
          } else {
            sb.append(appendAfterRender.mkString)
          }
        case Some(ctx)                       =>
          if !negated then
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                ctx +: sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
          else sb.append(appendAfterRender.mkString)
        case None                            =>
          if !negated then {
            // if the section is a name of a field on the current context,
            // we need to handle that, otherwise we just skip the section
            val isFieldOfLastContext =
              sectionContexts.headOption
                .flatMap(_ ? section)
                .isDefined
            if isFieldOfLastContext then
              sb.append(
                internalRender(
                  replaceBuilder
                    .dropRight(delimiter.closing(section).length)
                    .mkString,
                  context,
                  sectionContexts.headOption
                    .flatMap(_ ? section)
                    .get +: sectionContexts,
                  partials,
                  delimiter
                ) + appendAfterRender.mkString
              )
            else sb.append(appendAfterRender.mkString)
          } else {
            sb.append(
              internalRender(
                replaceBuilder
                  .dropRight(delimiter.closing(section).length)
                  .mkString,
                context,
                sectionContexts,
                partials,
                delimiter
              ) + appendAfterRender.mkString
            )
          }

      }

    }

    @tailrec
    def loop(strIter: Iterator[Char], delimiter: Delimiter): String = {
      var _delimiter = delimiter
      if !strIter.hasNext then {
        sb.result()
      } else {

        if delimiter.open.length > 1 then {
          sb.append(strIter.next())
        } else if delimiter.open.length == 1 &&
          !sb.takeRight(1).toString().equals(delimiter.open)
        then {
          sb.append(strIter.next())
        }

        if sb.takeRight(delimiter.open.length).mkString == delimiter.open then {
          sb.setLength(sb.length - delimiter.open.length)
          while replaceBuilder
              .takeRight(delimiter.close.length)
              .mkString != delimiter.close
          do replaceBuilder.append(strIter.next())

          replaceBuilder.headOption match {
            case Some('{') if delimiter.open.equals("{{") => {
              strIter.next() // This should be '}'. TODO Validate later...
              sb.append(
                replace(
                  replaceBuilder
                    .drop(1)
                    .dropRight(delimiter.close.length)
                    .mkString,
                  false
                )
              )
            }
            case Some('&')                                =>
              sb.append(
                replace(
                  replaceBuilder
                    .drop(1)
                    .dropRight(delimiter.close.length)
                    .mkString,
                  false
                )
              )
            case Some('#')                                =>
              val section = replaceBuilder
                .drop(1)
                .dropRight(delimiter.close.length)
                .mkString
              replaceBuilder.clear()
              handleSection(strIter, section, false, delimiter)
            case Some('!')                                =>
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
            case Some('^')                                =>
              val section = replaceBuilder
                .drop(1)
                .dropRight(delimiter.close.length)
                .mkString
              replaceBuilder.clear()
              handleSection(strIter, section, true, delimiter)

            case Some('>') =>
              val partial = replaceBuilder
                .drop(1)
                .dropRight(delimiter.close.length)
                .mkString
              replaceBuilder.clear()

              val preceding = sb.reverse.takeWhile(_ != '\n').mkString

              val appendAfterRender = new StringBuilder()
              strIter.nextOption().foreach(appendAfterRender.append)
              strIter.nextOption().foreach(appendAfterRender.append)

              val isStandalone: Boolean =
                if preceding.isBlank then
                  if appendAfterRender.mkString == "\r\n" then {
                    appendAfterRender.clear()
                    true
                  } else if appendAfterRender.headOption.contains('\n') then {
                    appendAfterRender.deleteCharAt(0)
                    true
                  } else if appendAfterRender.isEmpty then true
                  else false
                else false

              sb.append(
                internalRender(
                  partials ? partial match {
                    case Some(Stache.Str(str)) =>
                      if isStandalone then {
                        val indented = str
                          .replaceAll("\n", s"\n$preceding")
                        if indented.endsWith(s"\n$preceding") then {
                          if !delimiter.isDefault
                          then
                            Delimiter.replaceDefaultWith(
                              indented.dropRight(preceding.length),
                              delimiter
                            )
                          else indented.dropRight(preceding.length)

                        } else {
                          if !delimiter.isDefault then
                            Delimiter.replaceDefaultWith(indented, delimiter)
                          else indented
                        }
                      } else {
                        if !delimiter.isDefault then
                          Delimiter.replaceDefaultWith(str, delimiter)
                        else str
                      }
                    case _                     =>
                      ""
                  },
                  context,
                  sectionContexts,
                  partials,
                  delimiter
                ) + appendAfterRender.mkString
              )

            case Some('=') =>
              val rawDelimiter    = replaceBuilder
                .drop(1)
                .dropRight(delimiter.close.length + 1)
                .mkString
                .trim
              val openDelimiter   = rawDelimiter.takeWhile(!_.isWhitespace)
              val closeDelimiter  =
                rawDelimiter.reverse.takeWhile(!_.isWhitespace).reverse
              val parsedDelimiter = Delimiter(
                openDelimiter,
                closeDelimiter
              )

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

              _delimiter = parsedDelimiter

            case Some(_) =>
              sb.append(
                replace(
                  replaceBuilder.dropRight(delimiter.close.length).mkString,
                  true
                )
              )
            case None    =>
              throw new Exception("Unexpected error parsing template")
          }

          replaceBuilder.clear()
        }

        loop(strIter, _delimiter)
      }
    }

    loop(templateIterator, renderDelimiter)
  }

}
