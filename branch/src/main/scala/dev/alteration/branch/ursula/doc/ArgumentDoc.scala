package dev.alteration.branch.ursula.doc

import dev.alteration.branch.ursula.args.Argument

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
