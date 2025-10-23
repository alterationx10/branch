package dev.alteration.branch.ursula.command

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.ursula.args.{
  Argument,
  Arguments,
  BooleanFlag,
  Flag,
  IntFlag,
  StringFlag
}
import munit.*

import java.nio.file.Path
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

  // Test arguments using the Arguments factory
  val NameArg =
    Arguments.string("name", "User name", required = true)
  val PortArg =
    Arguments.int("port", "Server port", default = Some(8080))
  val PathArg =
    Arguments.path("path", "File path")
  val EnvArg  = Arguments.string(
    "env",
    "Environment",
    options = Some(Set("dev", "test", "prod"))
  )

  // An argument NOT in the command's arguments sequence
  val OrphanArg = Arguments.string("orphan", "Orphan argument")

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

  object TestCommandWithArgs extends Command {
    override val description: String         = "Test command with arguments"
    override val usage: String               = "testargs <name> [port] [path] [env]"
    override val examples: Seq[String]       = Seq("testargs myname 9000")
    override val trigger: String             = "testargs"
    override val flags: Seq[Flag[?]]         = Seq(TestVerboseFlag)
    override val arguments: Seq[Argument[?]] =
      Seq(NameArg, PortArg, PathArg, EnvArg)

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

  // Tests for argument functionality
  test("parse argument with automatic index lookup") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("alice", "9000", "/tmp/test", "dev")
    )

    assertEquals(ctx.argument(NameArg), "alice")
    assertEquals(ctx.argument(PortArg), 9000)
    assertEquals(ctx.argument(PathArg), Path.of("/tmp/test"))
    assertEquals(ctx.argument(EnvArg), "dev")
  }

  test("parse argument with explicit index") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("bob", "3000", "/tmp/data", "test")
    )

    assertEquals(ctx.argument(0, NameArg), "bob")
    assertEquals(ctx.argument(1, PortArg), 3000)
    assertEquals(ctx.argument(2, PathArg), Path.of("/tmp/data"))
    assertEquals(ctx.argument(3, EnvArg), "test")
  }

  test("use default value when argument not provided") {
    val ctx = new CommandContextImpl(TestCommandWithArgs, Seq("charlie"))

    assertEquals(ctx.argument(NameArg), "charlie")
    assertEquals(ctx.argument(PortArg), 8080) // default value
  }

  test("parse optional argument that is present") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("dana", "7000", "/tmp/foo", "prod")
    )

    assertEquals(ctx.optionalArgument(PathArg), Some(Path.of("/tmp/foo")))
    assertEquals(ctx.optionalArgument(EnvArg), Some("prod"))
  }

  test("return None for optional argument not provided") {
    val ctx = new CommandContextImpl(TestCommandWithArgs, Seq("eve"))

    assertEquals(ctx.optionalArgument(PathArg), None)
    assertEquals(ctx.optionalArgument(EnvArg), None)
  }

  test("return Some with default for optional argument with default") {
    val ctx = new CommandContextImpl(TestCommandWithArgs, Seq("frank"))

    assertEquals(ctx.optionalArgument(PortArg), Some(8080))
  }

  test("throw when required argument is missing") {
    val ctx = new CommandContextImpl(TestCommandWithArgs, Seq.empty)

    intercept[IllegalArgumentException] {
      ctx.argument(NameArg)
    }
  }

  test("validate argument options") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("grace", "5000", "/tmp", "staging")
    )

    // "staging" is not in the allowed options (dev, test, prod)
    intercept[IllegalArgumentException] {
      ctx.argument(EnvArg)
    }
  }

  test("parse valid argument option") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("henry", "5000", "/tmp", "prod")
    )

    assertEquals(ctx.argument(EnvArg), "prod")
  }

  test("throw when argument not in command's arguments sequence") {
    val ctx = new CommandContextImpl(TestCommandWithArgs, Seq("igor"))

    intercept[IllegalArgumentException] {
      ctx.argument(OrphanArg)
    }
  }

  test("strip flags before parsing arguments") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("-v", "jane", "6000", "/tmp/path", "dev")
    )

    assertEquals(ctx.argument(NameArg), "jane")
    assertEquals(ctx.argument(PortArg), 6000)
    assertEquals(ctx.argument(PathArg), Path.of("/tmp/path"))
    assertEquals(ctx.argument(EnvArg), "dev")
    assertEquals(ctx.booleanFlag(TestVerboseFlag), true)
  }

  test("parse arguments with mixed flags and args") {
    val ctx = new CommandContextImpl(
      TestCommandWithArgs,
      Seq("karl", "-v", "4000", "/tmp/mixed")
    )

    assertEquals(ctx.args, Seq("karl", "4000", "/tmp/mixed"))
    assertEquals(ctx.argument(NameArg), "karl")
    assertEquals(ctx.argument(PortArg), 4000)
    assertEquals(ctx.argument(PathArg), Path.of("/tmp/mixed"))
    assertEquals(ctx.booleanFlag(TestVerboseFlag), true)
  }
}
