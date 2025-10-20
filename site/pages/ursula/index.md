---
title: Ursula
description: A slim framework to make Scala CLI apps
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-10-15T13:50:00Z
tags:
  - cli
---

# Ursula

A slim, ergonomic framework to make Scala CLI apps with zero external dependencies.

## Quick Start (Modern Approach)

After a bit of a refactor, Ursula now offers an ergonomic, type-safe API for building CLI apps. Here's a complete example:

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

## Key Benefits

- ✅ **Concise**: Flag definitions using `Flags` factory methods
- ✅ **Type-Safe**: No unsafe `.get` calls with `CommandContext`
- ✅ **Clean**: `ctx.requiredFlag()`, `ctx.booleanFlag()`, `ctx.flag()`
- ✅ **Compatible**: Old code continues to work

## Documentation

- **[Commands and Flags](commands-and-flags.md)** - Building commands, using flags and arguments, CommandContext
- **[Migration Guide](migration.md)** - Moving from traditional to modern approach

## Anatomy of the Framework

Here is a general overview of how the pieces fit together.

### How it works: UrsulaApp

You only need to make an object that extends the `UrsulaApp` trait, and provide a `Seq[Command]`, which are your actions you wish to be available in your app. `UrsulaApp` has a final `main` method entrypoint, and does some processing automatically. It parses the arguments passed, and uses that to pull out the `Command` provided, and runs accordingly, passing on the arguments to it.

There are some built-in commands provided, currently only the `HelpCommand`, that are also automatically injected. This means that even if you only have:

```scala
object App extends UrsulaApp {
  override val commands: Seq[Command] = Seq.empty
}
```

you already have a functioning cli app that has a `help` command that prints all the available commands accepted (as little as they are so far).

At this point, you need only implement some `Command`s that wrap the functionality you desire, and add them to the `commands: Seq`.

## Running Your CLI

```bash
myapp greet --name World
myapp greet -n Alice
myapp help                # Show all commands
myapp greet --help        # Show greet command help
```

## More Examples

Check out the [examples directory](https://github.com/alterationx10/branch/tree/main/examples/src/main/scala/ursula) for:

- **BasicCliExample** - Traditional approach (for comparison)
- **ModernCliExample** - Ergonomic approach with multiple commands
- **FileToolsExample** - Practical file processing tool with custom types

Each example is fully documented and runnable with `sbt`.

## Next Steps

- Learn about [Commands and Flags](commands-and-flags.md)
- Read the [Migration Guide](migration.md) if updating existing code
