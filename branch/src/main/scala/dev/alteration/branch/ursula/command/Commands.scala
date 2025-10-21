package dev.alteration.branch.ursula.command

import dev.alteration.branch.ursula.args.{Argument, Flag}

/** Factory methods for creating commands with less boilerplate.
  *
  * Provides three approaches to command creation:
  *
  * 1. Traditional (extend Command trait directly) - for maximum control
  * 2. Inline factory (Commands.create) - for concise definitions
  * 3. Builder pattern (Commands.builder) - for fluent, discoverable API
  *
  * Example usage:
  * {{{
  * // Inline factory
  * val cmd = Commands.create(
  *   trigger = "greet",
  *   description = "Greet someone",
  *   flags = Seq(nameFlag)
  * ) { ctx =>
  *   println(s"Hello, ${ctx.requiredFlag(nameFlag)}!")
  * }
  *
  * // Builder pattern
  * val cmd = Commands.builder("greet")
  *   .description("Greet someone")
  *   .withFlags(nameFlag, loudFlag)
  *   .action { ctx => println("Hello!") }
  *   .build()
  *
  * // Simple factory (minimal)
  * val cmd = Commands.simple("greet", "Greet someone", Seq(nameFlag)) { ctx =>
  *   println("Hello!")
  * }
  * }}}
  */
object Commands {

  /** Create a command with all options specified inline.
    *
    * This is the most direct factory method, allowing all command properties
    * to be specified as named parameters.
    *
    * @param trigger
    *   The command trigger (what the user types)
    * @param description
    *   Brief description of what the command does
    * @param usage
    *   Usage string showing syntax (defaults to trigger)
    * @param examples
    *   Example invocations
    * @param flags
    *   Flags this command accepts
    * @param arguments
    *   Positional arguments this command expects
    * @param strict
    *   Whether to enforce strict flag validation
    * @param hidden
    *   Whether to hide from help output
    * @param isDefaultCommand
    *   Whether this is the default command
    * @param action
    *   The action to execute when the command is invoked
    * @return
    *   A Command instance
    */
  def create(
      trigger: String,
      description: String,
      usage: String = "",
      examples: Seq[String] = Seq.empty,
      flags: Seq[Flag[?]] = Seq.empty,
      arguments: Seq[Argument[?]] = Seq.empty,
      strict: Boolean = true,
      hidden: Boolean = false,
      isDefaultCommand: Boolean = false
  )(action: CommandContext => Unit): Command = {
    val _trigger          = trigger
    val _description      = description
    val _usage            = if (usage.isEmpty) trigger else usage
    val _examples         = examples
    val _flags            = flags
    val _arguments        = arguments
    val _strict           = strict
    val _hidden           = hidden
    val _isDefaultCommand = isDefaultCommand
    val _action           = action

    new Command {
      override val trigger: String                 = _trigger
      override val description: String             = _description
      override val usage: String                   = _usage
      override val examples: Seq[String]           = _examples
      override val flags: Seq[Flag[?]]             = _flags
      override val arguments: Seq[Argument[?]]     = _arguments
      override val strict: Boolean                 = _strict
      override val hidden: Boolean                 = _hidden
      override val isDefaultCommand: Boolean       = _isDefaultCommand
      override def actionWithContext(ctx: CommandContext): Unit = _action(ctx)
    }
  }

  /** Create a simple command with minimal boilerplate.
    *
    * Provides sensible defaults for most command properties. Usage is set to
    * the trigger, no examples, strict mode enabled.
    *
    * @param trigger
    *   The command trigger (what the user types)
    * @param description
    *   Brief description of what the command does
    * @param flags
    *   Flags this command accepts (defaults to empty)
    * @param arguments
    *   Positional arguments this command expects (defaults to empty)
    * @param action
    *   The action to execute when the command is invoked
    * @return
    *   A Command instance
    */
  def simple(
      trigger: String,
      description: String,
      flags: Seq[Flag[?]] = Seq.empty,
      arguments: Seq[Argument[?]] = Seq.empty
  )(action: CommandContext => Unit): Command = {
    create(
      trigger = trigger,
      description = description,
      usage = trigger,
      flags = flags,
      arguments = arguments
    )(action)
  }

  /** Start building a command using the fluent builder pattern.
    *
    * Returns a CommandBuilder that allows you to configure the command step by
    * step using a chainable API.
    *
    * @param trigger
    *   The command trigger (what the user types)
    * @return
    *   A CommandBuilder instance
    */
  def builder(trigger: String): CommandBuilder = {
    new CommandBuilder(trigger)
  }

}
