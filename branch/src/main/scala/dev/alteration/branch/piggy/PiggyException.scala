package dev.alteration.branch.piggy

/** Base exception for Piggy SQL operations with context information.
  */
sealed abstract class PiggyException(
    message: String,
    cause: Throwable = null
) extends RuntimeException(message, cause)

object PiggyException {

  /** Exception thrown when SQL execution fails, with SQL statement context.
    * @param sql
    *   the SQL statement that failed
    * @param cause
    *   the underlying exception
    */
  final case class SqlExecutionException(
      sql: String,
      cause: Throwable
  ) extends PiggyException(
        s"SQL execution failed: ${cause.getMessage}\nSQL: $sql",
        cause
      )

  /** Exception thrown when prepared statement execution fails, with SQL and
    * parameter context.
    * @param sql
    *   the SQL statement that failed
    * @param parameterCount
    *   the number of parameters
    * @param cause
    *   the underlying exception
    */
  final case class PreparedStatementException(
      sql: String,
      parameterCount: Int,
      cause: Throwable
  ) extends PiggyException(
        s"Prepared statement execution failed: ${cause.getMessage}\nSQL: $sql\nParameters: $parameterCount",
        cause
      )

  /** Exception thrown when result set parsing fails, with SQL context.
    * @param sql
    *   the SQL statement that was executed
    * @param cause
    *   the underlying exception
    */
  final case class ResultSetParseException(
      sql: String,
      cause: Throwable
  ) extends PiggyException(
        s"Result set parsing failed: ${cause.getMessage}\nSQL: $sql",
        cause
      )

  /** Exception thrown when batch execution fails, with context about which
    * batch failed.
    * @param sql
    *   the SQL statement that was batched
    * @param batchSize
    *   the size of the batch
    * @param cause
    *   the underlying exception
    */
  final case class BatchExecutionException(
      sql: String,
      batchSize: Int,
      cause: Throwable
  ) extends PiggyException(
        s"Batch execution failed: ${cause.getMessage}\nSQL: $sql\nBatch size: $batchSize",
        cause
      )

  /** Exception thrown when an empty argument sequence is provided where at
    * least one argument is required.
    * @param operation
    *   the operation that requires arguments
    */
  final case class EmptyArgumentException(
      operation: String
  ) extends PiggyException(
        s"$operation requires at least one argument"
      )

  /** Exception thrown when required named parameters are missing.
    * @param sql
    *   the SQL template with named parameters
    * @param missingParams
    *   the names of missing parameters
    */
  final case class MissingNamedParametersException(
      sql: String,
      missingParams: Seq[String]
  ) extends PiggyException(
        s"Missing named parameters: ${missingParams.mkString(", ")}\nSQL: $sql"
      )
}
