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

Designed with local LLMs in mind, this library is built to work with llama-server, primarily tested with gpt-oss. Since llama-server implements OpenAI-compatible endpoints, the library should work with other compatible LLM servers, though compatibility beyond llama-server has not been extensively validated.

This is an early-stage project that provides core functionality for straightforward agent workflows. As the library evolves, the API structure is subject to change — more so than other modules in this project.

## Configuration

Hollywood uses environment variables for configuration:

- `LLAMA_SERVER_URL` - Base URL for the LLM server (default: `http://localhost:8080`)
- `LLAMA_SERVER_COMPLETION_URL` - Base URL for chat completions (falls back to `LLAMA_SERVER_URL`, then `http://localhost:8080`)
- `LLAMA_SERVER_EMBEDDING_URL` - Base URL for embeddings (falls back to `LLAMA_SERVER_URL`, then `http://localhost:8080`)
- `SEARXNG_URL` - Base URL for SearXNG search (default: `http://localhost:8888`)

The completion and embedding URLs will first check their specific environment variable, then fall back to the general `LLAMA_SERVER_URL`, allowing you to configure a single URL for both or separate URLs if needed.

Here is what I am using to start llama-server, which allows gpt-oss to handle completions and embeddings:

```bash
llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
```

## Quick Start

### One Shot Agent

```scala
import dev.alteration.branch.hollywood.agents.*

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant. Respond concisely."
)

val response = agent.chat("What is 2+2?")
println(response)
```

### Agent with Tools

```scala
import dev.alteration.branch.hollywood.tools.*
import dev.alteration.branch.hollywood.tools.provided.http.HttpClientTool

val toolRegistry = ToolRegistry()
  .register[HttpClientTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant with web access.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Make a GET request to https://api.github.com/users/octocat")
```

## Documentation

- **[Agents](agents.md)** - OneShotAgent, RAG, Conversational agents
- **[Tools](tools.md)** - Tool system, CallableTool, ToolExecutor, ToolRegistry
- **[Security](security.md)** - ToolPolicy and RestrictedExecutor for safe tool execution
- **[Provided Tools](provided-tools.md)** - Built-in tools (HTTP, FileSystem, Regex, JSON, WebFetch, SearXNG)

## Agent Interface

All agents implement the `Agent` trait which defines a single method:

```scala
trait Agent {
  def chat(message: String): String
}
```

This simple interface allows agents to be easily composed and used as tools within other agents.

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

## Next Steps

- Learn about different [Agent types](agents.md)
- Explore the [Tool system](tools.md)
- Understand [Security policies](security.md)
- Browse [Provided tools](provided-tools.md)
