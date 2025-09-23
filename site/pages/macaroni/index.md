---
title: Macaroni
description: A collection of reusable modules
author: Mark Rudolph
published: 2025-01-25T04:37:00Z
lastUpdated: 2025-01-25T04:37:00Z
tags:
  - parser
  - pool
  - metaprogramming
  - crypto
  - filesystem
---

# Macaroni

Macaroni provides a collection of reusable utilities and helpers that could be useful in any project. It includes modules for meta-programming, parsing, resource pooling, cryptography, and filesystem operations.

## Meta Programming

The `meta` package provides type-level programming utilities:

### Type Helpers

The `Types` object provides type-level helpers for working with tuples and type intersections/unions:

```scala
// Intersection type with Any default
type IAnyType[T <: Tuple] = Tuple.Fold[T, Any, [x, y] =>> x & y]

// Intersection type with Nothing default
type INothingType[T <: Tuple] = Tuple.Fold[T, Nothing, [x, y] =>> x & y]

// Union type with Any default
type UAnyType[T <: Tuple] = Tuple.Fold[T, Any, [x, y] =>> x | y]
```

These are particularly useful when working with case class fields via mirrors to create intersection or union types of all fields.

## Parser Combinators

The `parsers` package provides a parser combinator library, which powers the [Friday](../friday/index.md) JSON parser. The implementation is based on the approach described in [Functional Programming in Scala (2nd Ed)](https://www.manning.com/books/functional-programming-in-scala-second-edition).

Key components:

- `Parsers` trait - Defines the core parsing primitives and combinators
- `Reference` implementation - A concrete implementation of the parser combinators
- `Location` - Tracks position in input for error reporting
- `ParseError` - Provides detailed error messages with line/column information

Example usage:

```scala
import dev.alteration.branch.macaroni.parsers.{Parsers, Reference}

// Create a simple parser
val parser = Reference.string("hello")

// Run the parser
parser.run("hello") // Right("hello")
parser.run("world") // Left(ParseError)
```

## Resource Pool

The `ResourcePool[R]` trait provides a simple way to manage pools of reusable resources. Key features:

- Semaphore-based access control (default 5 concurrent resources)
- Lazy resource initialization (resources created on first use)
- Optional health checking
- Automatic resource cleanup

To implement a resource pool:

```scala
import dev.alteration.branch.macaroni.pool.ResourcePool

class DatabasePool extends ResourcePool[Connection] {
  def acquire: Connection = // Create new connection
  def release(conn: Connection): Unit = conn.close()

  // Optional: Override to add connection testing
  override def test(conn: Connection): Boolean =
    conn.isValid(5000)
}
```

## Filesystem Operations

The `fs` package provides convenient filesystem utilities through the `PathOps` object:

```scala
import dev.alteration.branch.macaroni.fs.PathOps.*

// Get working directory
val wd: Path = PathOps.wd

// Path operations using extension methods
val configPath = wd / "config" / "app.conf"  // Append segments using /
val otherPath = Path.of("other/path")
val combined = configPath / otherPath        // Can combine with other Paths

// Path relativization
val relative = configPath.relativeTo(wd)      // Get path relative to working dir
val relativeStr = configPath.relativeTo("/home/user") // Or relative to string path

// Convert path to sequence of segments
val segments: Seq[String] = configPath.toSeq  // Split path into parts

// Pattern matching on paths using / extractor
configPath match {
  case parent / "app.conf" => println(s"Found config in $parent")
  case _ => println("Config not found")
}

// String interpolation
val srcPath = p"src/main/resources"     // Create Path using p"" interpolator
val pattern = ci"Config.JSON"           // Case-insensitive pattern matching

// Empty path constant
val emptyPath = >>                      // Represents Path.of("")
```

The PathOps utilities make it easy to work with filesystem paths in a more idiomatic Scala way, with extension methods for common operations and pattern matching support.

## Cryptography

The `crypto` package provides common cryptographic operations:

- Secure random key generation 
- Base64 encoding/decoding
- PBKDF2 password hashing
- HMAC message authentication
- AES encryption/decryption

Example usage:

```scala
import dev.alteration.branch.macaroni.crypto.Crypto

// Generate random keys
val publicKey = Crypto.generatePublicKey(16)  // 16-char key using limited ASCII range
val privateKey = Crypto.generatePrivateKey(32) // 32-char key using full ASCII range

// Password hashing with PBKDF2
val hash = Crypto.pbkdf2Hash("mypassword123")
val isValid = Crypto.validatePbkdf2Hash("mypassword123", hash) // true

// AES encryption/decryption
val message = "sensitive data"
val encrypted = Crypto.aesEncrypt(message, privateKey).get
val decrypted = Crypto.aesDecrypt(encrypted, privateKey).get // "sensitive data"

// HMAC authentication (SHA-256 and SHA-512)
val hmac256 = Crypto.hmac256("authenticate this", privateKey)
val isValid256 = Crypto.validateHmac256("authenticate this", privateKey, hmac256) // true

val hmac512 = Crypto.hmac512("authenticate this", privateKey)
val isValid512 = Crypto.validateHmac512("authenticate this", privateKey, hmac512) // true

// Base64 encoding/decoding
val encoded = Crypto.base64Encode("hello world")
val decoded = Crypto.base64Decode(encoded) // "hello world"
```

## Runtime Utilities

The `runtimes` package provides execution contexts and executor services that use Java 21's virtual threads:

```scala
import dev.alteration.branch.macaroni.runtimes.BranchExecutors

// Get the global virtual thread executor
val executor = BranchExecutors.executorService

// Get the global execution context
val ec = BranchExecutors.executionContext
```
