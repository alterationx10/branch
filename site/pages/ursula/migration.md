---
title: Ursula Migration Guide
description: Moving from traditional to modern approach
author: Mark Rudolph
published: 2025-01-25T04:38:00Z
lastUpdated: 2025-10-15T13:50:00Z
tags:
  - cli
  - migration
---

# Migration Guide

Migrating from the traditional to modern approach is straightforward and can be done incrementally.

## Step 1: Update Flag Definitions (Optional)

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

## Step 2: Add actionWithContext

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

## Step 3: Test and Remove Old Code

Once you've tested the new implementation, remove the old `action` method. The framework will automatically use `actionWithContext`.

**Important:** All old code continues to work! You can migrate at your own pace or leave code as-is.

## Complete Migration Example

### Before (Traditional)

```scala
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.{StringFlag, IntFlag, BooleanFlag}
import dev.alteration.branch.ursula.command.Command

object ServerApp extends UrsulaApp {
  val commands = Seq(StartCommand)
}

object HostFlag extends StringFlag {
  val name = "host"
  val shortKey = "h"
  val description = "Server host"
  val default = Some("localhost")
}

object PortFlag extends IntFlag {
  val name = "port"
  val shortKey = "p"
  val description = "Server port"
  val default = Some(8080)
}

object VerboseFlag extends BooleanFlag {
  val name = "verbose"
  val shortKey = "v"
  val description = "Enable verbose logging"
}

object StartCommand extends Command {
  val trigger = "start"
  val description = "Start the server"
  val usage = "start --host <host> --port <port> [--verbose]"
  val examples = Seq(
    "start --host 0.0.0.0 --port 3000",
    "start -h localhost -p 8080 -v"
  )
  val flags = Seq(HostFlag, PortFlag, VerboseFlag)
  val arguments = Seq.empty

  def action(args: Seq[String]): Unit = {
    val host = HostFlag.parseFirstArg(args).getOrElse("localhost")
    val port = PortFlag.parseFirstArg(args).getOrElse(8080)
    val verbose = VerboseFlag.parseArgs(args)

    if (verbose) {
      println(s"Starting server with verbose logging...")
    }
    println(s"Server running at http://$host:$port")
  }
}
```

### After (Modern)

```scala
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

object ServerApp extends UrsulaApp {
  val commands = Seq(StartCommand)
}

object StartCommand extends Command {
  // Concise flag definitions
  val HostFlag = Flags.string(
    name = "host",
    shortKey = "h",
    description = "Server host",
    default = Some("localhost")
  )

  val PortFlag = Flags.int(
    name = "port",
    shortKey = "p",
    description = "Server port",
    default = Some(8080)
  )

  val VerboseFlag = Flags.boolean(
    name = "verbose",
    shortKey = "v",
    description = "Enable verbose logging"
  )

  val trigger = "start"
  val description = "Start the server"
  val usage = "start --host <host> --port <port> [--verbose]"
  val examples = Seq(
    "start --host 0.0.0.0 --port 3000",
    "start -h localhost -p 8080 -v"
  )
  val flags = Seq(HostFlag, PortFlag, VerboseFlag)
  val arguments = Seq.empty

  // Type-safe action with CommandContext
  override def actionWithContext(ctx: CommandContext): Unit = {
    val host = ctx.requiredFlag(HostFlag)
    val port = ctx.requiredFlag(PortFlag)
    val verbose = ctx.booleanFlag(VerboseFlag)

    if (verbose) {
      println(s"Starting server with verbose logging...")
    }
    println(s"Server running at http://$host:$port")
  }
}
```

## Key Differences

### Flag Access

**Before:**
```scala
val name = NameFlag.parseFirstArg(args).get // Unsafe!
val verbose = VerboseFlag.parseArgs(args) // Returns Boolean
```

**After:**
```scala
val name = ctx.requiredFlag(NameFlag) // Type-safe, no .get
val verbose = ctx.booleanFlag(VerboseFlag) // Returns Boolean
```

### Optional Flags

**Before:**
```scala
val config = ConfigFlag.parseFirstArg(args).getOrElse("default.yml")
```

**After:**
```scala
val config = ctx.flag(ConfigFlag).getOrElse("default.yml")
// Or with default already in flag definition:
val config = ctx.requiredFlag(ConfigFlag)
```

### Multiple Approaches Can Coexist

You can mix old and new styles in the same application:

```scala
object App extends UrsulaApp {
  val commands = Seq(
    OldStyleCommand,  // Uses action(args)
    NewStyleCommand   // Uses actionWithContext(ctx)
  )
}
```

The framework automatically detects which method to call.

## Benefits of Migration

1. **Type Safety**: No more `.get` calls that can throw
2. **Cleaner Code**: Less boilerplate for flag definitions
3. **Better Errors**: Clear compiler errors instead of runtime exceptions
4. **Easier Testing**: Mock `CommandContext` instead of string arrays
5. **Self-Documenting**: Intent is clearer with `requiredFlag` vs `parseFirstArg().get`

## Backward Compatibility

All traditional code continues to work:
- Flag trait extensions (StringFlag, IntFlag, BooleanFlag)
- `action(args: Seq[String])` method
- Manual parsing with `parseFirstArg`, `parseArgs`, etc.

You can migrate at your own pace, or not at all!

## Next Steps

- Learn more about [Commands and Flags](/ursula/commands-and-flags)
- Return to [Ursula Overview](/ursula)
