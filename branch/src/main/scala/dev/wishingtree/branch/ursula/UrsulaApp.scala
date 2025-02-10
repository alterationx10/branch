package dev.wishingtree.branch.ursula

import dev.wishingtree.branch.lzy.Lazy
import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors
import dev.wishingtree.branch.ursula.command.Command
import dev.wishingtree.branch.ursula.command.builtin.HelpCommand

import scala.concurrent.ExecutionContext
import scala.util.*

trait UrsulaApp {

  /** This setting determines whether the built-in HelpCommand is the default
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
      k -> v.head
    }

  /** The default command to run if no trigger is found */
  private lazy val findDefaultCommand: Option[Command] = {
    val default = _allCommands.filter(_.isDefaultCommand)
    default.headOption
  }

  /** The executor service to run the Lazy[A] evaluation powering the app.
    */
  val executionContext: ExecutionContext =
    BranchExecutors.executionContext

  @volatile
  private var dropOne = true

  final def main(args: Array[String]): Unit = {
    val lzyApp = for {
      cmd    <- Lazy
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
      result <-
        cmd.lazyAction(
          if dropOne then args.drop(1).toIndexedSeq else args.toIndexedSeq
        )
    } yield result

    lzyApp.runSync()(using executionContext) match {
      case Success(value)     =>
        System.exit(0)
      case Failure(exception) =>
        println(s"${exception.getMessage}")
        System.exit(1)
    }
  }

}
