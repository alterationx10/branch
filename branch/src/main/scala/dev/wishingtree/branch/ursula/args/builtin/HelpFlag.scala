package dev.wishingtree.branch.ursula.args.builtin

import dev.wishingtree.branch.ursula.args.BooleanFlag

case object HelpFlag extends BooleanFlag {
  override val name: String        = "help"
  override val shortKey: String    = "h"
  override val description: String = "Prints help"
}
