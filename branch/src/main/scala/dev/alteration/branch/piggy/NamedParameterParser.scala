package dev.alteration.branch.piggy

import dev.alteration.branch.piggy.Sql.{PreparedStatementArg, PsArgHolder}

/** Parser for SQL with named parameters (e.g., :paramName). Converts named
  * parameters to positional JDBC placeholders (?).
  */
object NamedParameterParser {
  import dev.alteration.branch.macaroni.parsers.Reference.*

  /** Represents a parsed SQL fragment */
  private enum SqlFragment {
    case Literal(text: String)    // Plain SQL text or string literal
    case NamedParam(name: String) // Named parameter like :name
  }

  /** Parse a SQL string literal with single quotes. Handles escaped quotes
    * using SQL's doubled quote syntax (''). Matches: 'anything with ''escaped''
    * quotes'
    */
  private def singleQuotedString: Parser[SqlFragment] =
    regex("'(?:''|[^'])*'".r).map(text => SqlFragment.Literal(text))

  /** Parse a SQL string literal with double quotes. Handles escaped quotes
    * using SQL's doubled quote syntax (""). Matches: "anything with ""escaped""
    * quotes"
    */
  private def doubleQuotedString: Parser[SqlFragment] =
    regex("\"(?:\"\"|[^\"])*\"".r).map(text => SqlFragment.Literal(text))

  /** Parse a string literal (single or double quoted) */
  private def stringLiteral: Parser[SqlFragment] =
    singleQuotedString | doubleQuotedString

  /** Parse a named parameter like :paramName */
  private def namedParameter: Parser[SqlFragment] =
    (char(':') *> regex("[a-zA-Z_][a-zA-Z0-9_]*".r))
      .map(name => SqlFragment.NamedParam(name))

  /** Parse any SQL text that's not a string literal or named parameter */
  private def sqlText: Parser[SqlFragment] =
    regex("[^:'\"]+".r).map(text => SqlFragment.Literal(text))

  /** Parse a standalone colon (not part of a named parameter) */
  private def standaloneColon: Parser[SqlFragment] =
    char(':').map(_ => SqlFragment.Literal(":"))

  /** Parse a single SQL fragment */
  private def sqlFragment: Parser[SqlFragment] =
    stringLiteral | namedParameter.attempt | standaloneColon | sqlText

  /** Parse complete SQL into fragments */
  private def sqlParser: Parser[List[SqlFragment]] =
    sqlFragment.many

  /** Parse SQL with named parameters, returning (SQL with ?, parameter names in
    * order). Handles string literals to avoid false positives.
    * @param sql
    *   SQL string with :paramName placeholders
    * @return
    *   Tuple of (SQL with ?, Seq of parameter names in order)
    */
  def parse(sql: String): (String, Seq[String]) = {
    sqlParser.run(sql) match {
      case Left(error)      =>
        throw new Exception(s"Failed to parse SQL: ${error.toString}")
      case Right(fragments) =>
        val sqlBuilder = new StringBuilder
        val paramNames = scala.collection.mutable.ListBuffer[String]()

        fragments.foreach {
          case SqlFragment.Literal(text)    =>
            sqlBuilder.append(text)
          case SqlFragment.NamedParam(name) =>
            sqlBuilder.append('?')
            paramNames += name
        }

        (sqlBuilder.toString, paramNames.toSeq)
    }
  }

  /** Convert SQL with named parameters to a PsArgHolder with positional
    * parameters.
    * @param sqlTemplate
    *   SQL string with :paramName placeholders
    * @param namedParams
    *   Map of parameter names to values
    * @return
    *   PsArgHolder with positional parameters
    */
  def toPsArgHolder(
      sqlTemplate: String,
      namedParams: Map[String, PreparedStatementArg]
  ): PsArgHolder = {
    val (sql, paramNames) = parse(sqlTemplate)

    // Validate all parameters are provided
    val missing = paramNames.toSet.diff(namedParams.keySet)
    if (missing.nonEmpty) {
      throw PiggyException.MissingNamedParametersException(
        sqlTemplate,
        missing.toSeq
      )
    }

    // Warn about unused parameters (could be removed in production)
    val extra = namedParams.keySet.diff(paramNames.toSet)
    if (extra.nonEmpty) {
      // Log warning - in production you might want to throw or ignore
      System.err.println(
        s"Warning: Unused named parameters: ${extra.mkString(", ")}"
      )
    }

    // Map named parameters to positional order
    val positionalArgs = paramNames.map { name =>
      namedParams(name) // Safe because we validated above
    }

    PsArgHolder(sql, positionalArgs*)
  }
}
