package dev.wishingtree.branch.blammo

import java.util.logging.*

trait JsonConsoleLogger { self =>
  
  lazy val logger: Logger = {
    val _logger  = Logger.getLogger(self.getClass.getCanonicalName)
    _logger.setUseParentHandlers(false)
    _logger.getHandlers.foreach(_logger.removeHandler)
    val _handler = new ConsoleHandler()
    _handler.setFormatter(JsonFormatter())
    _logger.addHandler(_handler)
    _logger
  }

}
