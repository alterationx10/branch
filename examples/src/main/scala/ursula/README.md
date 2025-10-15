# Ursula CLI Framework Examples

This directory contains examples demonstrating the Ursula CLI framework.

There has been a bit of refactor to make using the piece a little more ergonomic. The "basic" examples show the original
way, and the "modern" way is the refactored way. The "old way" should be backwards compatible to make migration easier.

## Examples

### 1. Basic CLI (`basic/BasicCliExample.scala`)

**What it demonstrates:**

- Traditional flag definitions (extends IntFlag, StringFlag, BooleanFlag)
- Manual flag parsing with `.parseFirstArg()` and `.isPresent()`
- Single command CLI application

**Why it's useful:**

- Shows the original API (still fully supported)
- Good reference for migrating existing code
- Demonstrates that old code continues to work

**Run it:**

```bash
sbt "examples/runMain ursula.basic.BasicCliExample greet -n Alice"
sbt "examples/runMain ursula.basic.BasicCliExample greet -n Bob --loud"
sbt "examples/runMain ursula.basic.BasicCliExample greet -n Charlie -r 3"
sbt "examples/runMain ursula.basic.BasicCliExample help"
```

### 2. Modern CLI (`advanced/ModernCliExample.scala`)

**What it demonstrates:**

- Concise flag definitions using `Flags` factory
- Type-safe flag access via `CommandContext`
- Multiple commands in one CLI
- Boolean flags for operation selection

**Why it's useful:**

- Shows the recommended modern approach
- Eliminates unsafe `.get` calls
- Cleaner, more readable code
- Better type safety

**Run it:**

```bash
# Greeting command
sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Alice"
sbt "examples/runMain ursula.advanced.ModernCliExample greet -n Bob --loud -r 2"

# Math command
sbt "examples/runMain ursula.advanced.ModernCliExample math --add 5 10 15"
sbt "examples/runMain ursula.advanced.ModernCliExample math --multiply 2 3 4"
sbt "examples/runMain ursula.advanced.ModernCliExample math -m 7 8 --verbose"
```

### 3. File Tools (`filetools/FileToolsExample.scala`)

**What it demonstrates:**

- Path flags with custom parsers
- Custom flag types (enums)
- Options validation
- Real-world file operations
- Multiple related commands

**Why it's useful:**

- Practical example of a complete CLI tool
- Shows how to work with file system operations
- Demonstrates custom type parsers
- Error handling patterns

**Run it:**

```bash
# List files
sbt "examples/runMain ursula.filetools.FileToolsExample list"
sbt "examples/runMain ursula.filetools.FileToolsExample list -d /tmp"
sbt "examples/runMain ursula.filetools.FileToolsExample list -d . --recursive"

# Count files by type
sbt "examples/runMain ursula.filetools.FileToolsExample count"
sbt "examples/runMain ursula.filetools.FileToolsExample count -d src"
sbt "examples/runMain ursula.filetools.FileToolsExample count -d . --type scala"

# Find files by pattern
sbt "examples/runMain ursula.filetools.FileToolsExample find -p *.scala"
sbt "examples/runMain ursula.filetools.FileToolsExample find -d examples -p *Example*.scala"
sbt "examples/runMain ursula.filetools.FileToolsExample find -p *.md --verbose"
```

## Key Concepts

### Flag Factories (Modern Approach)

Instead of defining flags as objects extending flag traits:

```scala
// Old way (still works)
object PortFlag extends IntFlag {
  override val name = "port"
  override val shortKey = "p"
  override val description = "Server port"
  override val default = Some(8080)
}
```

Use the `Flags` factory for concise definitions:

```scala
// New way (recommended)
val PortFlag = Flags.int(
  name = "port",
  shortKey = "p",
  description = "Server port",
  default = Some(8080)
)
```

Available factories:

- `Flags.string()` - For String values
- `Flags.int()` - For Int values
- `Flags.boolean()` - For presence/absence flags
- `Flags.path()` - For Path values with optional custom parser
- `Flags.custom[T]()` - For any type with custom parser

### CommandContext (Modern Approach)

Instead of manual flag parsing:

```scala
// Old way (still works)
override def action(args: Seq[String]): Unit = {
  val port = PortFlag.parseFirstArg(args).get // Unsafe!
  val verbose = VerboseFlag.isPresent(args)
  // ...
}
```

Use `CommandContext` for type-safe access:

```scala
// New way (recommended)
override def actionWithContext(ctx: CommandContext): Unit = {
  val port = ctx.requiredFlag(PortFlag) // Safe!
  val verbose = ctx.booleanFlag(VerboseFlag)
  val host = ctx.flag(HostFlag) // Optional
  val positional = ctx.args // Non-flag args
  // ...
}
```

CommandContext API:

- `ctx.requiredFlag[R](flag)` - Get required flag (throws if missing)
- `ctx.flag[R](flag)` - Get optional flag value
- `ctx.booleanFlag(flag)` - Check boolean flag presence
- `ctx.args` - Get positional arguments (flags stripped)
- `ctx.rawArgs` - Get all arguments including flags

## Migration Guide

To migrate existing commands:

1. **Update flag definitions** (optional but recommended):
   ```scala
   // Before
   object MyFlag extends StringFlag { ... }

   // After
   val MyFlag = Flags.string(...)
   ```

2. **Add actionWithContext** (keep old action initially):
   ```scala
   override def actionWithContext(ctx: CommandContext): Unit = {
     // New implementation
   }

   override def action(args: Seq[String]): Unit = {
     // Keep old implementation temporarily
   }
   ```

3. **Test and remove old action**:
   Once satisfied, remove the old `action` method.

## Best Practices

1. **Use CommandContext**: Prefer `actionWithContext` over `action` for type safety
2. **Use Flags factories**: Reduce boilerplate with concise flag definitions
3. **Validate early**: Check for invalid inputs at the start of `actionWithContext`
4. **Provide good help**: Set descriptive `description`, `usage`, and `examples`
5. **Handle errors gracefully**: Print clear error messages and return early
6. **Use defaults wisely**: Provide sensible defaults for optional flags

## Further Reading

- See `branch/src/main/scala/dev/alteration/branch/ursula/IMPROVEMENTS.md` for detailed documentation
- Check the tests in `branch/src/test/scala/dev/alteration/branch/ursula/` for more examples
- All examples maintain zero external dependencies (standard library only)
