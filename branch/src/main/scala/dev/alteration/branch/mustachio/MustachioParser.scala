package dev.alteration.branch.mustachio

import MustachioAST.*

/** Parser for mustache templates using a tokenize-then-build-AST approach
  *
  * This provides a cleaner separation between lexical analysis (tokenizing)
  * and syntax analysis (building the AST).
  */
private[mustachio] object MustachioParser {

  /** Parse a mustache template into an AST */
  def parse(template: String, delimiter: Delimiter = Delimiter.default): List[MustachioAST] = {
    val tokens = tokenize(template, delimiter)
    val ast = buildAST(tokens)
    ast
  }

  /** Token types for intermediate representation */
  private enum Token {
    case TextToken(content: String, lineInfo: LineInfo)
    case VariableToken(name: String, escape: Boolean, lineInfo: LineInfo)
    case SectionOpen(name: String, inverted: Boolean, lineInfo: LineInfo)
    case SectionClose(name: String, lineInfo: LineInfo)
    case CommentToken(content: String, lineInfo: LineInfo)
    case PartialToken(name: String, lineInfo: LineInfo)
    case DelimiterChange(open: String, close: String, lineInfo: LineInfo)

    def getLineInfo: LineInfo = this match {
      case TextToken(_, li) => li
      case VariableToken(_, _, li) => li
      case SectionOpen(_, _, li) => li
      case SectionClose(_, li) => li
      case CommentToken(_, li) => li
      case PartialToken(_, li) => li
      case DelimiterChange(_, _, li) => li
    }
  }

  /** Line information for standalone tag detection */
  private case class LineInfo(
      precedingWhitespace: String,
      followingWhitespace: String,
      hasNewlineAfter: Boolean,
      isFirstOnLine: Boolean,
      isLastOnLine: Boolean,
      isAtEOF: Boolean
  ) {
    def isStandalone: Boolean =
      isFirstOnLine && isLastOnLine && (hasNewlineAfter || isAtEOF)
  }

  import Token.*

  /** Tokenize a template into a list of tokens with line information */
  private def tokenize(template: String, startDelimiter: Delimiter): List[Token] = {
    val tokens = scala.collection.mutable.ListBuffer[Token]()

    def loop(remaining: String, delimiter: Delimiter, lineStart: Boolean): Unit = {
      if remaining.isEmpty then return

      val openIdx = remaining.indexOf(delimiter.open)

      if openIdx == -1 then {
        // No more tags
        if remaining.nonEmpty then
          tokens += TextToken(remaining, LineInfo("", "", false, lineStart, true, true))
        return
      }

      // Capture text before tag
      val precedingText = remaining.substring(0, openIdx)
      val precedingLines = precedingText.reverse.takeWhile(_ != '\n').reverse
      val hasNewlineInPreceding = precedingText.contains('\n') || precedingText.contains('\r')
      val isTagFirstOnLine = (lineStart || hasNewlineInPreceding) && precedingLines.forall(_.isWhitespace)

      // We'll add the preceding text token later, after we know if this tag is standalone
      var precedingTextToken: Option[Token] = None
      if precedingText.nonEmpty then
        precedingTextToken = Some(TextToken(precedingText, LineInfo("", "", false, lineStart, false, false)))

      // Find closing delimiter
      val afterOpen = remaining.substring(openIdx + delimiter.open.length)
      val closeIdx = afterOpen.indexOf(delimiter.close)

      if closeIdx == -1 then
        throw new Exception(s"Unclosed tag at: ${remaining.take(50)}...")

      val tagContent = afterOpen.substring(0, closeIdx)
      var afterClose = afterOpen.substring(closeIdx + delimiter.close.length)

      // Handle triple stache special case
      var skipExtraBrace = false
      if delimiter.open == "{{" && tagContent.startsWith("{") && afterClose.startsWith("}") then {
        skipExtraBrace = true
        afterClose = afterClose.substring(1)
      }

      // Analyze what follows the tag
      val followingOnLine = afterClose.takeWhile(c => c != '\n' && c != '\r')
      val isTagLastOnLine = followingOnLine.forall(_.isWhitespace)

      var hasNewlineAfter = false
      var newlineLength = 0
      if afterClose.startsWith("\r\n") then {
        hasNewlineAfter = true
        newlineLength = 2
      } else if afterClose.startsWith("\n") || afterClose.startsWith("\r") then {
        hasNewlineAfter = true
        newlineLength = 1
      }

      // Check if this tag is at EOF (nothing after the closing delimiter)
      val isAtEOF = afterClose.isEmpty

      val lineInfo = LineInfo(
        precedingLines,
        followingOnLine,
        hasNewlineAfter,
        isTagFirstOnLine,
        isTagLastOnLine,
        isAtEOF
      )

      // Helper to add preceding text, removing whitespace for standalone tags
      def addPrecedingText(isStandalone: Boolean): Unit = {
        if isStandalone && precedingLines.nonEmpty then {
          precedingTextToken match {
            case Some(TextToken(content, info)) =>
              val withoutLastLine = content.dropRight(precedingLines.length)
              if withoutLastLine.nonEmpty then
                tokens += TextToken(withoutLastLine, info)
            case Some(token) => tokens += token
            case None => ()
          }
        } else {
          precedingTextToken.foreach(tokens += _)
        }
      }

      // Parse tag content and create token
      var skipNewline = false  // For standalone tags

      if tagContent.isEmpty then {
        // Skip empty tag - add preceding text as-is
        precedingTextToken.foreach(tokens += _)
      } else {
        tagContent.head match {
          case '!' =>
            addPrecedingText(lineInfo.isStandalone)
            tokens += CommentToken(tagContent.tail, lineInfo)
            skipNewline = lineInfo.isStandalone

          case '=' =>
            val delimContent = tagContent.tail.stripSuffix("=").trim
            val parts = delimContent.split("\\s+")
            if parts.length == 2 then {
              addPrecedingText(lineInfo.isStandalone)
              // Don't add DelimiterChange token - it's handled during tokenization only
              skipNewline = lineInfo.isStandalone
              // Continue with new delimiter and skip newline if standalone
              val nextPos = if skipNewline && newlineLength > 0 then afterClose.substring(newlineLength) else afterClose
              loop(nextPos, Delimiter(parts(0), parts(1)), skipNewline || hasNewlineAfter)
              return
            } else {
              throw new Exception(s"Invalid delimiter syntax: $tagContent")
            }

          case '#' =>
            addPrecedingText(lineInfo.isStandalone)
            tokens += SectionOpen(tagContent.tail.trim, false, lineInfo)
            skipNewline = lineInfo.isStandalone

          case '^' =>
            addPrecedingText(lineInfo.isStandalone)
            tokens += SectionOpen(tagContent.tail.trim, true, lineInfo)
            skipNewline = lineInfo.isStandalone

          case '/' =>
            addPrecedingText(lineInfo.isStandalone)
            tokens += SectionClose(tagContent.tail.trim, lineInfo)
            skipNewline = lineInfo.isStandalone

          case '>' =>
            addPrecedingText(lineInfo.isStandalone)
            tokens += PartialToken(tagContent.tail.trim, lineInfo)
            skipNewline = lineInfo.isStandalone

          case '&' =>
            precedingTextToken.foreach(tokens += _)
            tokens += VariableToken(tagContent.tail.trim, false, lineInfo)

          case '{' if delimiter.open == "{{" && skipExtraBrace =>
            precedingTextToken.foreach(tokens += _)
            tokens += VariableToken(tagContent.tail.stripSuffix("}").trim, false, lineInfo)

          case _ =>
            precedingTextToken.foreach(tokens += _)
            tokens += VariableToken(tagContent.trim, true, lineInfo)
        }
      }

      // Skip newline for standalone tags
      val nextPos = if skipNewline && newlineLength > 0 then afterClose.substring(newlineLength) else afterClose
      loop(nextPos, delimiter, skipNewline || hasNewlineAfter)
    }

    loop(template, startDelimiter, true)
    tokens.toList
  }

  /** Build AST from tokens, handling nested sections and standalone tags */
  private def buildAST(tokens: List[Token]): List[MustachioAST] = {

    def processTokens(
        tokens: List[Token],
        inSection: Boolean = false
    ): (List[MustachioAST], List[Token]) = {
      val result = scala.collection.mutable.ListBuffer[MustachioAST]()
      var remaining = tokens

      while remaining.nonEmpty do {
        remaining.head match {
          case TextToken(content, _) =>
            result += Text(content)
            remaining = remaining.tail

          case VariableToken(name, escape, _) =>
            if escape then
              result += Variable(name)
            else
              result += UnescapedVariable(name)
            remaining = remaining.tail

          case SectionOpen(name, inverted, openInfo) =>
            val (sectionTokens, rest) = extractSectionTokens(name, remaining.tail)

            // Check if closing tag exists
            val closeInfo = rest.headOption.collect {
              case SectionClose(_, li) => li
            }

            // Handle standalone tags
            val (cleanedTokens, indentation) = handleStandaloneTags(
              sectionTokens,
              openInfo,
              closeInfo
            )

            val (innerAST, _) = processTokens(cleanedTokens, inSection = true)

            if inverted then
              result += InvertedSection(name, innerAST)
            else
              result += Section(name, innerAST)

            remaining = rest.tail // Skip the closing tag

          case SectionClose(_, _) =>
            // This closes the current section
            return (result.toList, remaining)

          case CommentToken(content, info) =>
            // Comments might need to be kept for standalone processing
            if !info.isStandalone then
              result += Comment(content)
            remaining = remaining.tail

          case PartialToken(name, info) =>
            val indentation = if info.isStandalone then info.precedingWhitespace else ""
            result += Partial(name, indentation)
            remaining = remaining.tail

          case DelimiterChange(_, _, _) =>
            // Delimiter changes are handled during tokenization
            remaining = remaining.tail
        }
      }

      (result.toList, List.empty)
    }

    /** Extract all tokens belonging to a section */
    def extractSectionTokens(
        name: String,
        tokens: List[Token]
    ): (List[Token], List[Token]) = {
      val result = scala.collection.mutable.ListBuffer[Token]()
      var remaining = tokens
      var depth = 0

      while remaining.nonEmpty do {
        remaining.head match {
          case SectionOpen(n, _, _) if n == name =>
            result += remaining.head
            remaining = remaining.tail
            depth += 1

          case SectionClose(n, _) if n == name =>
            if depth == 0 then
              return (result.toList, remaining)
            else {
              result += remaining.head
              remaining = remaining.tail
              depth -= 1
            }

          case _ =>
            result += remaining.head
            remaining = remaining.tail
        }
      }

      throw new Exception(s"Unclosed section: $name")
    }

    /** Handle standalone tag whitespace removal
      *
      * Note: Newlines after standalone tags are already skipped during tokenization.
      * This function only needs to handle whitespace before the closing tag.
      */
    def handleStandaloneTags(
        tokens: List[Token],
        openInfo: LineInfo,
        closeInfo: Option[LineInfo]
    ): (List[Token], String) = {
      var cleaned = tokens
      var indentation = ""

      // Store indentation from opening tag for partials
      if openInfo.isStandalone then {
        indentation = openInfo.precedingWhitespace
      }

      // Handle standalone closing tag - remove whitespace before the tag
      closeInfo match {
        case Some(info) if info.isStandalone && info.precedingWhitespace.nonEmpty =>
          cleaned = cleaned.lastOption match {
            case Some(TextToken(content, tInfo)) =>
              // Remove trailing whitespace on the last line (before closing tag)
              val trimmed = content.stripSuffix(info.precedingWhitespace)

              if trimmed.isEmpty && cleaned.length == 1 then List.empty
              else if trimmed.isEmpty then cleaned.init
              else cleaned.init :+ TextToken(trimmed, tInfo)
            case _ => cleaned
          }
        case _ => ()
      }

      (cleaned, indentation)
    }

    val (ast, _) = processTokens(tokens)
    ast
  }

}
