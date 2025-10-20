---
title: Hollywood Agents
description: OneShotAgent, RAG, and Conversational agents
author: Mark Rudolph
published: 2025-10-03T00:00:00Z
lastUpdated: 2025-10-03T00:00:00Z
tags:
  - llm
  - agent
---

# Agents

All agents implement the `Agent` trait which defines a single method:

```scala
trait Agent {
  def chat(message: String): String
}
```

This simple interface allows agents to be easily composed and used as tools within other agents.

## One Shot Agent

A stateless agent that executes a single request-response cycle with a fixed system prompt. Useful for specific tasks or as a tool within other agents.

```scala
val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant. Respond concisely.",
  completionClient = ChatCompletionClient()
)

val response = agent.chat("What is 2+2?")
```

The `completionClient` parameter defaults to `ChatCompletionClient()`, so it can be omitted for standard usage.

### Creating Task-Specific Agents

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

### Variable Substitution

The `execute` method supports template variables:

```scala
val template = "Generate a {style} greeting for {name}."
val result = agent.execute(
  template,
  Map("style" -> "formal", "name" -> "Dr. Smith")
)
```

## RAG Agent

A Retrieval-Augmented Generation agent that uses a vector store to retrieve relevant documents and include them as context when answering questions.

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
  completionClient = ChatCompletionClient(),
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

### Vector Store

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

## Conversational Agent

A stateful agent that maintains conversation history across multiple interactions.

```scala
val agent = ConversationalAgent()

val response1 = agent.chat("I have an orange cat named Whiskers.")
val response2 = agent.chat("What color was it?")
// Response will remember the cat is orange
```

### Conversation State

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

## Agents as Tools

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

## Next Steps

- Learn about the [Tool system](tools.md)
- Explore [Security policies](security.md) for safe tool execution
- Browse [Provided tools](provided-tools.md)
