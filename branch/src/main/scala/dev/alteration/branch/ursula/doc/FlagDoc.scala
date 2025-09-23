package dev.alteration.branch.ursula.doc

import dev.alteration.branch.ursula.args.Flag
import dev.alteration.branch.ursula.extensions.Extensions.*

final case class FlagDoc(flag: Flag[?]) extends Documentation {

  override lazy val txt: String = {
    val sb: StringBuilder = new StringBuilder()
    sb.append(s"${flag._sk}, ${flag._lk}")
    if (flag.expectsArgument) sb.append(" [arg]")
    sb.append(s"\t${flag.description}")
    if (flag.required) sb.append(" [required")
    sb.newLine
    if (flag.dependsOn.nonEmpty) {
      sb.appendLine("Requires:")
      flag.dependsOn.foreach { s =>
        s.foreach { f =>
          sb.appendLine(s"\t${f._sk}, ${f._lk}")
        }
      }
    }
    if (flag.exclusive.nonEmpty) {
      sb.appendLine("Conflicts with:")
      flag.exclusive.foreach { s =>
        s.foreach { f =>
          sb.appendLine(s"\t${f._sk}, ${f._lk}")
        }
      }
    }
    sb.toString()
  }

}
