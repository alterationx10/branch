# Friday Examples

Examples for the `friday` module, a functional JSON library for Scala.

## What is Friday?

Friday is a JSON library built on parser combinators that provides:

1. **JSON AST**: Parse and manipulate JSON with a simple AST
2. **Safe Navigation**: Use the `?` operator to safely traverse nested structures
3. **Type Classes**: Auto-derive encoders, decoders, and codecs for your types
4. **Transformations**: Transform codecs to work with different types

## Examples

### Basic - JSON Parsing and Navigation

**Location**: `friday/basic/BasicJsonExample.scala`

Demonstrates how to:
- Parse JSON strings into the Json AST
- Use the `?` operator for safe navigation
- Access values safely with `strOpt`, `numOpt`, etc.
- Access values directly with `strVal`, `numVal`, etc.
- Build JSON objects programmatically

**Run**:
```bash
sbt "examples/runMain friday.basic.BasicJsonExample"
```

### EncoderDecoder - Type Classes

**Location**: `friday/typeclass/EncoderDecoderExample.scala`

Demonstrates how to:
- Auto-derive encoders and decoders with `derives`
- Encode Scala objects to JSON
- Decode JSON to Scala types
- Work with nested case classes
- Handle sum types (enums)
- Handle decoding errors with Try

**Run**:
```bash
sbt "examples/runMain friday.typeclass.EncoderDecoderExample"
```

### Codec - Combined Encoding/Decoding

**Location**: `friday/typeclass/CodecExample.scala`

Demonstrates how to:
- Auto-derive codecs with `derives JsonCodec`
- Use codec extension methods (`toJson`, `toJsonString`, `decodeAs`)
- Transform codecs with `transform`, `bimap`, and `map`
- Create custom codecs for types like `Instant`
- Round-trip encode and decode data
- Work with collections

**Run**:
```bash
sbt "examples/runMain friday.typeclass.CodecExample"
```

## Quick Reference

### JSON AST

```scala
import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.*

// Parse JSON
val json = Json.parse("""{"name": "Alice"}""")

// Safe navigation
val name = json ? "name"

// Build JSON
val obj = Json.obj(
  "name" -> JsonString("Bob"),
  "age" -> JsonNumber(42)
)
```

### Type Classes

```scala
import dev.alteration.branch.friday.JsonCodec
import dev.alteration.branch.friday.JsonEncoder.given
import dev.alteration.branch.friday.JsonDecoder.given

case class Person(name: String, age: Int) derives JsonCodec

val person = Person("Alice", 30)

// Encode
val json = person.toJsonString

// Decode
val decoded = json.decodeAs[Person]
```
