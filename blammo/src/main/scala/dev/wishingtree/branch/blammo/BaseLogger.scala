package dev.wishingtree.branch.blammo

import dev.wishingtree.branch.veil.RuntimeEnv.*
import dev.wishingtree.branch.veil.Veil

import java.util.logging.*

transparent trait BaseLogger { self =>

  private[blammo] val handler: Handler

  val logLevel: Level =
    Veil.runtimeEnv match {
      case DEV  => Level.ALL
      case TEST => Level.INFO
      case PROD => Level.WARNING
    }

  val logger: Logger = {
    val _logger  = Logger.getLogger(self.getClass.getName)
    _logger.setLevel(logLevel)
    _logger.setUseParentHandlers(false)
    _logger.getHandlers.foreach(_logger.removeHandler)
    val _handler = new ConsoleHandler()
    _handler.setFormatter(JsonFormatter())
    _logger.addHandler(_handler)
    _logger
  }

}
