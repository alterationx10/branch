---
title: Friday
description: A(nother) Scala JSON library
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - json
---

# Friday

_Friday_ is built off of a parser that is the topic of a chapter in
[Function Programming in Scala (2nd Ed)](https://www.manning.com/books/functional-programming-in-scala-second-edition)
of parser combinators.

The library provides an AST to convert JSON to/from, as well as type-classes for Encoders, Decoders, and Codecs.

There is also an emphasis on Json AST helper methods to easily work with JSON, without having to convert to/from an explicit schema.

## Working with the AST

The Json AST is described fully as

```scala
enum Json {
  case JsonNull
  case JsonBool(value: Boolean)
  case JsonNumber(value: Double)
  case JsonString(value: String)
  case JsonArray(value: IndexedSeq[Json])
  case JsonObject(value: Map[String, Json])
}
```

A json string can be parsed to `Json`, using the `parse` method on the `Json` companion object.

```scala
def parse(json: String): Either[ParseError, Json]
```

### Accessing Values

There are two ways to access values from a `Json` instance:

1. **Direct Access (Unsafe)** - These methods will throw an exception if the value is not of the expected type:
   - `strVal` - Get the underlying String
   - `numVal` - Get the underlying Double
   - `boolVal` - Get the underlying Boolean
   - `arrVal` - Get the underlying IndexedSeq[Json]
   - `objVal` - Get the underlying Map[String, Json]

For example:

```scala
JsonString("Some Str").strVal // Works fine
JsonString("Some Str").numVal // Throws exception
```

2. **Safe Access** - These methods return an `Option` of the underlying value:
   - `strOpt` - Try to get String
   - `numOpt` - Try to get Double
   - `boolOpt` - Try to get Boolean
   - `arrOpt` - Try to get IndexedSeq[Json]
   - `objOpt` - Try to get Map[String, Json]

### Working with Objects

To quickly parse through possible sub-fields on a JsonObject, there is a `?` extension method on both `Json` and
`Option[Json]` that takes a field name as an argument:

```scala
def ?(field: String): Option[Json]
```

This allows for safe traversal of nested JSON structures. For example:

```json
{
  "name": "Branch",
  "some": {
    "nested": {
      "key": "value"
    }
  }
}
```

We can safely traverse this structure:

```scala
val js: Json = ???

val maybeName: Option[Json] = js ? "name" // Some(JsonString("Branch"))
val deepField: Option[Json] = js ? "some" ? "nested" ? "key" // Some(JsonString("value"))

// Missing fields return None without throwing exceptions
val probablyNot: Option[Json] = js ? "totally" ? "not" ? "there" // None
```

## Type Classes

Friday provides three main type classes for working with JSON:

1. `JsonEncoder[A]` - For converting Scala types to JSON
2. `JsonDecoder[A]` - For converting JSON to Scala types
3. `JsonCodec[A]` - Combines both encoding and decoding capabilities

### Decoders

A `JsonDecoder[A]` converts `Json` to type `A` by implementing:

```scala
def decode(json: Json): Try[A]
```

For example:

```scala
given JsonDecoder[String] with {
  def decode(json: Json): Try[String] =
    Try(json.strVal)
}
```

Common decoders are provided and can be imported with:

```scala
import dev.wishingtree.branch.friday.JsonDecoder.given
```

Auto derivation is supported for `Product` types (case classes):

```scala
case class Person(name: String, age: Int) derives JsonDecoder

// Usage
val personJson = """{"name": "Mark", "age": 42}"""
val person: Try[Person] = Json.decode[Person](personJson)
```

### Encoders

A `JsonEncoder[A]` converts type `A` to `Json` by implementing:

```scala
def encode(a: A): Json
```

For example:

```scala
given JsonEncoder[String] with {
  def encode(a: String): Json = Json.JsonString(a)
}
```

Common encoders are provided and can be imported with:

```scala
import dev.wishingtree.branch.friday.JsonEncoder.given
```

Auto derivation works the same as with decoders:

```scala
case class Person(name: String, age: Int) derives JsonEncoder

// Usage
val person = Person("Mark", 42)
val json: Json = person.toJson  // Using extension method
// or
val json: Json = Json.encode(person)  // Using companion object
```

### Codecs

When you need both encoding and decoding, use `JsonCodec[A]`:

```scala
trait JsonCodec[A] { self =>
  given encoder: JsonEncoder[A]
  given decoder: JsonDecoder[A]

  def encode(a: A): Json = encoder.encode(a)
  def decode(json: Json): Try[A] = decoder.decode(json)
}
```

Codecs can be created in several ways:

1. Auto derivation for case classes:

```scala
case class Person(name: String, age: Int) derives JsonCodec
```

2. Combining existing encoder and decoder from the companion object JsonCodec.apply:

```scala
val codec: JsonCodec[Person] = JsonCodec[Person]  // If encoder and decoder are in scope
```

3. From explicit encode/decode functions:

```scala
val codec = JsonCodec.from[Person](
  decode = json => Try(/* decode logic */),
  encode = person => /* encode logic */
)
```

4. Transforming existing codecs:

```scala
// Transform a String codec into an Instant codec
val instantCodec: JsonCodec[Instant] = JsonCodec[String].transform(
  Instant.parse    // String => Instant
)(_.toString)      // Instant => String
```

The codec provides extension methods for convenient usage:

```scala
// Encoding
person.toJson         // Convert to Json
person.toJsonString   // Convert directly to JSON string

// Decoding
json.decodeAs[Person]       // Json => Try[Person]
jsonString.decodeAs[Person] // String => Try[Person]
```

You can also transform codecs to work with different types while preserving type safety:

```scala
// Transform with bimap
val longCodec: JsonCodec[Long] = JsonCodec[String].bimap(_.toLong)(_.toString)

// Transform with map
val intCodec: JsonCodec[Int] = JsonCodec[Long].map(_.toInt)(_.toLong)
```

## Other Libraries

If you like _Friday_, you should check out [uPickle](https://com-lihaoyi.github.io/upickle/) for a more comprehensive JSON library.
