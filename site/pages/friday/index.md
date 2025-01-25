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

*Friday* is built off of a parser that is the topic of a chapter in
[Function Programming in Scala (2nd Ed)](https://www.manning.com/books/functional-programming-in-scala-second-edition)
of parser combinators.

The library provides an AST to convert JSON to/from, as well as type-classes for Encoders, Decoders, and Codecs.

There is also an emphasis on Json AST helper methods to easily work with JSON, without having to convert to/from an
explicit schema.

Great uses of this library are for encoding/decoding JSON with the [Spider](../spider/index.md) http server/client
project, or simple JSON driven configuration files.

## Working with the AST

The Json AST is described fully as

```scala 3
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

```scala 3
def parse(json: String): Either[ParseError, Json] 
```

Once you have a reference to a `Json`, you can (**dangerously**) access the underlying value by calling one of

* strVal
* numVal
* boolVal
* arrVal
* objVal

These methods will throw an exception if the underlying value is not the approriate type, for example

```scala 3
JsonString("Some Str").strVal // Works fine
JsonString("Some Str").numVal // Throws exception
```

You can *safely* use one of the following methods to get an Option of the underlying value

* strOpt
* numOpt
* boolOpt
* arrOpt
* objOpt

To quickly parse through possible sub-fields on a JsonObject, there is a `?` extension method on both `Json` and
`Option[Json]` that takes a field name as an argument

```scala 3
def ?(field: String): Option[Json]
```

With this, if we had some JSON

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

We can do things like

```scala 3
val js: Json = ???

val maybeName: Option[Json] = js ? "name" // It's there!
val deepField: Option[Json] = js ? "some" ? "nested" ? "key" // This too!

// Not present, but doesn't throw an exception attempting to access deeper fields!
val probablyNot: Option[String] = js ? "totally" ? "not" ? "there"
```

## Encoder, Decoders, and Codecs

*Friday* provides type-classes to convert Json to/from Scala models.

## Decoders

For some type `A`, we can define a `JsonEncoder[A]` that can convert `Json` to `A` by providing an implementation of

```scala 3
  def decode(json: Json): Try[A]
  ````

For example, the following decoder can convert `Json` to `String`

```scala 3
given JsonDecoder[String] with {
  def decode(json: Json): Try[String] =
    Try(json.strVal)
}
```

Some decoders for common types like this are provided and can accessed by importing

```scala 3
import dev.wishingtree.branch.friday.JsonDecoder.given
```

Auto derivation is also supported for `Product` types (`Sum` types soon™️). We can use `derives` on case classes as

```scala 3
case class Person(name: String, age: Int)derives JsonDecoder
```

With the proper decoder in scope, we can decode JSON (or JSON that is still in String form) with

```scala 3
val personJson =
  """
    |{
    |  "name": "Mark",
    |  "age": 42
    |}
    |""".stripMargin

println {
  Json.decode[Person](personJson) // returns a Try[Person]
}
```

## Encoders

For some type `A`, we can define an encoder that can convert `A` to `Json` by providing an implementation of

```scala 3
  def encode(a: A): Json
```

For example, the following encoder can convert `String` to `Json`

```scala 3
given JsonEncoder[String] with {
  def encode(a: String): Json = Json.JsonString(a)
}
```

Some encoders for common types are provided and can accessed by importing

```scala 3
import dev.wishingtree.branch.friday.JsonEncoder.given
```

Auto derivation is also supported for `Product` types (`Sum` types soon™️). We can use `derives` on case classes as

```scala 3
case class Person(name: String, age: Int)derives JsonEncoder
```

With the proper encoder in scope, we can use the extension method provided by the type class, or the method on the Json
companion object to convert to `Json`

```scala 3
Person("Mark", 42).toJson
Json.encode(Person("Mark", 42))
```

## Codecs

We often want to be able to go both ways, so this library provides a codec which has the ability to encode and decode.

```scala 3
trait JsonCodec[A] extends JsonDecoder[A], JsonEncoder[A]
```

This also supports auto derivation for `Product` types (`Sum` types soon™️). We can use `derives` on case classes as

```scala 3
case class Person(name: String, age: Int)derives JsonCodec
```

With both an encoder and decoder, there is also the `JsonCodec.apply` method to easily create an instance

```scala 3
def apply[A](using
             encoder: JsonEncoder[A],
             decoder: JsonDecoder[A]
            ): JsonCodec[A] =
  new JsonCodec[A] {
    override def decode(json: Json): Try[A] = decoder.decode(json)

    override def encode(a: A): Json = encoder.encode(a)
  }
```

## Other Libraries

If you like *Friday*, you should check out [uPickle](https://com-lihaoyi.github.io/upickle/)