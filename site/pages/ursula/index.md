---
title: Ursula
description: A slim framework to make Scala CLI apps.
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-10-15T13:50:00Z
tags:
  - cli
---

# Ursula

A slim, ergonomic framework to make Scala CLI apps with zero external dependencies.

## Quick Start (Modern Approach)

After a bit of a refactor, Ursula now offers an ergonomic, type-safe API for building CLI apps. This doc has a bit of
both "the old way" and "the new way", and currently highlights some of the features of "the new way".

Here's a complete example:

```scala
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

object GreetApp extends UrsulaApp {
  val commands = Seq(GreetCommand)
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

  val trigger = "greet"
  val description = "Greets someone"
  val usage = "greet --name <name> [--loud]"
  val examples = Seq(
    "greet --name World",
    "greet -n Alice --loud"
  )
  val flags = Seq(NameFlag, LoudFlag)
  val arguments = Seq.empty

  // Type-safe action using CommandContext
  override def actionWithContext(ctx: CommandContext): Unit = {
    val name = ctx.requiredFlag(NameFlag) // No .get needed!
    val loud = ctx.booleanFlag(LoudFlag)

    val greeting = if (loud) s"HELLO, ${name.toUpperCase}!" else s"Hello, $name!"
    println(greeting)
  }
}
```

Run it:

```bash
myapp greet --name World     # Hello, World!
myapp greet -n Alice --loud  # HELLO, ALICE!
myapp help                   # Show all commands
myapp greet --help           # Show greet command help
```

**Key Benefits:**

- ✅ **Concise**: Flag definitions using `Flags` factory methods
- ✅ **Type-Safe**: No unsafe `.get` calls with `CommandContext`
- ✅ **Clean**: `ctx.requiredFlag()`, `ctx.booleanFlag()`, `ctx.flag()`
- ✅ **Compatible**: Old code continues to work

See the [examples](https://github.com/alterationx10/branch/tree/main/examples/src/main/scala/ursula) for more!

## Anatomy of the Framework

Here is a general overview of how the pieces fit together.

### How it works: UrsulaApp

You only need to make an object that extends the `UrsulaApp` trait, and provide a `Seq[Command]`, which are your actions
you wish to be available in your app. `UrsulaApp` has a final `main` method entrypoint, and does some processing
automatically. It parses the arguments passed, and uses that to pull out the `Command` provided, and runs accordingly,
passing on the arguments to it.

There are some [built in commands](#built-in-commands) provided, currently only the
`HelpCommand`, that are also automatically injected. This means that even if you only have:

```scala
object App extends UrsulaApp {
  override val commands: Seq[Command] = Seq.empty
}
```

you already have a functioning cli app that has a `help` command that prints all the available commands accepted (as
little as they are so far).

At this point, you need only implement some `Command`s that wrap the functionality you desire, and add them to the
`commands: Seq`.

### Commands

Commands are the primary building blocks. Each command can have flags and arguments:

```scala
trait Command {
  // Required fields
  val trigger: String // How to invoke the command
  val description: String // Brief description
  val usage: String // Usage pattern
  val examples: Seq[String] // Example usages

  val flags: Seq[Flag[?]] // Command flags
  val arguments: Seq[Argument[?]] // Positional arguments

  // Core implementation (choose one)
  def actionWithContext(ctx: CommandContext): Unit // Modern (recommended)

  def action(args: Seq[String]): Unit // Traditional (still works)

  // Optional settings
  val strict: Boolean = true // Enforce flag validation
  val hidden: Boolean = false // Hide from help
  val isDefaultCommand: Boolean = false
}
```

#### Modern Approach: CommandContext

The recommended way to implement commands is using `actionWithContext`, which provides type-safe access to parsed flags:

```scala
override def actionWithContext(ctx: CommandContext): Unit = {
  // Type-safe flag access - no .get needed!
  val port = ctx.requiredFlag(PortFlag) // Required flags
  val verbose = ctx.booleanFlag(VerboseFlag) // Boolean flags
  val host = ctx.flag(HostFlag) // Optional flags
  val positional = ctx.args // Positional arguments

  // Your command logic here
}
```

**CommandContext API:**

- `ctx.requiredFlag[R](flag: Flag[R]): R` - Get required flag (throws if missing)
- `ctx.flag[R](flag: Flag[R]): Option[R]` - Get optional flag value
- `ctx.booleanFlag(flag: BooleanFlag): Boolean` - Check boolean flag presence
- `ctx.args: Seq[String]` - Get positional arguments (flags stripped)
- `ctx.rawArgs: Seq[String]` - Get all arguments including flags

**Benefits:**

- ✅ No unsafe `.get` calls
- ✅ Clear intent with named methods
- ✅ Automatic flag parsing
- ✅ Better testability (can mock CommandContext)

#### Built-In Commands

- HelpCommand - handles the printing of documentation

### Flags

Flags (`trait Flag[R]`) are non-positional arguments passed to the command. They can be used in two ways:

- Argument flags which expect a value of type `R`
- Boolean flags which are simply present/not present

#### Modern Approach: Flags Factory (Recommended)

The `Flags` object provides factory methods for creating flags with minimal boilerplate:

```scala
import dev.alteration.branch.ursula.args.Flags

// String flag
val ConfigFlag = Flags.string(
  name = "config",
  shortKey = "c",
  description = "Config file path",
  default = Some("config.yml"),
  required = false,
  options = Some(Set("dev", "prod"))
)

// Integer flag
val PortFlag = Flags.int(
  name = "port",
  shortKey = "p",
  description = "Server port",
  default = Some(8080)
)

// Boolean flag (presence/absence)
val VerboseFlag = Flags.boolean(
  name = "verbose",
  shortKey = "v",
  description = "Enable verbose output"
)

// Path flag with custom parser
val DirFlag = Flags.path(
  name = "dir",
  shortKey = "d",
  description = "Working directory",
  default = Some(Path.of(".")),
  parser = s => Path.of(s).toAbsolutePath
)

// Custom type flag
val LogLevelFlag = Flags.custom[LogLevel](
  name = "log-level",
  shortKey = "l",
  description = "Log level",
  parser = {
    case "debug" => LogLevel.Debug
    case "info" => LogLevel.Info
    case "error" => LogLevel.Error
  },
  default = Some(LogLevel.Info)
)
```

**Available factory methods:**

- `Flags.string()` - String values
- `Flags.int()` - Integer values
- `Flags.boolean()` - Presence/absence flags
- `Flags.path()` - Path values with optional custom parser
- `Flags.custom[T]()` - Any type with custom parser

#### Traditional Approach: Extending Flag Traits

You can still define flags by extending flag traits (fully supported):

```scala
object ConfigFlag extends StringFlag {
  val name: String = "config" // Used as --config
  val shortKey: String = "c" // Used as -c
  val description: String = "Config file path"

  // Optional settings
  val required: Boolean = false
  val expectsArgument: Boolean = true
  val multiple: Boolean = false // Can be used multiple times
  val env: Option[String] = Some("CONFIG_PATH")
  val default: Option[String] = Some("config.yml")
  val options: Option[Set[String]] = Some(Set("dev", "prod"))

  // Dependencies and conflicts
  val dependsOn: Option[Seq[Flag[?]]] = Some(Seq(OtherFlag))
  val exclusive: Option[Seq[Flag[?]]] = Some(Seq(ConflictingFlag))
}
```

Built-in flag traits:

- `BooleanFlag`: Simple presence/absence flags
- `StringFlag`: String value flags
- `IntFlag`: Integer value flags

### Arguments

Arguments (`trait Argument[R]`) are positional parameters that are parsed to type `R`:

```scala
object CountArg extends Argument[Int] {
  val name: String = "count"
  val description: String = "Number of items"
  val required: Boolean = true

  def parse: PartialFunction[String, Int] = {
    case s => s.toInt
  }

  val options: Option[Set[Int]] = Some(Set(1, 2, 3))
  val default: Option[Int] = Some(1)
}
```

## Value Resolution

For flags that take values, the resolution order is:

1. Command line argument
2. Environment variable (if configured)
3. Default value (if provided)

## Complete Examples

### Modern Example (Recommended)

Using the ergonomic API with `Flags` factory and `CommandContext`:

```scala
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

object GreetApp extends UrsulaApp {
  val commands = Seq(GreetCommand)
}

object GreetCommand extends Command {
  // Concise flag definitions
  val NameFlag = Flags.string(
    name = "name",
    shortKey = "n",
    description = "Name to greet",
    required = true
  )

  val RepeatFlag = Flags.int(
    name = "repeat",
    shortKey = "r",
    description = "Number of times to repeat",
    default = Some(1)
  )

  val trigger = "greet"
  val description = "Greets someone"
  val usage = "greet --name <name> [--repeat <n>]"
  val examples = Seq(
    "greet --name World",
    "greet -n Alice -r 3"
  )
  val flags = Seq(NameFlag, RepeatFlag)
  val arguments = Seq.empty

  // Type-safe action
  override def actionWithContext(ctx: CommandContext): Unit = {
    val name = ctx.requiredFlag(NameFlag) // No .get!
    val repeat = ctx.requiredFlag(RepeatFlag)

    (1 to repeat).foreach { i =>
      println(s"$i. Hello, $name!")
    }
  }
}
```

### Traditional Example

The traditional approach still works perfectly:

```scala
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.StringFlag
import dev.alteration.branch.ursula.command.Command

object GreetApp extends UrsulaApp {
  val commands = Seq(GreetCommand)
}

object NameFlag extends StringFlag {
  val name = "name"
  val shortKey = "n"
  val description = "Name to greet"
  val required = true
}

object GreetCommand extends Command {
  val trigger = "greet"
  val description = "Greets someone"
  val usage = "greet --name <name>"
  val examples = Seq(
    "greet --name World",
    "greet -n Alice"
  )
  val flags = Seq(NameFlag)
  val arguments = Seq.empty

  def action(args: Seq[String]): Unit = {
    val name = NameFlag.parseFirstArg(args).get
    println(s"Hello, $name!")
  }
}
```

### Running Your CLI

```bash
myapp greet --name World
myapp greet -n Alice
myapp help                # Show all commands
myapp greet --help        # Show greet command help
```

## Migration Guide

Migrating from the traditional to modern approach is straightforward and can be done incrementally:

### Step 1: Update Flag Definitions (Optional)

Replace flag object definitions with `Flags` factory methods:

```scala
// Before
object PortFlag extends IntFlag {
  val name = "port"
  val shortKey = "p"
  val description = "Server port"
  val default = Some(8080)
}

// After
val PortFlag = Flags.int(
  name = "port",
  shortKey = "p",
  description = "Server port",
  default = Some(8080)
)
```

### Step 2: Add actionWithContext

Keep your old `action` method initially and add the new one:

```scala
object MyCommand extends Command {
  // ... flags, trigger, etc. ...

  // New implementation
  override def actionWithContext(ctx: CommandContext): Unit = {
    val port = ctx.requiredFlag(PortFlag)
    val verbose = ctx.booleanFlag(VerboseFlag)
    // Use flags safely
  }

  // Keep old implementation temporarily
  override def action(args: Seq[String]): Unit = {
    // Old implementation
  }
}
```

### Step 3: Test and Remove Old Code

Once you've tested the new implementation, remove the old `action` method. The framework will automatically use
`actionWithContext`.

**Important:** All old code continues to work! You can migrate at your own pace or leave code as-is.

## More Examples

Check out the [examples directory](https://github.com/alterationx10/branch/tree/main/examples/src/main/scala/ursula)
for:

- **BasicCliExample** - Traditional approach (for comparison)
- **ModernCliExample** - Ergonomic approach with multiple commands
- **FileToolsExample** - Practical file processing tool with custom types

Each example is fully documented and runnable with `sbt`.