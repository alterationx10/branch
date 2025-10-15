# Ursula CLI Framework - Ergonomic Improvements

## Overview

The Ursula CLI framework has been enhanced with ergonomic improvements that reduce boilerplate and improve type safety, while maintaining full backward compatibility.

## New Features

### 1. CommandContext - Type-Safe Flag Access

Commands can now override `actionWithContext(ctx: CommandContext)` instead of `action(args: Seq[String])` to get automatic, type-safe access to parsed flags.

**Before:**
```scala
override def action(args: Seq[String]): Unit = {
  val port = PortFlag.parseFirstArg(args).get  // Manual parsing, unsafe .get
  val verbose = VerboseFlag.isPresent(args)    // Verbose checks

  startServer(port, verbose)
}
```

**After:**
```scala
override def actionWithContext(ctx: CommandContext): Unit = {
  val port = ctx.requiredFlag(PortFlag)     // Type-safe, no .get needed
  val verbose = ctx.booleanFlag(VerboseFlag) // Clean boolean access

  startServer(port, verbose)
}
```

### 2. Flags Factory - Concise Flag Definitions

The new `Flags` object provides factory methods for creating common flag types with minimal boilerplate.

**Before:**
```scala
object PortFlag extends IntFlag {
  override val name: String         = "port"
  override val shortKey: String     = "p"
  override val description: String  = "Server port"
  override val default: Option[Int] = Some(8080)
}

object VerboseFlag extends BooleanFlag {
  override val name: String        = "verbose"
  override val shortKey: String    = "v"
  override val description: String = "Verbose output"
}
```

**After:**
```scala
val PortFlag = Flags.int(
  name = "port",
  shortKey = "p",
  description = "Server port",
  default = Some(8080)
)

val VerboseFlag = Flags.boolean(
  name = "verbose",
  shortKey = "v",
  description = "Verbose output"
)
```

## Available Flag Factories

- `Flags.string()` - For String flags
- `Flags.int()` - For Int flags
- `Flags.boolean()` - For Boolean flags (presence/absence)
- `Flags.path()` - For Path flags with optional custom parsers
- `Flags.custom[T]()` - For custom types with user-defined parsers

## CommandContext API

- `ctx.flag[R](flag: Flag[R]): Option[R]` - Get optional flag value
- `ctx.requiredFlag[R](flag: Flag[R]): R` - Get required flag (throws if missing)
- `ctx.booleanFlag(flag: BooleanFlag): Boolean` - Check boolean flag presence
- `ctx.args: Seq[String]` - Get positional arguments (flags stripped)
- `ctx.rawArgs: Seq[String]` - Get raw arguments including flags

## Complete Example

```scala
import dev.alteration.branch.ursula.args.Flags
import dev.alteration.branch.ursula.command.{Command, CommandContext}

object ServeCommand extends Command {
  // Concise flag definitions
  val PortFlag = Flags.int("port", "p", "Server port", default = Some(8080))
  val HostFlag = Flags.string("host", "h", "Server host", default = Some("localhost"))
  val VerboseFlag = Flags.boolean("verbose", "v", "Enable verbose logging")

  override val trigger = "serve"
  override val description = "Start the HTTP server"
  override val usage = "serve [options]"
  override val examples = Seq("serve -p 3000", "serve --host 0.0.0.0 --verbose")
  override val flags = Seq(PortFlag, HostFlag, VerboseFlag)
  override val arguments = Seq.empty

  // Type-safe action with CommandContext
  override def actionWithContext(ctx: CommandContext): Unit = {
    val port = ctx.requiredFlag(PortFlag)
    val host = ctx.requiredFlag(HostFlag)
    val verbose = ctx.booleanFlag(VerboseFlag)

    if (verbose) println(s"Starting server on $host:$port")

    startServer(host, port, verbose)
  }
}
```

## Backward Compatibility

All existing commands continue to work without modification:
- Old commands using `action(args: Seq[String])` work as before
- Old flag definitions using `extends Flag[R]` work as before
- Default implementation of `actionWithContext` delegates to `action`
- No breaking changes to existing APIs

## Migration Path

Commands can be migrated incrementally:
1. Keep existing `action` method while adding `actionWithContext`
2. Test the new implementation
3. Remove old `action` method once satisfied
4. Optionally refactor flag definitions to use `Flags` factories

## Benefits

- ✅ **Reduced Boilerplate** - Less code for flag definitions
- ✅ **Type Safety** - No unsafe `.get` calls
- ✅ **Better Readability** - Intent is clearer
- ✅ **Easier Testing** - CommandContext can be mocked
- ✅ **Backward Compatible** - Adopt at your own pace
- ✅ **Zero Dependencies** - All improvements use standard library only
