package dev.alteration.branch.ursula.doc

import dev.alteration.branch.ursula.args.Flag
import dev.alteration.branch.ursula.extensions.Extensions.*

final case class FlagDoc(flag: Flag[?]) extends Documentation {

  override lazy val txt: String = {
    val sb = new StringBuilder()

    // Main flag line
    sb.append(s"${flag._sk}, ${flag._lk}")
    if (flag.expectsArgument) sb.append(" [arg]")
    sb.append(s"\t${flag.description}")
    if (flag.required) sb.append(" [required]")
    sb.newLine

    // Dependencies
    if (flag.dependsOn.nonEmpty) {
      sb.appendLine("Requires:".indented)
      flag.dependsOn.foreach { s =>
        s.foreach { f =>
          sb.appendLine(s"${f._sk}, ${f._lk}".indented.indented)
        }
      }
    }

    // Conflicts
    if (flag.exclusive.nonEmpty) {
      sb.appendLine("Conflicts with:".indented)
      flag.exclusive.foreach { s =>
        s.foreach { f =>
          sb.appendLine(s"${f._sk}, ${f._lk}".indented.indented)
        }
      }
    }

    sb.toString()
  }

}
