---
title: Hollywood Provided Tools
description: Built-in tools for HTTP, FileSystem, Regex, JSON, WebFetch, and SearXNG
author: Mark Rudolph
published: 2025-10-03T00:00:00Z
lastUpdated: 2025-10-03T00:00:00Z
tags:
  - llm
  - tools
---

# Provided Tools

The library includes built-in tools that can be registered with agents.

## HttpClientTool

Make HTTP requests to any API endpoint with full control over method, headers, and body.

```scala
import dev.alteration.branch.hollywood.tools.provided.http.HttpClientTool

val toolRegistry = ToolRegistry()
  .register[HttpClientTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant that can make HTTP requests.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Make a GET request to https://api.github.com/users/octocat")
```

### Parameters

```scala
@schema.Tool("Make HTTP requests to any API endpoint")
case class HttpClientTool(
  @Param("The URL to request") url: String,
  @Param("HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)")
  method: String = "GET",
  @Param("Optional request headers as JSON object")
  headers: Option[String] = None,
  @Param("Optional request body as string")
  body: Option[String] = None
)
```

Supports all standard HTTP methods and allows custom headers and request bodies. Headers should be provided as a JSON object string (e.g., `"{\"Content-Type\": \"application/json\"}"`).

## FileSystemTool

Read, write, list, or check existence of files on the filesystem.

```scala
import dev.alteration.branch.hollywood.tools.provided.fs.FileSystemTool

val toolRegistry = ToolRegistry()
  .register[FileSystemTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant with filesystem access.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("List the files in the /tmp directory")
```

### Parameters

```scala
@schema.Tool("Read, write, or list files on the filesystem")
case class FileSystemTool(
  @Param("Operation to perform: 'read', 'write', 'list', or 'exists'")
  operation: String,
  @Param("File or directory path")
  path: String,
  @Param("Content to write (required for 'write' operation)")
  content: Option[String] = None,
  @Param("Whether to append to file instead of overwriting (for 'write' operation)")
  append: Option[Boolean] = None
)
```

### Operations

- `read`: Read contents of a file
- `write`: Write or append content to a file (creates parent directories if needed)
- `list`: List contents of a directory with file types and sizes
- `exists`: Check if a file or directory exists

### Security

**Important**: When using `FileSystemTool` in production, use `RestrictedExecutor` with the provided `FileSystemPolicy` to restrict access:

```scala
import dev.alteration.branch.hollywood.tools.provided.fs.{FileSystemTool, FileSystemPolicy}
import dev.alteration.branch.hollywood.tools.{ToolExecutor, RestrictedExecutor}
import java.nio.file.Paths

// Create a sandboxed filesystem policy
val policy = FileSystemPolicy.strict(Paths.get("/tmp"))

val executor = ToolExecutor.derived[FileSystemTool]
val restricted = RestrictedExecutor(executor, policy)

val toolRegistry = ToolRegistry()
  .register(ToolSchema.derive[FileSystemTool], restricted)
```

See [Security](/hollywood/security) for more details on FileSystemPolicy.

## RegexTool

Extract text, find patterns, and perform regex operations on text.

```scala
import dev.alteration.branch.hollywood.tools.provided.regex.RegexTool

val toolRegistry = ToolRegistry()
  .register[RegexTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant with text processing capabilities.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Extract all email addresses from this text: Contact us at info@example.com or support@test.org")
```

### Parameters

```scala
@schema.Tool("Extract text, find patterns, and perform regex operations")
case class RegexTool(
  @Param("Operation to perform: 'match', 'find_all', 'replace', 'extract', or 'split'")
  operation: String,
  @Param("Regular expression pattern")
  pattern: String,
  @Param("Input text to process")
  text: String,
  @Param("Replacement text (required for 'replace' operation)")
  replacement: Option[String] = None,
  @Param("Whether to use case-insensitive matching")
  caseInsensitive: Option[Boolean] = None,
  @Param("Whether to use multiline mode (^ and $ match line boundaries)")
  multiline: Option[Boolean] = None,
  @Param("Whether to use dotall mode (. matches newlines)")
  dotall: Option[Boolean] = None
)
```

### Operations

- `match`: Check if pattern matches the entire text (returns "true" or "false")
- `find_all`: Find all occurrences of pattern in text
- `replace`: Replace all occurrences with replacement text
- `extract`: Extract numbered groups from pattern matches
- `split`: Split text by pattern delimiter

### Examples

```scala
// Find all numbers
RegexTool("find_all", "\\d+", "I have 3 apples and 42 oranges")
// Output: Found 2 match(es):
// 1. 3
// 2. 42

// Extract email parts
RegexTool("extract", "(\\w+)@(\\w+\\.\\w+)", "Contact: user@example.com")
// Output: Found 1 match(es) with groups:
// Match 1: [Group 0: user@example.com, Group 1: user, Group 2: example.com]

// Replace URLs with placeholders
RegexTool("replace", "https?://[^\\s]+", "Visit https://example.com", replacement = Some("[LINK]"))
// Output: Replaced 1 occurrence(s):
// Visit [LINK]

// Split by commas
RegexTool("split", ",\\s*", "apple, banana, cherry")
// Output: Split into 3 part(s):
// 1. apple
// 2. banana
// 3. cherry
```

## JsonQueryTool

Query, filter, and transform JSON data with path expressions.

```scala
import dev.alteration.branch.hollywood.tools.provided.json.JsonQueryTool

val toolRegistry = ToolRegistry()
  .register[JsonQueryTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant that can process JSON data.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Extract all user names from this JSON: {\"users\": [{\"name\": \"Alice\", \"age\": 30}, {\"name\": \"Bob\", \"age\": 25}]}")
```

### Parameters

```scala
@schema.Tool("Query, filter, and transform JSON data with path expressions")
case class JsonQueryTool(
  @Param("JSON string to query") json: String,
  @Param("Operation to perform: 'get', 'filter', 'map', 'keys', 'values', 'exists', 'validate'")
  operation: String,
  @Param("JSONPath query (e.g., 'users.0.name', 'items.*.id', 'data.results')")
  path: Option[String] = None,
  @Param("Field name to extract for 'map' operation")
  field: Option[String] = None,
  @Param("Expected type for 'validate' operation: 'object', 'array', 'string', 'number', 'boolean', 'null'")
  expectedType: Option[String] = None
)
```

### Operations

- `get`: Extract value at path (supports wildcards like `users.*.name`)
- `map`: Extract specific field from all array elements
- `filter`: Filter array elements by field existence
- `keys`: List all keys in a JSON object
- `values`: List all values in a JSON object
- `exists`: Check if a path exists in the JSON
- `validate`: Verify JSON type matches expected type

### Path Syntax

- Dot notation for objects: `metadata.total`
- Numeric index for arrays: `users.0.name`
- Wildcard for all array elements: `items.*.id`

### Examples

```scala
// Extract nested value
JsonQueryTool(json, "get", path = Some("users.0.email"))
// Output: "alice@example.com"

// Get all names from array
JsonQueryTool(json, "get", path = Some("users.*.name"))
// Output: ["Alice", "Bob", "Charlie"]

// Map array to extract specific field
JsonQueryTool(json, "map", path = Some("users"), field = Some("name"))
// Output: Extracted 3 values for field 'name':
// ["Alice", "Bob", "Charlie"]

// Filter array by field existence
JsonQueryTool(json, "filter", path = Some("users"), field = Some("email"))
// Output: Filtered 3 items to 2 items with field 'email':
// [users with email field only]

// Validate type
JsonQueryTool(json, "validate", path = Some("users"), expectedType = Some("array"))
// Output: true - Value is of type 'array'
```

## WebFetch

Fetch a webpage by URL.

```scala
import dev.alteration.branch.hollywood.tools.provided.WebFetch

val toolRegistry = ToolRegistry()
  .register[WebFetch]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant that can fetch web pages.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("What's on the page at https://branch.alteration.dev?")
```

## SearXNGTool

Search the web using a SearXNG instance. Requires a running SearXNG server (configure via `SEARXNG_URL` environment variable).

```scala
import dev.alteration.branch.hollywood.tools.provided.searxng.SearXNGTool

val toolRegistry = ToolRegistry()
  .register[SearXNGTool]

val agent = OneShotAgent(
  systemPrompt = "You are a research assistant with web search capabilities.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Search for recent developments in Scala 3")
```

### Parameters

```scala
@schema.Tool("Search the web using SearXNG")
case class SearXNGTool(
                        @Param("search query (required)") q: String,
                        @Param("comma-separated list of categories (e.g., 'general', 'news', 'images', 'videos', 'science')")
                        categories: Option[String] = None,
                        @Param("comma-separated list of specific engines")
                        engines: Option[String] = None,
                        @Param("language code (e.g., 'en', 'es', 'fr')")
                        language: Option[String] = Some("en"),
                        @Param("search page number (default: 1)")
                        pageno: Option[Int] = None,
                        @Param("time range filter: 'day', 'month', or 'year'")
                        time_range: Option[String] = None,
                        @Param("safesearch level: 0 (off), 1 (moderate), or 2 (strict)")
                        safesearch: Option[Int] = Some(0),
                        @Param("maximum number of search results to return. Defaults to 10")
                        max_results: Option[Int] = None
                      )
```

The tool returns search results with title, URL, content, engine, score, and published date when available.

SearXNG is a privacy-focused metasearch engine that aggregates results from multiple search engines. It can be run locally, providing free web search capabilities without API keys. Learn more at [docs.searxng.org](https://docs.searxng.org).

## Combining Tools

You can register multiple tools for agents to use together:

```scala
val toolRegistry = ToolRegistry()
  .register[HttpClientTool]
  .register[JsonQueryTool]
  .register[RegexTool]
  .register[SearXNGTool]

val agent = OneShotAgent(
  systemPrompt = "You are a research assistant with web access and data processing capabilities.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat(
  "Search for Scala 3 documentation, fetch the main page, and extract all code examples"
)
```

## Next Steps

- Learn about [Security policies](/hollywood/security) for safe tool execution
- Explore the [Tool system](/hollywood/tools)
- Learn about [Agents](/hollywood/agents)
