---
title: Mustachio
description: A Mustache template engine
author: Mark Rudolph
published: 2025-01-25T04:37:00Z
lastUpdated: 2025-01-25T04:37:00Z
tags:
  - mustache
  - template
---

# Mustachio

Mustachio is a [Mustache](https://mustache.github.io) template engine implementation in Scala. It has been built and tested against the official [mustache/spec](https://github.com/mustache/spec), supporting all core Mustache features (but not the optional modules).

## Features

The library fully supports the core Mustache specification features:

- **Interpolation** - Basic variable replacement with HTML escaping (`{{name}}`) and unescaped interpolation (`{{{name}}}` or `{{&name}}`)
- **Sections** - Conditional and iterative sections (`{{#section}}`) with support for:
  - Boolean values
  - Lists/Arrays
  - Objects/Lambdas
  - Nested contexts
- **Inverted Sections** - Negative conditions (`{{^section}}`)
- **Partials** - Template inclusion (`{{>partial}}`) with proper indentation support
- **Comments** - Comment blocks (`{{!comment}}`) with standalone comment removal
- **Delimiters** - Custom delimiter changes (`{{=<% %>=}}`)

## Usage

Basic usage example:

```scala
import dev.alteration.branch.mustachio.{Mustachio, Stache}

// Create context
val context = Stache.obj(
  "name" -> Stache.str("John"),
  "items" -> Stache.Arr(List(
    Stache.obj("item" -> Stache.str("one")),
    Stache.obj("item" -> Stache.str("two"))
  ))
)

// Define template
val template = """
Hello {{name}}!
{{#items}}
- {{item}}
{{/items}}
"""

// Render
val result = Mustachio.render(template, context)
```

### Working with Partials

Partials can be provided as an optional parameter:

```scala
val partials = Stache.obj(
  "header" -> Stache.str("{{title}}")
)

Mustachio.render(template, context, Some(partials))
```

### JSON Integration

The library provides convenient JSON integration through the `Stache.fromJson` method using the [Friday JSON library](../friday/index.md):

```scala
import dev.alteration.branch.friday.Json

val jsonContext = Json.obj(
  "name" -> Json.str("John"),
  "age" -> Json.num(30)
)

val context = Stache.fromJson(jsonContext)
```

## Specification Compliance

The implementation is tested against the official Mustache specification tests for:

- Interpolation
- Sections
- Inverted Sections
- Comments
- Partials
- Delimiter Changes

### Unsupported Features

The following optional Mustache features are not currently supported:

- Dynamic partial names
- Template inheritance
- Lambda functions (advanced)

## Implementation Notes

- HTML escaping is performed on all interpolated values by default unless using triple mustaches `{{{var}}}` or ampersand `{{&var}}`
- Whitespace handling follows the Mustache spec for standalone tags
- Partial indentation is preserved according to spec
- Numbers are formatted as integers when possible
