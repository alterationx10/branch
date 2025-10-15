package dev.alteration.branch.ursula.command

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.ursula.args.{
  Argument,
  BooleanFlag,
  Flag,
  IntFlag,
  StringFlag
}
import munit.*

import scala.concurrent.ExecutionContext

object CommandContextSpec {

  object TestPortFlag extends IntFlag {
    override val name: String        = "port"
    override val shortKey: String    = "p"
    override val description: String = "Port number"
    override val default             = Some(8080)
  }

  object TestVerboseFlag extends BooleanFlag {
    override val name: String        = "verbose"
    override val shortKey: String    = "v"
    override val description: String = "Verbose output"
  }

  object TestNameFlag extends StringFlag {
    override val name: String        = "name"
    override val shortKey: String    = "n"
    override val description: String = "Name"
  }

  object TestCommand extends Command {
    override val description: String         = "Test command"
    override val usage: String               = "test [flags]"
    override val examples: Seq[String]       = Seq("test -p 9000")
    override val trigger: String             = "test"
    override val flags: Seq[Flag[?]]         =
      Seq(TestPortFlag, TestVerboseFlag, TestNameFlag)
    override val arguments: Seq[Argument[?]] = Seq.empty

    var lastContext: Option[CommandContext] = None

    override def actionWithContext(ctx: CommandContext): Unit = {
      lastContext = Some(ctx)
    }
  }
}

class CommandContextSpec extends FunSuite {
  given ExecutionContext = BranchExecutors.executionContext

  import CommandContextSpec._

  test("provide typed access to flags") {
    val ctx =
      new CommandContextImpl(TestCommand, Seq("-p", "9000", "-n", "test"))

    assertEquals(ctx.flag(TestPortFlag), Some(9000))
    assertEquals(ctx.flag(TestNameFlag), Some("test"))
  }

  test("return None for absent flags") {
    val ctx = new CommandContextImpl(TestCommand, Seq.empty)

    assertEquals(ctx.flag(TestNameFlag), None)
  }

  test("return default values when flag not provided") {
    val ctx = new CommandContextImpl(TestCommand, Seq.empty)

    assertEquals(ctx.flag(TestPortFlag), Some(8080))
  }

  test("check boolean flags correctly") {
    val ctx1 = new CommandContextImpl(TestCommand, Seq("-v"))
    assertEquals(ctx1.booleanFlag(TestVerboseFlag), true)

    val ctx2 = new CommandContextImpl(TestCommand, Seq.empty)
    assertEquals(ctx2.booleanFlag(TestVerboseFlag), false)
  }

  test("throw when requiredFlag is not present") {
    val ctx = new CommandContextImpl(TestCommand, Seq.empty)

    intercept[IllegalArgumentException] {
      ctx.requiredFlag(TestNameFlag)
    }
  }

  test("return value for requiredFlag when present") {
    val ctx = new CommandContextImpl(TestCommand, Seq("-n", "myname"))

    assertEquals(ctx.requiredFlag(TestNameFlag), "myname")
  }

  test("strip flags from args") {
    val ctx = new CommandContextImpl(
      TestCommand,
      Seq("-p", "9000", "arg1", "-v", "arg2", "-n", "test", "arg3")
    )

    assertEquals(ctx.args, Seq("arg1", "arg2", "arg3"))
  }

  test("provide rawArgs unchanged") {
    val rawArgs = Seq("-p", "9000", "arg1")
    val ctx     = new CommandContextImpl(TestCommand, rawArgs)

    assertEquals(ctx.rawArgs, rawArgs)
  }

  test("be used by Command.lazyAction") {
    TestCommand.lastContext = None

    val result = TestCommand.lazyAction(Seq("-p", "3000", "-v")).runSync()

    assert(result.isSuccess)
    assert(TestCommand.lastContext.isDefined)

    val ctx = TestCommand.lastContext.get
    assertEquals(ctx.flag(TestPortFlag), Some(3000))
    assertEquals(ctx.booleanFlag(TestVerboseFlag), true)
  }
}
