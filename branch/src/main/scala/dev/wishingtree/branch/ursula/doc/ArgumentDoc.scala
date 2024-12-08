package dev.wishingtree.branch.ursula.doc

import dev.wishingtree.branch.ursula.args.Argument

final case class ArgumentDoc(arg: Argument[?]) extends Documentation {
  override lazy val txt: String = {
    val sb = new StringBuilder()
    sb.append(s"${arg.name}\t${arg.description}")
    if (arg.required) {
      sb.append(" [required]")
    }
    sb.toString()
  }
}
