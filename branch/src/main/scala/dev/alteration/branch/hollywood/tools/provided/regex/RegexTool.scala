package dev.alteration.branch.hollywood.tools.provided.regex

import dev.alteration.branch.hollywood.tools.schema.Param
import dev.alteration.branch.hollywood.tools.{schema, CallableTool}

import scala.util.Try
import java.util.regex.Pattern

@schema.Tool("Extract text, find patterns, and perform regex operations")
case class RegexTool(
    @Param(
      "Operation to perform: 'match', 'find_all', 'replace', 'extract', or 'split'"
    )
    operation: String,
    @Param("Regular expression pattern") pattern: String,
    @Param("Input text to process") text: String,
    @Param("Replacement text (required for 'replace' operation)")
    replacement: Option[String] = None,
    @Param("Whether to use case-insensitive matching") caseInsensitive: Option[
      Boolean
    ] = None,
    @Param("Whether to use multiline mode (^ and $ match line boundaries)")
    multiline: Option[Boolean] = None,
    @Param("Whether to use dotall mode (. matches newlines)") dotall: Option[
      Boolean
    ] = None
) extends CallableTool[String] {

  override def execute(): Try[String] = Try {
    val flags    = buildFlags()
    val jPattern = createRegex(flags)

    operation.toLowerCase match {
      case "match"    => matchPattern(jPattern)
      case "find_all" => findAll(jPattern)
      case "replace"  => replacePattern(jPattern)
      case "extract"  => extractGroups(jPattern)
      case "split"    => splitText(jPattern)
      case _          =>
        throw new IllegalArgumentException(
          s"Unknown operation: $operation. Valid operations are: match, find_all, replace, extract, split"
        )
    }
  }

  private def buildFlags(): Int = {
    var flags = 0
    if (caseInsensitive.getOrElse(false)) flags |= Pattern.CASE_INSENSITIVE
    if (multiline.getOrElse(false)) flags |= Pattern.MULTILINE
    if (dotall.getOrElse(false)) flags |= Pattern.DOTALL
    flags
  }

  private def createRegex(flags: Int): Pattern = {
    Pattern.compile(pattern, flags)
  }

  // Check if pattern matches the entire text
  private def matchPattern(jPattern: Pattern): String = {
    val matcher = jPattern.matcher(text)
    if (matcher.matches()) "true" else "false"
  }

  // Find all occurrences of pattern in text
  private def findAll(jPattern: Pattern): String = {
    val matcher = jPattern.matcher(text)
    val matches = scala.collection.mutable.ListBuffer[String]()
    while (matcher.find()) {
      matches += matcher.group()
    }

    if (matches.isEmpty) {
      "No matches found"
    } else {
      s"Found ${matches.size} match(es):\n${matches.zipWithIndex
          .map { case (m, i) => s"${i + 1}. $m" }
          .mkString("\n")}"
    }
  }

  // Replace all occurrences of pattern with replacement text
  private def replacePattern(jPattern: Pattern): String = {
    replacement match {
      case Some(repl) =>
        val matcher = jPattern.matcher(text)
        val count   = {
          var c = 0
          while (matcher.find()) c += 1
          c
        }
        val result  = jPattern.matcher(text).replaceAll(repl)
        s"Replaced $count occurrence(s):\n$result"
      case None       =>
        throw new IllegalArgumentException(
          "Replacement parameter is required for replace operation"
        )
    }
  }

  // Extract named or numbered groups from matches
  private def extractGroups(jPattern: Pattern): String = {
    val matcher = jPattern.matcher(text)
    val matches = scala.collection.mutable.ListBuffer[List[String]]()

    while (matcher.find()) {
      val groups = (0 to matcher.groupCount()).map { i =>
        val groupValue = Option(matcher.group(i)).getOrElse("")
        s"Group $i: $groupValue"
      }.toList
      matches += groups
    }

    if (matches.isEmpty) {
      "No matches found"
    } else {
      val results = matches.zipWithIndex.map { case (groups, idx) =>
        s"Match ${idx + 1}: [${groups.mkString(", ")}]"
      }
      s"Found ${matches.size} match(es) with groups:\n${results.mkString("\n")}"
    }
  }

  // Split text by pattern
  private def splitText(jPattern: Pattern): String = {
    val parts = jPattern.split(text)

    if (parts.length == 1 && parts(0) == text) {
      "No split occurred (pattern not found)"
    } else {
      s"Split into ${parts.length} part(s):\n${parts.zipWithIndex
          .map { case (p, i) => s"${i + 1}. $p" }
          .mkString("\n")}"
    }
  }
}
