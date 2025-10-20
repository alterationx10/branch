---
title: Ursula Commands and Flags
description: Building commands, using flags and arguments, CommandContext
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-10-15T13:50:00Z
tags:
  - cli
  - commands
  - flags
---

# Commands and Flags

## Commands

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

## Modern Approach: CommandContext

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

### CommandContext API

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

## Built-In Commands

- **HelpCommand** - handles the printing of documentation

## Flags

Flags (`trait Flag[R]`) are non-positional arguments passed to the command. They can be used in two ways:

- Argument flags which expect a value of type `R`
- Boolean flags which are simply present/not present

### Modern Approach: Flags Factory (Recommended)

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

### Traditional Approach: Extending Flag Traits

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

## Arguments

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

## Next Steps

- Read the [Migration Guide](/ursula/migration) to update existing code
- Return to [Ursula Overview](/ursula)
