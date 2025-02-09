---
title: Ursula
description: A slim framework to make Scala CLI apps.
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-01-25T04:38:00Z
tags: 
  - cli
---

# Ursula

A slim framework to make Scala CLI apps.

## Anatomy of the Framework

Here is a general overview of how the pieces fit together.

### How it works: UrsulaApp

You only need to make an object that extends the `UrsulaApp` trait, and provide
a `Seq[Command]`, which are your actions you wish to be available in your
app. `UrsulaApp` has a final `main` method entrypoint, and does some processing automatically.
It parses the arguments passed, and uses that to pull out the `Command` provided, and runs
accordingly, passing on the arguments to it.

There are some [built in commands](#built-in-commands) provided, currently only the
`HelpCommand`, that are also automatically injected. This
means that even if you only have:

```scala
object App extends UrsulaApp {
  override val commands: Seq[Command] = Seq.empty
}
```

you already have a functioning cli app that has a `help` command that prints all
the available commands accepted (as little as they are so far).

At this point, you need only implement some `Command`s that wrap the
functionality you desire, and add them to the `commands: Seq`.

### Commands

Commands are the primary building blocks. Each command can have flags and arguments:

```scala
trait Command {
  // Required fields
  val trigger: String        // How to invoke the command
  val description: String    // Brief description
  val usage: String         // Usage pattern
  val examples: Seq[String] // Example usages
  
  val flags: Seq[Flag[?]]           // Command flags
  val arguments: Seq[Argument[?]]   // Positional arguments
  
  // Core implementation
  def action(args: Seq[String]): Unit
  
  // Optional settings
  val strict: Boolean = true    // Enforce flag validation
  val hidden: Boolean = false   // Hide from help
  val isDefaultCommand: Boolean = false
}
```

#### Built-In Commands

- HelpCommand - handles the printing of documentation

### Flags

Flags (`trait Flag[R]`) are non-positional arguments passed to the command.
They can be used in two ways:
- Argument flags which expect a value of type `R`
- Boolean flags which are simply present/not present

Here's an example of a string flag:

```scala
object ConfigFlag extends StringFlag {
  val name: String = "config"        // Used as --config
  val shortKey: String = "c"         // Used as -c
  val description: String = "Config file path"
  
  // Optional settings
  val required: Boolean = false
  val expectsArgument: Boolean = true
  val multiple: Boolean = false      // Can be used multiple times
  val env: Option[String] = Some("CONFIG_PATH")
  val default: Option[String] = Some("config.yml")
  val options: Option[Set[String]] = Some(Set("dev", "prod"))
  
  // Dependencies and conflicts
  val dependsOn: Option[Seq[Flag[?]]] = Some(Seq(OtherFlag))
  val exclusive: Option[Seq[Flag[?]]] = Some(Seq(ConflictingFlag))
}
```

Built-in flag types:
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

## Example Usage

Here's a complete example:

```scala
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

You can run it like:

```bash
myapp greet --name World
myapp greet -n Alice
myapp help
myapp greet --help
```