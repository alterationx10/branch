package dev.wishingtree.branch.blammo

import java.util.logging.*

trait JsonConsoleLogger extends BaseLogger {
  override private[blammo] val handler = new ConsoleHandler()
}
