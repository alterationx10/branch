package dev.wishingtree.branch.ursula

import dev.wishingtree.branch.lzy.Lazy
import dev.wishingtree.branch.ursula.command.Command
import dev.wishingtree.branch.ursula.command.builtin.HelpCommand
import scala.util.*

trait UrsulaApp {

  /** This setting determines whether the built in HelpCommand is the default
    * command. Defaults true, override to false if you want to use a different
    * Command as default.
    */
  val defaultHelp: Boolean = true

  /** This is a Seq of your Command implementations that you want your CLI to
    * have access to.
    */
  val commands: Seq[Command]

  /** A collection of built-in Commands that are always available to the CLI.
    */
  private lazy val builtInCommands: Seq[Command] = Seq(
    HelpCommand(commands = commands, isDefault = defaultHelp)
  )

  /** Provided and built-in commands combined */
  private lazy val _allCommands = commands ++ builtInCommands

  /** A map of trigger -> Command for quick lookup */
  private lazy val commandMap: Map[String, Command] =
    _allCommands.groupBy(_.trigger).map { case (k, v) =>
      if v.size > 1 then {
        println(
          s"Multiple commands injected with the same trigger - using first found:"
        )
        v.foreach(c => println(c.getClass.getSimpleName))
      }
      k -> v.head
    }

  /** A list of all triggers for all commands */
  private lazy val triggerList: Seq[String] =
    _allCommands.map(_.trigger)

  /** The default command to run if no trigger is found */
  private lazy val findDefaultCommand: Option[Command] = {
    val default = _allCommands.filter(_.isDefaultCommand)
    if default.size > 1 then {
      println(
        "Multiple commands injected with isDefaultCommand=true - using the first:"
      )
      default.foreach(c => println(c.getClass.getSimpleName))
    }
    default.headOption
  }

  @volatile
  private var dropOne = true

  final def main(args: Array[String]): Unit = {
    val lzyApp = for {
      cmd <- Lazy
               .fromOption(args.headOption)
               .map(commandMap.get)
               .recover { case _: NoSuchElementException =>
                 dropOne = false
                 Lazy.fn(findDefaultCommand)
               }
               .someOrFail(
                 new IllegalArgumentException(
                   "Could not find command from argument, and no default command provided"
                 )
               )

      result <- cmd.lazyAction(if dropOne then args.drop(1) else args)
    } yield result

    lzyApp.runSync() match {
      case Success(value)     => ()
      case Failure(exception) => println(s"${exception.getMessage}")
    }
  }

}
