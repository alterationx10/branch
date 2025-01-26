package dev.wishingtree.branch.ursula.args.builtin

import dev.wishingtree.branch.ursula.args.BooleanFlag

/** A flag that triggers the display of help information.
  *
  * This flag is a boolean flag, meaning it does not expect an argument.
  * It can be triggered using either the long form "--help" or the short form "-h".
  */
case object HelpFlag extends BooleanFlag {
  override val name: String        = "help"
  override val shortKey: String    = "h"
  override val description: String = "Prints help"
}