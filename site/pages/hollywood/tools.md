---
title: Hollywood Tools
description: Tool system, CallableTool, ToolExecutor, and ToolRegistry
author: Mark Rudolph
published: 2025-10-03T00:00:00Z
lastUpdated: 2025-10-03T00:00:00Z
tags:
  - llm
  - tools
---

# Tools

Tools allow agents to perform actions beyond text generation. The library provides a type-safe system for defining and executing tools using compile-time derivation.

## CallableTool

Tools are defined as case classes that extend `CallableTool[A]`:

```scala
trait CallableTool[A] extends Product {
  def execute(): Try[A]
}
```

The `execute()` method returns a `Try[A]` for safe execution. If execution fails, the error message is returned to the LLM as helpful feedback.

Annotate your tool with `@Tool` and parameters with `@Param` to generate schemas:

```scala
@schema.Tool("Add two numbers together")
case class Calculator(
                       @Param("a number") a: Int,
                       @Param("a number") b: Int
                     ) extends CallableTool[Int] {
  def execute(): Try[Int] = Success(a + b)
}
```

## Tool Executor

The `ToolExecutor` trait handles deserializing JSON arguments from the LLM into typed tool instances and executing them:

```scala
trait ToolExecutor[T <: CallableTool[?]] {
  def execute(args: Json): Json
}
```

Executors are automatically derived at compile time using `ToolExecutor.derived[T]`, which:

1. Uses `JsonDecoder` to deserialize the JSON arguments into the tool case class
2. Calls the tool's `execute()` method, which returns `Try[A]`
3. Pattern matches on the `Try` result:
    - On `Success`, encodes the result using `JsonEncoder[A]`
    - On `Failure`, returns a helpful error message to the LLM
4. Uses match types to extract the result type `A` from `CallableTool[A]`
5. Requires `JsonEncoder[A]` as a `using` parameter, resolved implicitly by the compiler to encode the result as JSON

The derivation uses Scala 3's match types to extract the return type from the tool definition:

```scala
type ResultType[T <: CallableTool[?]] <: Any = T match {
  case CallableTool[a] => a
}
```

This means tools automatically support any return type that has a `JsonEncoder` instance. The compiler will verify at compile time that an encoder exists for the tool's return type.

### Supported Types

Both for arguments and results:

- Primitives: `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`
- Collections: `List[T]`, `Option[T]`, `Map[String, T]`
- Nested case classes
- Any custom types with `JsonCodec`

Case classes extending `CallableTool` automatically derive `JsonCodec` through Scala 3's derivation mechanism, so no explicit `derives` clause is needed. If a required encoder is missing, you'll get a clear compile-time error.

## Tool Registry

The `ToolRegistry` manages tool schemas and executors:

```scala
val toolRegistry = ToolRegistry()
  .register[Calculator]
  .register[Add]
  .register[Multiply]

// Use with an agent
val agent = OneShotAgent(
  systemPrompt = "You are a math assistant.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("What is 15 plus 27?")
```

The registry:

- Stores tool schemas and executors
- Converts schemas to OpenAI-compatible function definitions
- Executes tools by name with string arguments
- Supports chaining registrations

## Tool Schema

Tool schemas are derived at compile time from annotated case classes using macros. The `ToolSchema.derive[T]` macro:

1. Extracts the `@Tool` annotation for the tool description
2. Reads the case class constructor parameters
3. Extracts `@Param` annotations for parameter descriptions
4. Converts Scala types to JSON schema types
5. Handles optional parameters and enum types

The generated schema includes:

- Tool name (fully qualified class name)
- Description from `@Tool` annotation
- Parameter definitions with types and descriptions
- Required vs optional parameters

## Example: Custom Tool

```scala
import dev.alteration.branch.hollywood.tools.*
import dev.alteration.branch.hollywood.tools.schema.*
import scala.util.{Try, Success, Failure}

@Tool("Calculate the area of a rectangle")
case class RectangleArea(
                          @Param("width of the rectangle") width: Double,
                          @Param("height of the rectangle") height: Double
                        ) extends CallableTool[Double] {
  def execute(): Try[Double] = {
    if (width < 0 || height < 0) {
      Failure(new IllegalArgumentException("Width and height must be positive"))
    } else {
      Success(width * height)
    }
  }
}

// Register and use
val toolRegistry = ToolRegistry()
  .register[RectangleArea]

val agent = OneShotAgent(
  systemPrompt = "You are a geometry assistant.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("What is the area of a 5.5 by 3.2 rectangle?")
```

## Example: Tool with Complex Return Type

```scala
case class SearchResult(title: String, url: String, snippet: String)

@Tool("Search for information on the web")
case class WebSearch(
                      @Param("search query") query: String,
                      @Param("number of results") limit: Option[Int] = None
                    ) extends CallableTool[List[SearchResult]] {
  def execute(): Try[List[SearchResult]] = {
    Try {
      // Perform search and return results
      performSearch(query, limit.getOrElse(10))
    }
  }
}
```

## Next Steps

- Explore [Security policies](/hollywood/security) for safe tool execution
- Browse [Provided tools](/hollywood/provided-tools)
- Learn about [Agents](/hollywood/agents)
