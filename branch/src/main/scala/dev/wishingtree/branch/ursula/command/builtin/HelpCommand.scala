package dev.wishingtree.branch.ursula.command.builtin

//case class HelpCommand(commands: Seq[Command], isDefault: Boolean)
//  extends Command {
//
//  override val isDefaultCommand: Boolean = isDefault
//
//  override val description: String =
//    "Prints a list of commands, and their description"
//
//  override val usage: String = "help"
//
//  override val examples: Seq[String] = Seq(
//    "help",
//    "help --help",
//    "help -h"
//  )
//
//  override val trigger: String = "help"
//
//  override val flags: Seq[Flag[?]] = Seq(
//    HelpFlag
//  )
//
//  override val arguments: Seq[Argument[?]] = Seq.empty
//
//  override def action(args: Chunk[String]): Task[Unit] = for {
//    _ <- Console.printLine("The CLI supports the following commands:")
//    _ <- ZIO.foreachDiscard(commands.filter(!_.hidden)) { c =>
//      Console.printLine(s"${c.trigger}: ${c.description}")
//    }
//    _ <- ZIO.when(!this.hidden)(
//      Console.printLine(s"${this.trigger}: ${this.description}")
//    )
//    _ <-
//      Console.printLine(
//        s"use [cmd] ${HelpFlag._sk}, ${HelpFlag._lk} for cmd-specific help"
//      )
//  } yield ()
//
//}
