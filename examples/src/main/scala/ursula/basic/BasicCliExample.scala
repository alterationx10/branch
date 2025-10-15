package ursula.basic

import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag, IntFlag, StringFlag}
import dev.alteration.branch.ursula.command.Command

/** Traditional (pre-ergonomic improvements) CLI example.
  *
  * This shows the original way of defining commands and flags. While this still
  * works perfectly, the modern approach (see ModernCliExample) offers better
  * ergonomics.
  *
  * Run with:
  * {{{
  * sbt "examples/runMain ursula.basic.BasicCliExample greet -n Alice"
  * sbt "examples/runMain ursula.basic.BasicCliExample greet -n Bob --loud"
  * }}}
  */
object BasicCliExample extends UrsulaApp {
  override val commands: Seq[Command] = Seq(GreetCommand)
}

// Traditional flag definitions
object NameFlag extends StringFlag {
  override val name: String        = "name"
  override val shortKey: String    = "n"
  override val description: String = "Name to greet"
  override val required: Boolean   = true
}

object LoudFlag extends BooleanFlag {
  override val name: String        = "loud"
  override val shortKey: String    = "l"
  override val description: String = "Greet loudly (uppercase)"
}

object RepeatFlag extends IntFlag {
  override val name: String         = "repeat"
  override val shortKey: String     = "r"
  override val description: String  = "Number of times to repeat greeting"
  override val default: Option[Int] = Some(1)
}

object GreetCommand extends Command {
  override val trigger: String             = "greet"
  override val description: String         = "Greet someone"
  override val usage: String               = "greet -n <name> [options]"
  override val examples: Seq[String]       = Seq(
    "greet -n Alice",
    "greet -n Bob --loud",
    "greet -n Charlie -r 3"
  )
  override val flags: Seq[Flag[?]]         = Seq(NameFlag, LoudFlag, RepeatFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def action(args: Seq[String]): Unit = {
    // Manual flag parsing - requires .get calls
    val name = NameFlag.parseFirstArg(args).get
    val loud = LoudFlag.isPresent(args)
    val repeat = RepeatFlag.parseFirstArg(args).get

    val greeting = if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"

    (1 to repeat).foreach { i =>
      println(s"$i. $greeting")
    }
  }
}
