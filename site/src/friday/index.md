# Friday

*Friday* is built off of a parser that is the topic of a chapter in
[Function Programming in Scala (2nd Ed)](https://www.manning.com/books/functional-programming-in-scala-second-edition)
of parser combinators.

The library provides an AST to convert JSON to/from, as well as type-classes for Encoders, Decoders, and Codecs.

There is also an emphasis on Json AST helper methods to easily work with JSON, without having to convert to/from an
explicit schema.

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
  "some" : {
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
val probablyNot: Option[String] = js ? "totally" ? "not" ? "there" /
```

## Encoder, Decoders, and Codecs



## Other Libraries

If you like *Friday*, you should check out [uPickle](https://com-lihaoyi.github.io/upickle/)