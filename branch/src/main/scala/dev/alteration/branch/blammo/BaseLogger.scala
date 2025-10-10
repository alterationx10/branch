package dev.alteration.branch.blammo

import dev.alteration.branch.veil.RuntimeEnv.*
import dev.alteration.branch.veil.Veil

import java.util.logging.*

/** A base trait for JSON logging. The default log level is set from
  * [[Veil.runtimeEnv]]
  * {{{
  *   Veil.runtimeEnv match {
  *       case DEV  => Level.ALL
  *       case TEST => Level.INFO
  *       case PROD => Level.WARNING
  *     }
  * }}}
  */
transparent trait BaseLogger { self =>

  /** The handler for the logger. */
  private[blammo] val handler: Handler

  /** The default log level. Default value set from [[Veil.runtimeEnv]]
    * {{{
    *   Veil.runtimeEnv match {
    *       case DEV  => Level.ALL
    *       case TEST => Level.INFO
    *       case PROD => Level.WARNING
    *     }
    * }}}
    */
  val logLevel: Level =
    Veil.runtimeEnv match {
      case DEV  => Level.ALL
      case TEST => Level.INFO
      case PROD => Level.WARNING
    }

  /** The logger instance. Existing handlers are removed and a new [[handler]]
    * is added with the [[JsonFormatter]].
    */
  lazy val logger: Logger = {
    val _logger = Logger.getLogger(self.getClass.getName)
    _logger.setLevel(logLevel)
    _logger.setUseParentHandlers(false)
    _logger.getHandlers.foreach(_logger.removeHandler)
    handler.setFormatter(JsonFormatter())
    _logger.addHandler(handler)
    _logger
  }

}
