package dev.alteration.branch.ursula.command.builtin

import dev.alteration.branch.ursula.args.{Argument, Flag}
import dev.alteration.branch.ursula.args.builtin.HelpFlag
import dev.alteration.branch.ursula.command.Command

case class HelpCommand(commands: Seq[Command], isDefault: Boolean)
    extends Command {

  override val isDefaultCommand: Boolean = isDefault

  override val description: String =
    "Prints a list of commands, and their description"

  override val usage: String = "help"

  override val examples: Seq[String] = Seq(
    "help",
    "help --help",
    "help -h"
  )

  override val trigger: String = "help"

  override val flags: Seq[Flag[?]] = Seq(
    HelpFlag
  )

  override val arguments: Seq[Argument[?]] = Seq.empty

  override def action(args: Seq[String]): Unit = {
    println("The CLI supports the following commands:")

    commands.filter(!_.hidden).foreach { c =>
      println(s"${c.trigger}: ${c.description}")
    }

    if !this.hidden then println(s"${this.trigger}: ${this.description}")

    println(s"use [cmd] ${HelpFlag._sk}, ${HelpFlag._lk} for cmd-specific help")
  }

}
