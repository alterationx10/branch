# Mustachio Examples

This directory contains examples demonstrating the Mustachio templating engine, a lightweight mustache-style template renderer for Scala.

## What is Mustachio?

Mustachio is a simple yet powerful templating engine that uses mustache syntax (`{{variable}}`) to render templates with dynamic data. It's perfect for generating HTML emails, configuration files, reports, and any text-based output.

## Available Examples

### 1. Basic Templating
**File:** `basic/BasicMustachioExample.scala`

Learn the fundamentals of Mustachio:
- Variable substitution with `{{variable}}`
- HTML escaping vs unescaped output
- Nested object access with dot notation
- Handling missing values

```bash
sbt "examples/runMain examples.mustachio.basic.BasicMustachioExample"
```

### 2. Sections and Arrays
**File:** `sections/SectionsExample.scala`

Master control flow and iteration:
- Loop over arrays with `{{#section}}...{{/section}}`
- Conditional rendering based on truthiness
- Inverted sections with `{{^section}}...{{/section}}`
- Nested sections for complex data structures
- Using `{{.}}` to reference the current item

```bash
sbt "examples/runMain examples.mustachio.sections.SectionsExample"
```

### 3. Partials
**File:** `partials/PartialsExample.scala`

Build reusable template components:
- Include partials with `{{>partialName}}`
- Create template libraries
- Nested partials for complex layouts
- Context inheritance

```bash
sbt "examples/runMain examples.mustachio.partials.PartialsExample"
```

### 4. Email Templates
**File:** `email/EmailTemplateExample.scala`

Real-world HTML email generation:
- Complete order confirmation email
- Welcome email with promotional codes
- Combining partials, sections, and conditionals
- Professional email layouts

```bash
sbt "examples/runMain examples.mustachio.email.EmailTemplateExample"
```

### 5. JSON Integration
**File:** `json/JsonIntegrationExample.scala`

Use Friday JSON with Mustachio:
- Convert JSON to Stache with `Stache.fromJson`
- Render templates from API responses
- Generate reports from JSON data
- Configuration file templating

```bash
sbt "examples/runMain examples.mustachio.json.JsonIntegrationExample"
```

## Quick Start

```scala
import dev.alteration.branch.mustachio.{Mustachio, Stache}

// Create a template
val template = "Hello, {{name}}! You have {{count}} messages."

// Create context data
val context = Stache.obj(
  "name"  -> Stache.str("Alice"),
  "count" -> Stache.str("5")
)

// Render the template
val result = Mustachio.render(template, context)
// Output: "Hello, Alice! You have 5 messages."
```

## Mustache Syntax Reference

| Syntax | Description | Example |
|--------|-------------|---------|
| `{{variable}}` | HTML-escaped variable | `{{name}}` |
| `{{{variable}}}` | Unescaped variable | `{{{html}}}` |
| `{{#section}}...{{/section}}` | Section (loops/conditionals) | `{{#users}}...{{/users}}` |
| `{{^section}}...{{/section}}` | Inverted section | `{{^empty}}...{{/empty}}` |
| `{{>partial}}` | Include partial | `{{>header}}` |
| `{{.}}` | Current item in array | `{{#tags}}{{.}}{{/tags}}` |
| `{{object.field}}` | Nested field access | `{{user.email}}` |

## Building Context Data

### From Scala
```scala
val context = Stache.obj(
  "title" -> Stache.str("My Title"),
  "items" -> Stache.Arr(
    List(
      Stache.str("Item 1"),
      Stache.str("Item 2")
    )
  )
)
```

### From JSON
```scala
import dev.alteration.branch.friday.Json

val jsonString = """{"name": "Alice", "age": 30}"""
Json.parse(jsonString) match {
  case Right(json) =>
    val context = Stache.fromJson(json)
    Mustachio.render(template, context)
  case Left(error) =>
    println(s"Parse error: $error")
}
```

## Tips

- Start with the **Basic** example to understand fundamentals
- Move to **Sections** to learn control flow
- Use **Partials** for reusable components
- Check **Email** and **JSON** examples for real-world patterns
- All examples are self-contained and runnable
- Templates are just strings - load them from files, resources, or databases

## Learn More

For implementation details, see the source files in `branch/src/main/scala/dev/alteration/branch/mustachio/`.
