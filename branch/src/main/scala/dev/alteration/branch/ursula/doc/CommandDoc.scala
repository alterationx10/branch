package dev.alteration.branch.ursula.doc

import dev.alteration.branch.ursula.command.Command
import dev.alteration.branch.ursula.extensions.Extensions.*

final case class CommandDoc(cmd: Command) extends Documentation {

  override lazy val txt: String = {
    val sb = new StringBuilder()

    // Command header
    sb.appendLine(s"${cmd.trigger}\t${cmd.description}")

    // Flags section
    if (cmd.flags.nonEmpty) {
      sb.appendLine("Flags:")
      cmd.flags.sortBy(_.name).foreach { f =>
        sb.appendLine(f.documentation.txt.indented)
      }
    }

    // Arguments section
    if (cmd.arguments.nonEmpty) {
      sb.appendLine("Arguments:")
      cmd.arguments.foreach { a =>
        sb.appendLine(a.documentation.txt.indented)
      }
    }

    // Usage section
    sb.appendLine("Usage:")
    sb.appendLine(cmd.usage.indented)

    // Examples section
    if (cmd.examples.nonEmpty) {
      sb.appendLine("Examples:")
      cmd.examples.foreach { e =>
        sb.appendLine(e.indented)
      }
    }

    sb.toString()
  }

}
