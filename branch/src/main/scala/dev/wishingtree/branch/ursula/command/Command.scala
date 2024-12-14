package dev.wishingtree.branch.ursula.command

import dev.wishingtree.branch.lzy.Lazy
import dev.wishingtree.branch.ursula.args.builtin.HelpFlag
import dev.wishingtree.branch.ursula.args.{Argument, Flag}
import dev.wishingtree.branch.ursula.doc.{CommandDoc, Documentation}

import scala.annotation.tailrec

trait Command {

  /** A brief description of what this command does.
    */
  val description: String

  /** A one-line generic example of how to use this command
    */
  val usage: String

  /** Some specific examples of how to use this command
    */
  val examples: Seq[String]

  /** The argument to trigger this command.
    */
  val trigger: String

  /** A collection fo [[Flag]] that this command supports.
    */
  val flags: Seq[Flag[?]]

  /** A collection of arguments this command expects.
    */
  val arguments: Seq[Argument[?]]

  /** Hides this command from the help
    */
  val hidden: Boolean = false

  /** Indicates if this command should be the default run, when no other
    * matching trigger is found. There can only be one per CLI.
    */
  val isDefaultCommand: Boolean = false

  /** The central logic to implement for this Command
    * @param args
    *   The cli arguments passed in, with any trigger command already stripped
    *   out.
    * @return
    */
  def action(args: Seq[String]): Unit

  /** Indicates if the program should stop on unrecognized, missing, and/or
    * conflicting flags.
    */
  val strict: Boolean = true

  private def hasBooleanFlag(a: String) =
    flags
      .filter(!_.expectsArgument)
      .exists(f => f._sk == a || f._lk == a)

  private def hasArgumentFlag(a: String) =
    flags
      .filter(_.expectsArgument)
      .exists(f => f._sk == a || f._lk == a)

  /** Strips flags and their arguments from the cli arguments, which can then be
    * parsed for Arguments
    * @param args
    *   The cli arguments
    */
  def stripFlags(args: Seq[String]): Seq[String] = {
    @tailrec
    def loop(a: Seq[String], r: Seq[String]): Seq[String] = {
      a.headOption match {
        case Some(h) => {
          if (hasBooleanFlag(h)) {
            loop(a.drop(1), r)
          } else if (hasArgumentFlag(h)) {
            loop(a.drop(2), r)
          } else {
            loop(a.drop(1), r.appended(h))
          }
        }
        case None    => r
      }
    }
    loop(args, Seq.empty)
  }

  /** An object that has formatted help for this command.
    */
  lazy val documentation: Documentation = CommandDoc(this)

  /** Prints documentation
    * @return
    */
  final def printHelp: Unit =
    println(documentation.txt)

  private final def unrecognizedFlags(args: Seq[String]): Boolean = {
    val flagTriggers: Seq[String] =
      flags.flatMap(f => Seq(f._sk, f._lk)).distinct
    args.filter(_.startsWith("-")).exists(a => !flagTriggers.contains(a))
  }

  private final def conflictingFlags(presentFlags: Seq[Flag[?]]): Boolean = {
    presentFlags
      .map { f =>
        presentFlags.flatMap(_.exclusive).flatten.contains(f)
      }
      .fold(false)(_ || _)
  }

  private final def missingRequiredFlags(
      presentFlags: Seq[Flag[?]]
  ): Boolean = {
    flags.filterNot(presentFlags.toSet).exists(_.required)
  }

  private final def printArgs(args: Seq[String]): Unit =
    println(s"> ${args.mkString(" ")}")

  private final val printHelpfulError: Seq[String] => Throwable => Unit = {
    args => error =>
      println(error.getMessage)
      printArgs(args)
      printHelp
  }

  private[ursula] final def lazyAction(
      args: Seq[String]
  ): Lazy[Unit] =
    for {
      showHelp         <- Lazy.fn(HelpFlag.isPresent(args))
      presentFlags     <- Lazy.fn(flags.filter(_.isPresent(args)))
      unrecognizedExit <-
        Lazy.fn(!showHelp && strict && unrecognizedFlags(args))
      conflictingExit  <-
        Lazy.fn(!showHelp && strict && conflictingFlags(presentFlags))
      missingExit      <-
        Lazy.fn(!showHelp && strict && missingRequiredFlags(presentFlags))
      _                <- Lazy.when(showHelp) {
                            Lazy.fn(printHelp)
                          }
      _                <- Lazy.when(unrecognizedExit) {
                            Lazy
                              .fail(new IllegalArgumentException("Unrecognized flags"))
                              .tapError { printHelpfulError(args) }
                          }
      _                <- Lazy.when(conflictingExit) {
                            Lazy
                              .fail(new IllegalArgumentException("Conflicting flags"))
                              .tapError { printHelpfulError(args) }
                          }
      _                <- Lazy.when(missingExit) {
                            Lazy
                              .fail(new IllegalArgumentException("Missing required flags"))
                              .tapError { printHelpfulError(args) }
                          }
      _                <- Lazy.fn(action(args)).when(!showHelp)
    } yield ()

}
