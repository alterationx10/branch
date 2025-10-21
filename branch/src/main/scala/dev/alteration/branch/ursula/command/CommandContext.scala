package dev.alteration.branch.ursula.command

import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag}

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

  /** Get the parsed and typed value of a positional argument at the specified
    * index. Handles parsing, default values, required validation, and options
    * validation automatically.
    * @param index
    *   The zero-based position of the argument
    * @param argument
    *   The argument definition to use for parsing
    * @throws IllegalArgumentException
    *   if the argument is required but not provided, or if the value is not in
    *   the allowed options
    * @return
    *   The parsed argument value (or default if not provided and default exists)
    */
  def argument[R](index: Int, argument: Argument[R]): R

  /** Get the parsed value of a positional argument at the specified index, or
    * None if not present and not required.
    * @param index
    *   The zero-based position of the argument
    * @param argument
    *   The argument definition to use for parsing
    * @return
    *   Some(value) if present, None if not present and has default or not
    *   required
    */
  def optionalArgument[R](index: Int, argument: Argument[R]): Option[R]

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

  def argument[R](index: Int, argument: Argument[R]): R =
    argument.valueOrDefault(args.lift(index))

  def optionalArgument[R](index: Int, argument: Argument[R]): Option[R] =
    args.lift(index).map(argument.parse).orElse(argument.default)

  lazy val args: Seq[String] =
    command.stripFlags(rawArgs)
}
