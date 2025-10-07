---
title: Hollywood
description: LLM Agents
author: Mark Rudolph
published: 2025-10-03T00:00:00Z
lastUpdated: 2025-10-03T00:00:00Z
tags:
  - llm
  - agent
  - llama-server
  - llama.cpp
---

# Hollywood

A library for LLM Agents.

Designed with local LLMs in mind, this library is built to work with llama-server, primarily tested with gpt-oss. Since
llama-server implements OpenAI-compatible endpoints, the library should work with other compatible LLM servers, though
compatibility beyond llama-server has not been extensively validated.

This is an early-stage project that provides core functionality for straightforward agent workflows. As the library
evolves, the API structure is subject to change — more so than other modules in this project.

## Configuration

Hollywood uses environment variables for configuration:

- `LLAMA_SERVER_URL` - Base URL for the LLM server (default: `http://localhost:8080`)
- `LLAMA_SERVER_COMPLETION_URL` - Base URL for chat completions (falls back to `LLAMA_SERVER_URL`, then
  `http://localhost:8080`)
- `LLAMA_SERVER_EMBEDDING_URL` - Base URL for embeddings (falls back to `LLAMA_SERVER_URL`, then
  `http://localhost:8080`)
- `SEARXNG_URL` - Base URL for SearXNG search (default: `http://localhost:8888`)

The completion and embedding URLs will first check their specific environment variable, then fall back to the general
`LLAMA_SERVER_URL`, allowing you to configure a single URL for both or separate URLs if needed.

Here is what I am using to start llama-server, which allows gpt-oss to handle completions and embeddings:

```bash
llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
```

## Agent

All agents implement the `Agent` trait which defines a single method:

```scala
trait Agent {
  def chat(message: String): String
}
```

This simple interface allows agents to be easily composed and used as tools within other agents.

### One Shot Agent

A stateless agent that executes a single request-response cycle with a fixed system prompt. Useful for specific tasks or
as a tool within other agents.

```scala
val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant. Respond concisely."
)

val response = agent.chat("What is 2+2?")
```

#### Creating Task-Specific Agents

The `OneShotAgent.forTask` factory helps create agents for specific tasks:

```scala
val summarizer = OneShotAgent.forTask(
  taskName = "Text Summarization",
  taskDescription = "Summarize the given text in one sentence.",
  inputFormat = Some("Raw text"),
  outputFormat = Some("One sentence summary")
)

val text = "Artificial intelligence has made tremendous progress. " +
  "AI systems are becoming increasingly capable in many domains."
val summary = summarizer.chat(text)
```

#### Variable Substitution

The `execute` method supports template variables:

```scala
val template = "Generate a {style} greeting for {name}."
val result = agent.execute(
  template,
  Map("style" -> "formal", "name" -> "Dr. Smith")
)
```

### RAG Agent

A Retrieval-Augmented Generation agent that uses a vector store to retrieve relevant documents and include them as
context when answering questions.

```scala
val vectorStore = new InMemoryVectorStore()
val embeddingClient = new EmbeddingClient()
val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

// Index documents
val documents = List(
  ("doc1", "Scala is a strong statically typed programming language..."),
  ("doc2", "The JVM enables running Java programs...")
)
documentIndexer.indexDocuments(documents)

// Create RAG agent
val ragAgent = new RagAgent(
  embeddingClient = embeddingClient,
  vectorStore = vectorStore,
  topK = 3,
  maxTurns = 10
)

val answer = ragAgent.chat("What is Scala?")
```

The agent:

1. Embeds the query using the embedding client
2. Searches the vector store for the top K relevant documents
3. Constructs a system message with the retrieved context
4. Passes the context and query to the LLM

#### Vector Store

The `VectorStore` trait defines the interface for document storage and retrieval:

```scala
trait VectorStore {
  def add(id: String, content: String, embedding: List[Double]): Unit

  def addAll(documents: List[(String, String, List[Double])]): Unit

  def search(queryEmbedding: List[Double], topK: Int = 5): List[ScoredDocument]

  def get(id: String): Option[Document]

  def remove(id: String): Unit

  def clear(): Unit
}
```

The included `InMemoryVectorStore` implementation uses cosine similarity for search.

### Conversational Agent

A stateful agent that maintains conversation history across multiple interactions.

```scala
val agent = ConversationalAgent()

val response1 = agent.chat("I have an orange cat named Whiskers.")
val response2 = agent.chat("What color was it?")
// Response will remember the cat is orange
```

#### Conversation State

Conversation history is managed by the `ConversationState` trait:

```scala
trait ConversationState {
  def get: List[ChatMessage]

  def update(messages: List[ChatMessage]): Unit
}
```

The default `InMemoryState` implementation keeps the most recent messages:

```scala
val conversationState = new InMemoryState(maxMessages = 50)
val agent = ConversationalAgent(
  conversationState = conversationState
)
```

## Tools

Tools allow agents to perform actions beyond text generation. The library provides a type-safe system for defining and
executing tools using compile-time derivation.

### CallableTool

Tools are defined as case classes that extend `CallableTool[A]`:

```scala
trait CallableTool[A] extends Product {
  def execute(): Try[A]
}
```

The `execute()` method returns a `Try[A]` for safe execution. If execution fails, the error message is returned to the
LLM as helpful feedback.

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

### Tool Executor

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

This means tools automatically support any return type that has a `JsonEncoder` instance. The compiler will verify at
compile time that an encoder exists for the tool's return type.

**Supported types** (both for arguments and results):

- Primitives: `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`
- Collections: `List[T]`, `Option[T]`, `Map[String, T]`
- Nested case classes
- Any custom types with `JsonCodec`

Case classes extending `CallableTool` automatically derive `JsonCodec` through Scala 3's derivation mechanism, so no
explicit `derives` clause is needed. If a required encoder is missing, you'll get a clear compile-time error.

### Tool Registry

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

### Tool Policies and Security

Hollywood provides `ToolPolicy` and `RestrictedExecutor` for validating and restricting tool execution based on custom rules.

#### ToolPolicy

A `ToolPolicy[T]` defines validation and transformation rules for tools:

```scala
trait ToolPolicy[T <: CallableTool[?]] {
  def validate(tool: T): Try[Unit]

  def transformArgs(args: Json): Json = args
}
```

**Built-in policies:**

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

**Use cases for policies:**

- Restrict tool operations based on input values
- Prevent access to sensitive resources
- Enforce rules
- Sanitize or transform inputs before execution

#### RestrictedExecutor

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

When executing tools, the `RestrictedExecutor`:

1. Applies the policy's `transformArgs` to the JSON input
2. Decodes the transformed arguments into the tool instance
3. Validates the tool against the policy
4. If validation passes, executes the tool with the delegate executor
5. If validation fails, returns a policy violation error

**Example with ToolRegistry:**

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

This approach allows you to add security and validation layers without modifying the tool implementation itself.

#### Tool Schema

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

#### Agents as Tools

Agents can be used as tools within other agents using `Agent.deriveAgentTool`:

```scala
// Create a specialized agent
val calculatorAgent = OneShotAgent(
  systemPrompt = "You are a calculator. Perform arithmetic accurately."
)

// Derive a tool from the agent
val calculatorTool = Agent.deriveAgentTool(
  calculatorAgent,
  agentName = Some("calculator"),
  description = "Use this tool to perform arithmetic calculations"
)

// Register the agent tool
val toolRegistry = ToolRegistry()
  .register(calculatorTool)

// Use in another agent
val mainAgent = OneShotAgent(
  systemPrompt = "You are an assistant. When asked to do math, use the calculator tool.",
  toolRegistry = Some(toolRegistry)
)

val response = mainAgent.chat("What is 100 divided by 4?")
```

This enables agent composition and specialization, where complex tasks can be delegated to domain-specific agents.

### Provided Tools

The library includes built-in tools that can be registered with agents:

#### HttpClientTool

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

**Tool parameters:**

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

#### FileSystemTool

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

**Tool parameters:**

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

**Operations:**

- `read`: Read contents of a file
- `write`: Write or append content to a file (creates parent directories if needed)
- `list`: List contents of a directory with file types and sizes
- `exists`: Check if a file or directory exists

**Security note:** When using `FileSystemTool` in production, use `RestrictedExecutor` with the provided `FileSystemPolicy` to restrict access to specific directories and prevent dangerous operations:

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
// or .registerWithPolicy(policy)
```

`FileSystemPolicy` provides:
- **Sandboxing**: Restrict operations to a specific directory tree
- **Read-only mode**: Block write operations entirely
- **File size limits**: Prevent writing excessively large files (default 10MB)
- **Blocked patterns**: Automatically block sensitive files (`.env`, `.key`, `.pem`, `.ssh`, credentials, passwords, etc.)

Three preset policies are available:
- `FileSystemPolicy.strict(path)`: Read-only, sandboxed with default blocked patterns
- `FileSystemPolicy.default(path)`: Sandboxed with write access and default restrictions
- `FileSystemPolicy.permissive(path)`: Larger file size limit (100MB), minimal blocked patterns

#### RegexTool

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

**Tool parameters:**

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

**Operations:**

- `match`: Check if pattern matches the entire text (returns "true" or "false")
- `find_all`: Find all occurrences of pattern in text
- `replace`: Replace all occurrences with replacement text
- `extract`: Extract numbered groups from pattern matches
- `split`: Split text by pattern delimiter

**Pattern flags:**

- `caseInsensitive`: Ignore case when matching (default: false)
- `multiline`: Make `^` and `$` match line boundaries instead of just start/end of text (default: false)
- `dotall`: Make `.` match newline characters (default: false)

**Examples:**

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

Uses `java.util.regex.Pattern` from the JVM standard library with zero external dependencies.

#### JsonQueryTool

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

**Tool parameters:**

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

**Operations:**

- `get`: Extract value at path (supports wildcards like `users.*.name`)
- `map`: Extract specific field from all array elements
- `filter`: Filter array elements by field existence
- `keys`: List all keys in a JSON object
- `values`: List all values in a JSON object
- `exists`: Check if a path exists in the JSON
- `validate`: Verify JSON type matches expected type

**Path syntax:**

- Dot notation for objects: `metadata.total`
- Numeric index for arrays: `users.0.name`
- Wildcard for all array elements: `items.*.id`

**Examples:**

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

#### WebFetch

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

#### SearXNG

Search the web using a SearXNG instance. Requires a running SearXNG server (configure via `SEARXNG_URL` environment
variable).

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

**Tool parameters:**

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

SearXNG is a privacy-focused metasearch engine that aggregates results from multiple search engines. It can be run
locally, providing free web search capabilities without API keys. Learn more
at [docs.searxng.org](https://docs.searxng.org).

## Tests

Tests are included but ignored by default since they require a running llama-server instance.

### Running Tests

To run the tests:

1. Start llama-server with embeddings support:

```bash
llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
```

2. In the test file, set `munitIgnore` to `false`:

```scala
class OneShotAgentSpec extends LlamaServerFixture {
  override def munitIgnore: Boolean = false // Enable tests

  override val shouldStartLlamaServer: Boolean = false // Using external server

  // ... tests ...
}
```

Alternatively, let the test fixture manage the server by keeping `shouldStartLlamaServer = true`.

### Test Examples

The test suite demonstrates key functionality:

- **OneShotAgentSpec**: Basic agent usage and task-specific agents
- **RagAgentSpec**: Document indexing, vector search, and RAG queries
- **ConversationalAgentSpec**: Conversation history management
- **CallableToolSpec**: Tool registration and agent-to-agent tool composition
