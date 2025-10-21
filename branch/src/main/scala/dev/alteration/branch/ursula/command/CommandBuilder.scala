package dev.alteration.branch.ursula.command

import dev.alteration.branch.ursula.args.{Argument, Flag}

/** Fluent builder for creating Command instances.
  *
  * Provides a chainable API for configuring commands step by step. This is
  * particularly useful for complex commands or when building commands
  * dynamically.
  *
  * Example usage:
  * {{{
  * val cmd = Commands.builder("greet")
  *   .description("Greet someone")
  *   .usage("greet -n <name> [options]")
  *   .example("greet -n Alice")
  *   .example("greet -n Bob --loud")
  *   .withFlags(nameFlag, loudFlag, repeatFlag)
  *   .withArguments(messageArg)
  *   .strict(true)
  *   .action { ctx =>
  *     val name = ctx.requiredFlag(nameFlag)
  *     println(s"Hello, $name!")
  *   }
  *   .build()
  * }}}
  */
class CommandBuilder(trigger: String) {
  private var _description: String                    = ""
  private var _usage: String                          = trigger
  private var _examples: Seq[String]                  = Seq.empty
  private var _flags: Seq[Flag[?]]                    = Seq.empty
  private var _arguments: Seq[Argument[?]]            = Seq.empty
  private var _strict: Boolean                        = true
  private var _hidden: Boolean                        = false
  private var _isDefaultCommand: Boolean              = false
  private var _action: Option[CommandContext => Unit] = None

  /** Set the command description.
    *
    * @param desc
    *   Brief description of what the command does
    * @return
    *   This builder for chaining
    */
  def description(desc: String): CommandBuilder = {
    _description = desc
    this
  }

  /** Set the usage string.
    *
    * @param usage
    *   Usage string showing command syntax
    * @return
    *   This builder for chaining
    */
  def usage(usage: String): CommandBuilder = {
    _usage = usage
    this
  }

  /** Add a single example.
    *
    * @param example
    *   Example invocation of the command
    * @return
    *   This builder for chaining
    */
  def example(example: String): CommandBuilder = {
    _examples = _examples :+ example
    this
  }

  /** Set all examples at once.
    *
    * @param examples
    *   All example invocations
    * @return
    *   This builder for chaining
    */
  def examples(examples: Seq[String]): CommandBuilder = {
    _examples = examples
    this
  }

  /** Add flags to the command.
    *
    * Can be called multiple times to add flags incrementally.
    *
    * @param flags
    *   Flags this command accepts
    * @return
    *   This builder for chaining
    */
  def withFlags(flags: Flag[?]*): CommandBuilder = {
    _flags = _flags ++ flags
    this
  }

  /** Set all flags at once, replacing any previously added.
    *
    * @param flags
    *   All flags this command accepts
    * @return
    *   This builder for chaining
    */
  def flags(flags: Seq[Flag[?]]): CommandBuilder = {
    _flags = flags
    this
  }

  /** Add arguments to the command.
    *
    * Can be called multiple times to add arguments incrementally.
    *
    * @param args
    *   Positional arguments this command expects
    * @return
    *   This builder for chaining
    */
  def withArguments(args: Argument[?]*): CommandBuilder = {
    _arguments = _arguments ++ args
    this
  }

  /** Set all arguments at once, replacing any previously added.
    *
    * @param args
    *   All positional arguments this command expects
    * @return
    *   This builder for chaining
    */
  def arguments(args: Seq[Argument[?]]): CommandBuilder = {
    _arguments = args
    this
  }

  /** Set strict mode.
    *
    * When true (default), the command will fail on unrecognized flags, missing
    * required flags, or conflicting flags.
    *
    * @param strict
    *   Whether to enforce strict flag validation
    * @return
    *   This builder for chaining
    */
  def strict(strict: Boolean): CommandBuilder = {
    _strict = strict
    this
  }

  /** Mark this command as hidden from help output.
    *
    * @return
    *   This builder for chaining
    */
  def hidden(): CommandBuilder = {
    _hidden = true
    this
  }

  /** Mark this command as visible in help output (default).
    *
    * @return
    *   This builder for chaining
    */
  def visible(): CommandBuilder = {
    _hidden = false
    this
  }

  /** Mark this command as the default command.
    *
    * The default command will be executed when no matching trigger is found.
    * Only one command per CLI should be marked as default.
    *
    * @return
    *   This builder for chaining
    */
  def asDefault(): CommandBuilder = {
    _isDefaultCommand = true
    this
  }

  /** Set the action to execute when the command is invoked.
    *
    * @param action
    *   Function that takes a CommandContext and performs the command logic
    * @return
    *   This builder for chaining
    */
  def action(action: CommandContext => Unit): CommandBuilder = {
    _action = Some(action)
    this
  }

  /** Build the final Command instance.
    *
    * @throws IllegalStateException
    *   if the description or action has not been set
    * @return
    *   A configured Command instance
    */
  def build(): Command = {
    if (_description.isEmpty) {
      throw new IllegalStateException("Command description is required")
    }
    if (_action.isEmpty) {
      throw new IllegalStateException("Command action is required")
    }

    Commands.create(
      trigger = trigger,
      description = _description,
      usage = _usage,
      examples = _examples,
      flags = _flags,
      arguments = _arguments,
      strict = _strict,
      hidden = _hidden,
      isDefaultCommand = _isDefaultCommand
    )(_action.get)
  }

}
