package dev.alteration.branch.blammo

import java.util.logging.*

/** A logger that writes JSON formatted log records to the console
  */
trait JsonConsoleLogger extends BaseLogger {
  override private[blammo] val handler = new ConsoleHandler()
}
