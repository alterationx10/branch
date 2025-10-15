package dev.alteration.branch.ursula.command

import dev.alteration.branch.ursula.args.{BooleanFlag, Flag}

/** A context object that provides typed access to parsed command-line
  * arguments. This eliminates the need for manual flag parsing in command
  * actions.
  */
trait CommandContext {

  /** Get the parsed value of a flag, if present
    * @param flag
    *   The flag to retrieve
    * @return
    *   Some(value) if the flag was provided, None otherwise (including when
    *   default is used)
    */
  def flag[R](flag: Flag[R]): Option[R]

  /** Get the parsed value of a required flag. Throws if not present.
    * @param flag
    *   The flag to retrieve
    * @throws IllegalArgumentException
    *   if the flag is not present
    * @return
    *   The parsed flag value
    */
  def requiredFlag[R](flag: Flag[R]): R

  /** Check if a boolean flag is present
    * @param flag
    *   The boolean flag to check
    * @return
    *   true if present, false otherwise
    */
  def booleanFlag(flag: BooleanFlag): Boolean

  /** Get positional arguments after all flags have been stripped
    * @return
    *   The sequence of positional arguments
    */
  def args: Seq[String]

  /** Get the raw unparsed arguments passed to the command
    * @return
    *   The raw argument sequence
    */
  def rawArgs: Seq[String]
}

/** Default implementation of CommandContext
  */
private[ursula] class CommandContextImpl(
    command: Command,
    val rawArgs: Seq[String]
) extends CommandContext {

  def flag[R](flag: Flag[R]): Option[R] =
    flag.parseFirstArg(rawArgs)

  def requiredFlag[R](flag: Flag[R]): R =
    flag.parseFirstArg(rawArgs).getOrElse {
      throw new IllegalArgumentException(
        s"Required flag --${flag.name} not provided"
      )
    }

  def booleanFlag(flag: BooleanFlag): Boolean =
    flag.isPresent(rawArgs)

  lazy val args: Seq[String] =
    command.stripFlags(rawArgs)
}
