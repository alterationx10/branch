package dev.alteration.branch.ursula.command

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.ursula.args.{
  Argument,
  Arguments,
  Flags,
  StringFlag
}
import munit.*

import scala.concurrent.ExecutionContext

class CommandsSpec extends FunSuite {
  given ExecutionContext = BranchExecutors.executionContext

  val testFlag: StringFlag      =
    Flags.string("name", "n", "Test flag", required = true)
  val testArg: Argument[String] = Arguments.string("message", "Test argument")

  // Inline Factory Tests

  test("Commands.create creates command with all properties") {
    val cmd = Commands.create(
      trigger = "test",
      description = "Test command",
      usage = "test -n <name>",
      examples = Seq("test -n Alice", "test -n Bob"),
      flags = Seq(testFlag),
      arguments = Seq(testArg),
      strict = false,
      hidden = true,
      isDefaultCommand = true
    ) { _ =>
      // test action
    }

    assertEquals(cmd.trigger, "test")
    assertEquals(cmd.description, "Test command")
    assertEquals(cmd.usage, "test -n <name>")
    assertEquals(cmd.examples, Seq("test -n Alice", "test -n Bob"))
    assertEquals(cmd.flags, Seq(testFlag))
    assertEquals(cmd.arguments, Seq(testArg))
    assertEquals(cmd.strict, false)
    assertEquals(cmd.hidden, true)
    assertEquals(cmd.isDefaultCommand, true)
  }

  test("Commands.create uses trigger as default usage") {
    val cmd = Commands.create(
      trigger = "test",
      description = "Test command"
    ) { _ => }

    assertEquals(cmd.usage, "test")
  }

  test("Commands.create defaults to empty collections") {
    val cmd = Commands.create(
      trigger = "test",
      description = "Test command"
    ) { _ => }

    assertEquals(cmd.examples, Seq.empty)
    assertEquals(cmd.flags, Seq.empty)
    assertEquals(cmd.arguments, Seq.empty)
  }

  test("Commands.create defaults to strict mode") {
    val cmd = Commands.create(
      trigger = "test",
      description = "Test command"
    ) { _ => }

    assertEquals(cmd.strict, true)
    assertEquals(cmd.hidden, false)
    assertEquals(cmd.isDefaultCommand, false)
  }

  test("Commands.create action is executed") {
    var executed = false
    val cmd      = Commands.create(
      trigger = "test",
      description = "Test command"
    ) { _ =>
      executed = true
    }

    val ctx = new CommandContextImpl(cmd, Seq.empty)
    cmd.actionWithContext(ctx)

    assert(executed)
  }

  test("Commands.simple creates command with minimal config") {
    val cmd = Commands.simple(
      trigger = "simple",
      description = "Simple command",
      flags = Seq(testFlag)
    ) { _ => }

    assertEquals(cmd.trigger, "simple")
    assertEquals(cmd.description, "Simple command")
    assertEquals(cmd.usage, "simple")
    assertEquals(cmd.flags, Seq(testFlag))
    assertEquals(cmd.examples, Seq.empty)
    assertEquals(cmd.strict, true)
  }

  test("Commands.simple with no flags or arguments") {
    val cmd = Commands.simple("simple", "Simple command") { _ => }

    assertEquals(cmd.flags, Seq.empty)
    assertEquals(cmd.arguments, Seq.empty)
  }

  // Builder Pattern Tests

  test("Commands.builder creates builder instance") {
    val builder = Commands.builder("test")
    assert(builder != null)
    assert(builder.isInstanceOf[CommandBuilder])
  }

  test("CommandBuilder builds command with all properties") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .usage("test -n <name>")
      .example("test -n Alice")
      .example("test -n Bob")
      .withFlags(testFlag)
      .withArguments(testArg)
      .strict(false)
      .hidden()
      .asDefault()
      .action { _ => }
      .build()

    assertEquals(cmd.trigger, "test")
    assertEquals(cmd.description, "Test command")
    assertEquals(cmd.usage, "test -n <name>")
    assertEquals(cmd.examples, Seq("test -n Alice", "test -n Bob"))
    assertEquals(cmd.flags, Seq(testFlag))
    assertEquals(cmd.arguments, Seq(testArg))
    assertEquals(cmd.strict, false)
    assertEquals(cmd.hidden, true)
    assertEquals(cmd.isDefaultCommand, true)
  }

  test("CommandBuilder.example adds examples incrementally") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .example("example 1")
      .example("example 2")
      .example("example 3")
      .action { _ => }
      .build()

    assertEquals(cmd.examples, Seq("example 1", "example 2", "example 3"))
  }

  test("CommandBuilder.examples sets all examples at once") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .examples(Seq("example 1", "example 2"))
      .action { _ => }
      .build()

    assertEquals(cmd.examples, Seq("example 1", "example 2"))
  }

  test("CommandBuilder.withFlags adds flags incrementally") {
    val flag1 = Flags.string("flag1", "f1", "Flag 1")
    val flag2 = Flags.string("flag2", "f2", "Flag 2")
    val flag3 = Flags.string("flag3", "f3", "Flag 3")

    val cmd = Commands
      .builder("test")
      .description("Test command")
      .withFlags(flag1)
      .withFlags(flag2, flag3)
      .action { _ => }
      .build()

    assertEquals(cmd.flags, Seq(flag1, flag2, flag3))
  }

  test("CommandBuilder.flags replaces flags") {
    val flag1 = Flags.string("flag1", "f1", "Flag 1")
    val flag2 = Flags.string("flag2", "f2", "Flag 2")

    val cmd = Commands
      .builder("test")
      .description("Test command")
      .withFlags(flag1)
      .flags(Seq(flag2))
      .action { _ => }
      .build()

    assertEquals(cmd.flags, Seq(flag2))
  }

  test("CommandBuilder.withArguments adds arguments incrementally") {
    val arg1 = Arguments.string("arg1", "Argument 1")
    val arg2 = Arguments.string("arg2", "Argument 2")

    val cmd = Commands
      .builder("test")
      .description("Test command")
      .withArguments(arg1)
      .withArguments(arg2)
      .action { _ => }
      .build()

    assertEquals(cmd.arguments, Seq(arg1, arg2))
  }

  test("CommandBuilder.arguments replaces arguments") {
    val arg1 = Arguments.string("arg1", "Argument 1")
    val arg2 = Arguments.string("arg2", "Argument 2")

    val cmd = Commands
      .builder("test")
      .description("Test command")
      .withArguments(arg1)
      .arguments(Seq(arg2))
      .action { _ => }
      .build()

    assertEquals(cmd.arguments, Seq(arg2))
  }

  test("CommandBuilder.visible sets hidden to false") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .hidden()
      .visible()
      .action { _ => }
      .build()

    assertEquals(cmd.hidden, false)
  }

  test("CommandBuilder throws if description not set") {
    intercept[IllegalStateException] {
      Commands
        .builder("test")
        .action { _ => }
        .build()
    }
  }

  test("CommandBuilder throws if action not set") {
    intercept[IllegalStateException] {
      Commands
        .builder("test")
        .description("Test command")
        .build()
    }
  }

  test("CommandBuilder defaults to trigger as usage") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .action { _ => }
      .build()

    assertEquals(cmd.usage, "test")
  }

  test("CommandBuilder defaults to strict mode") {
    val cmd = Commands
      .builder("test")
      .description("Test command")
      .action { _ => }
      .build()

    assertEquals(cmd.strict, true)
    assertEquals(cmd.hidden, false)
    assertEquals(cmd.isDefaultCommand, false)
  }

  // Integration Tests

  test("factory-created command can be executed via lazyAction") {
    var executed = false
    val cmd      = Commands.simple("test", "Test command") { _ =>
      executed = true
    }

    val result = cmd.lazyAction(Seq.empty).runSync()

    assert(result.isSuccess)
    assert(executed)
  }

  test("builder-created command can be executed via lazyAction") {
    var executed = false
    val cmd      = Commands
      .builder("test")
      .description("Test command")
      .action { _ =>
        executed = true
      }
      .build()

    val result = cmd.lazyAction(Seq.empty).runSync()

    assert(result.isSuccess)
    assert(executed)
  }

  test("factory-created command handles flags correctly") {
    val nameFlag = Flags.string("name", "n", "Name", required = true)
    var captured = ""

    val cmd = Commands.create(
      trigger = "greet",
      description = "Greet someone",
      flags = Seq(nameFlag)
    ) { ctx =>
      captured = ctx.requiredFlag(nameFlag)
    }

    val result = cmd.lazyAction(Seq("-n", "Alice")).runSync()

    assert(result.isSuccess)
    assertEquals(captured, "Alice")
  }

  test("builder-created command handles arguments correctly") {
    val messageArg = Arguments.string("message", "Message")
    var captured   = ""

    val cmd = Commands
      .builder("echo")
      .description("Echo a message")
      .withArguments(messageArg)
      .action { ctx =>
        captured = ctx.argument(messageArg)
      }
      .build()

    val result = cmd.lazyAction(Seq("Hello")).runSync()

    assert(result.isSuccess)
    assertEquals(captured, "Hello")
  }

}
