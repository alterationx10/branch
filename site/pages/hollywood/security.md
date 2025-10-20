---
title: Hollywood Security
description: ToolPolicy and RestrictedExecutor for safe tool execution
author: Mark Rudolph
published: 2025-10-03T00:00:00Z
lastUpdated: 2025-10-03T00:00:00Z
tags:
  - llm
  - security
  - tools
---

# Security

Hollywood provides `ToolPolicy` and `RestrictedExecutor` for validating and restricting tool execution based on custom rules.

## ToolPolicy

A `ToolPolicy[T]` defines validation and transformation rules for tools:

```scala
trait ToolPolicy[T <: CallableTool[?]] {
  def validate(tool: T): Try[Unit]

  def transformArgs(args: Json): Json = args
}
```

### Built-in Policies

```scala
// Allow all operations
val permissive = ToolPolicy.allowAll[Calculator]

// Block all operations
val restrictive = ToolPolicy.denyAll[Calculator](
  reason = "Calculator operations disabled"
)

// Custom validation
val policy = ToolPolicy.fromValidator[Calculator] { calc =>
  if (calc.a < 0 || calc.b < 0) {
    Failure(new SecurityException("Negative numbers not allowed"))
  } else {
    Success(())
  }
}

// Custom validation with argument transformation
val sanitizingPolicy = ToolPolicy.custom[Calculator](
  validator = calc => Success(()),
  transformer = args => {
    // Modify args before validation/execution
    args
  }
)
```

### Use Cases

- Restrict tool operations based on input values
- Prevent access to sensitive resources
- Enforce business rules
- Sanitize or transform inputs before execution

## RestrictedExecutor

`RestrictedExecutor` wraps a `ToolExecutor` to enforce a policy:

```scala
val baseExecutor = ToolExecutor.derived[Calculator]
val policy = ToolPolicy.fromValidator[Calculator] { calc =>
  if (calc.a > 1000 || calc.b > 1000) {
    Failure(new SecurityException("Numbers too large"))
  } else {
    Success(())
  }
}

val restrictedExecutor = RestrictedExecutor(baseExecutor, policy)
```

### Execution Flow

When executing tools, the `RestrictedExecutor`:

1. Applies the policy's `transformArgs` to the JSON input
2. Decodes the transformed arguments into the tool instance
3. Validates the tool against the policy
4. If validation passes, executes the tool with the delegate executor
5. If validation fails, returns a policy violation error

## Example with ToolRegistry

```scala
val calculator = ToolExecutor.derived[Calculator]
val policy = ToolPolicy.fromValidator[Calculator] { calc =>
  if (calc.a < 0 || calc.b < 0) {
    Failure(new SecurityException("Negative numbers not allowed"))
  } else {
    Success(())
  }
}

val restricted = RestrictedExecutor(calculator, policy)

val toolRegistry = ToolRegistry()
  .register(ToolSchema.derive[Calculator], restricted)

val agent = OneShotAgent(
  systemPrompt = "You are a math assistant.",
  toolRegistry = Some(toolRegistry)
)

// This will succeed
agent.chat("What is 5 plus 3?")

// This will fail with policy violation
agent.chat("What is -5 plus 3?")
```

## FileSystem Security Example

When using FileSystemTool, use the provided `FileSystemPolicy` to restrict access:

```scala
import dev.alteration.branch.hollywood.tools.provided.fs.{FileSystemTool, FileSystemPolicy}
import dev.alteration.branch.hollywood.tools.{ToolExecutor, RestrictedExecutor}
import java.nio.file.Paths

// Create a sandboxed filesystem policy
val policy = FileSystemPolicy.strict(Paths.get("/tmp"))
// or FileSystemPolicy.default(Paths.get("/allowed/path"))
// or FileSystemPolicy.permissive(Some(Paths.get("/path")))

val executor = ToolExecutor.derived[FileSystemTool]
val restricted = RestrictedExecutor(executor, policy)

val toolRegistry = ToolRegistry()
  .register(ToolSchema.derive[FileSystemTool], restricted)
```

### FileSystemPolicy Features

- **Sandboxing**: Restrict operations to a specific directory tree
- **Read-only mode**: Block write operations entirely
- **File size limits**: Prevent writing excessively large files (default 10MB)
- **Blocked patterns**: Automatically block sensitive files (`.env`, `.key`, `.pem`, `.ssh`, credentials, passwords, etc.)

### Preset Policies

- `FileSystemPolicy.strict(path)`: Read-only, sandboxed with default blocked patterns
- `FileSystemPolicy.default(path)`: Sandboxed with write access and default restrictions
- `FileSystemPolicy.permissive(path)`: Larger file size limit (100MB), minimal blocked patterns

## Complex Policy Example

```scala
import java.time.LocalTime

case class TimeRestrictedPolicy[T <: CallableTool[?]](
  allowedStart: LocalTime,
  allowedEnd: LocalTime
) extends ToolPolicy[T] {
  override def validate(tool: T): Try[Unit] = {
    val now = LocalTime.now()
    if (now.isAfter(allowedStart) && now.isBefore(allowedEnd)) {
      Success(())
    } else {
      Failure(new SecurityException(
        s"Tool can only be used between $allowedStart and $allowedEnd"
      ))
    }
  }
}

// Only allow calculator during business hours
val businessHoursPolicy = TimeRestrictedPolicy[Calculator](
  allowedStart = LocalTime.of(9, 0),
  allowedEnd = LocalTime.of(17, 0)
)

val executor = ToolExecutor.derived[Calculator]
val restricted = RestrictedExecutor(executor, businessHoursPolicy)
```

## Rate Limiting Example

```scala
import scala.collection.mutable
import java.time.Instant

case class RateLimitPolicy[T <: CallableTool[?]](
  maxCalls: Int,
  windowSeconds: Int
) extends ToolPolicy[T] {
  private val callTimes = mutable.Queue[Instant]()

  override def validate(tool: T): Try[Unit] = synchronized {
    val now = Instant.now()
    val cutoff = now.minusSeconds(windowSeconds)

    // Remove old calls outside the window
    while (callTimes.nonEmpty && callTimes.head.isBefore(cutoff)) {
      callTimes.dequeue()
    }

    if (callTimes.size >= maxCalls) {
      Failure(new SecurityException(
        s"Rate limit exceeded: $maxCalls calls per $windowSeconds seconds"
      ))
    } else {
      callTimes.enqueue(now)
      Success(())
    }
  }
}

// Limit to 10 calls per minute
val rateLimitPolicy = RateLimitPolicy[WebSearch](
  maxCalls = 10,
  windowSeconds = 60
)
```

## Best Practices

1. **Always use policies for sensitive tools**: FileSystem, HTTP, and other tools that access external resources
2. **Start restrictive**: Use `strict` or `default` policies and relax as needed
3. **Combine policies**: Layer multiple policies for defense in depth
4. **Log policy violations**: Track and alert on security violations
5. **Test policies**: Ensure policies work as expected before deployment

## Next Steps

- Browse [Provided tools](/hollywood/provided-tools)
- Learn about the [Tool system](/hollywood/tools)
- Explore [Agents](/hollywood/agents)
