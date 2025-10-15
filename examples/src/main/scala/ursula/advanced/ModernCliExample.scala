package ursula.advanced

import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

/** Modern CLI example using ergonomic improvements.
  *
  * This demonstrates:
  * - Concise flag definitions using Flags factory
  * - Type-safe flag access via CommandContext
  * - Multiple commands in one CLI
  *
  * Run with:
  * {{{
  * sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Alice"
  * sbt "examples/runMain ursula.advanced.ModernCliExample math --add 5 10"
  * sbt "examples/runMain ursula.advanced.ModernCliExample math --multiply 3 7 --verbose"
  * }}}
  */
object ModernCliExample extends UrsulaApp {
  override val commands: Seq[Command] = Seq(GreetCommand, MathCommand)
}

object GreetCommand extends Command {
  // Concise flag definitions using Flags factory
  val NameFlag = Flags.string(
    name = "name",
    shortKey = "n",
    description = "Name to greet",
    required = true
  )

  val LoudFlag = Flags.boolean(
    name = "loud",
    shortKey = "l",
    description = "Greet loudly (uppercase)"
  )

  val RepeatFlag = Flags.int(
    name = "repeat",
    shortKey = "r",
    description = "Number of times to repeat greeting",
    default = Some(1)
  )

  override val trigger = "greet"
  override val description = "Greet someone"
  override val usage = "greet -n <name> [options]"
  override val examples = Seq(
    "greet -n Alice",
    "greet -n Bob --loud",
    "greet -n Charlie -r 3"
  )
  override val flags = Seq(NameFlag, LoudFlag, RepeatFlag)
  override val arguments = Seq.empty

  // Type-safe action using CommandContext
  override def actionWithContext(ctx: CommandContext): Unit = {
    // No .get calls needed - type-safe access!
    val name = ctx.requiredFlag(NameFlag)
    val loud = ctx.booleanFlag(LoudFlag)
    val repeat = ctx.requiredFlag(RepeatFlag)

    val greeting = if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"

    (1 to repeat).foreach { i =>
      println(s"$i. $greeting")
    }
  }
}

object MathCommand extends Command {
  // Flags for different math operations
  val AddFlag = Flags.boolean(
    name = "add",
    shortKey = "a",
    description = "Add numbers"
  )

  val MultiplyFlag = Flags.boolean(
    name = "multiply",
    shortKey = "m",
    description = "Multiply numbers"
  )

  val VerboseFlag = Flags.boolean(
    name = "verbose",
    shortKey = "v",
    description = "Show detailed output"
  )

  override val trigger = "math"
  override val description = "Perform mathematical operations"
  override val usage = "math [--add|--multiply] <numbers...>"
  override val examples = Seq(
    "math --add 1 2 3 4",
    "math --multiply 5 10",
    "math -m 2 3 4 --verbose"
  )
  override val flags = Seq(AddFlag, MultiplyFlag, VerboseFlag)
  override val arguments = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val add = ctx.booleanFlag(AddFlag)
    val multiply = ctx.booleanFlag(MultiplyFlag)
    val verbose = ctx.booleanFlag(VerboseFlag)

    // Get positional arguments (numbers)
    val numbers = ctx.args.map(_.toDouble)

    if (numbers.isEmpty) {
      println("Error: Please provide at least one number")
      return
    }

    val result = (add, multiply) match {
      case (true, false) =>
        val sum = numbers.sum
        if (verbose) {
          println(s"Adding: ${numbers.mkString(" + ")}")
        }
        sum

      case (false, true) =>
        val product = numbers.product
        if (verbose) {
          println(s"Multiplying: ${numbers.mkString(" × ")}")
        }
        product

      case (true, true) =>
        println("Error: Cannot use both --add and --multiply")
        return

      case (false, false) =>
        println("Error: Please specify either --add or --multiply")
        return
    }

    println(s"Result: $result")
  }
}
