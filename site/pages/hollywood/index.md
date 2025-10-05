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

Here is what I am using to start llama-server, which allows gpt-oss to completions and embeddings:

```bash
`llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean`
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

This means tools automatically support any return type that has a `JsonEncoder` instance. The compiler will verify at compile time that an encoder exists for the tool's return type.

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

The library includes some built-in tools that can be registered with agents:

#### WebFetch

Fetch a webpage by url.

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
