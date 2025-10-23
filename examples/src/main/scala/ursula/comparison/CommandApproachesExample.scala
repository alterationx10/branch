package ursula.comparison

import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.{Argument, Flag, Flags}
import dev.alteration.branch.ursula.command.{Command, CommandContext, Commands}

/** Demonstration of all three command creation approaches.
  *
  * This example shows the same greeting functionality implemented in three
  * different ways:
  *
  * 1. Traditional approach (extending Command trait)
  * 2. Inline factory approach (Commands.create/simple)
  * 3. Builder pattern approach (Commands.builder)
  *
  * Run with:
  * {{{
  * sbt "examples/runMain ursula.comparison.CommandApproachesExample traditional -n Alice"
  * sbt "examples/runMain ursula.comparison.CommandApproachesExample inline -n Bob --loud"
  * sbt "examples/runMain ursula.comparison.CommandApproachesExample builder -n Charlie -r 3"
  * }}}
  */
object CommandApproachesExample extends UrsulaApp {
  override val commands: Seq[Command] = Seq(
    TraditionalCommand,
    InlineCommand.command,
    BuilderCommand.command
  )
}

object TraditionalCommand extends Command {
  // Define flags as objects for external access
  val NameFlag   = Flags.string("name", "n", "Name to greet", required = true)
  val LoudFlag   = Flags.boolean("loud", "l", "Greet loudly")
  val RepeatFlag = Flags.int("repeat", "r", "Repeat count", default = Some(1))

  // Required overrides
  override val trigger: String             = "traditional"
  override val description: String         = "Greet using traditional approach"
  override val usage: String               = "traditional -n <name> [options]"
  override val examples: Seq[String]       = Seq(
    "traditional -n Alice",
    "traditional -n Bob --loud",
    "traditional -n Charlie -r 3"
  )
  override val flags: Seq[Flag[?]]         = Seq(NameFlag, LoudFlag, RepeatFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  // Action implementation
  override def actionWithContext(ctx: CommandContext): Unit = {
    val name   = ctx.requiredFlag(NameFlag)
    val loud   = ctx.booleanFlag(LoudFlag)
    val repeat = ctx.requiredFlag(RepeatFlag)

    val greeting = if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"

    println(s"[Traditional Approach]")
    (1 to repeat).foreach { i =>
      println(s"  $i. $greeting")
    }
  }
}

object InlineCommand {
  // Flags are defined in the outer object for reuse
  val NameFlag   = Flags.string("name", "n", "Name to greet", required = true)
  val LoudFlag   = Flags.boolean("loud", "l", "Greet loudly")
  val RepeatFlag = Flags.int("repeat", "r", "Repeat count", default = Some(1))

  // Create command using inline factory
  val command: Command = Commands.create(
    trigger = "inline",
    description = "Greet using inline factory approach",
    usage = "inline -n <name> [options]",
    examples = Seq(
      "inline -n Alice",
      "inline -n Bob --loud",
      "inline -n Charlie -r 3"
    ),
    flags = Seq(NameFlag, LoudFlag, RepeatFlag)
  ) { ctx =>
    val name   = ctx.requiredFlag(NameFlag)
    val loud   = ctx.booleanFlag(LoudFlag)
    val repeat = ctx.requiredFlag(RepeatFlag)

    val greeting = if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"

    println(s"[Inline Factory Approach]")
    (1 to repeat).foreach { i =>
      println(s"  $i. $greeting")
    }
  }
}

// Alternative: Ultra-minimal using Commands.simple
val SimpleInlineExample: Command = {
  val NameFlag = Flags.string("name", "n", "Name", required = true)

  Commands.simple(
    trigger = "simple",
    description = "Minimal greeting",
    flags = Seq(NameFlag)
  ) { ctx =>
    println(s"Hello, ${ctx.requiredFlag(NameFlag)}!")
  }
}

object BuilderCommand {
  // Flags defined in outer object
  val NameFlag   = Flags.string("name", "n", "Name to greet", required = true)
  val LoudFlag   = Flags.boolean("loud", "l", "Greet loudly")
  val RepeatFlag = Flags.int("repeat", "r", "Repeat count", default = Some(1))

  // Build command using fluent API
  val command: Command = Commands.builder("builder")
    .description("Greet using builder pattern approach")
    .usage("builder -n <name> [options]")
    .example("builder -n Alice")
    .example("builder -n Bob --loud")
    .example("builder -n Charlie -r 3")
    .withFlags(NameFlag, LoudFlag, RepeatFlag)
    .action { ctx =>
      val name   = ctx.requiredFlag(NameFlag)
      val loud   = ctx.booleanFlag(LoudFlag)
      val repeat = ctx.requiredFlag(RepeatFlag)

      val greeting =
        if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"

      println(s"[Builder Pattern Approach]")
      (1 to repeat).foreach { i =>
        println(s"  $i. $greeting")
      }
    }
    .build()
}
