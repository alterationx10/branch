package dev.alteration.branch.ursula.doc

import dev.alteration.branch.ursula.args.Argument
import dev.alteration.branch.ursula.extensions.Extensions.*

final case class ArgumentDoc(arg: Argument[?]) extends Documentation {

  override lazy val txt: String = {
    val sb = new StringBuilder()

    // Main argument line
    sb.append(s"${arg.name}\t${arg.description}")

    // Add required indicator
    if (arg.required) {
      sb.append(" [required]")
    }

    // Add default value if present
    arg.default.foreach { default =>
      sb.append(s" [default: $default]")
    }

    // Add allowed options if present
    arg.options.foreach { opts =>
      sb.append(s" [options: ${opts.mkString(", ")}]")
    }

    sb.newLine
    sb.toString()
  }

}
