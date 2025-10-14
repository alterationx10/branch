# Hollywood Module Examples

The Hollywood module provides a framework for building AI agents with different capabilities and communication patterns.
These examples demonstrate the key features.

## Prerequisites

All examples require an OpenAI-compatible LLM server running locally:

- **Default URL**: `http://localhost:8080`
- **Environment Variable**: Set `LLAMA_SERVER_URL` to override the default
- **Server Requirements**: Must support chat completions API (`/v1/chat/completions`)
- **For tool examples**: Server must support function/tool calling

I've only tested it again llama-server, and mostly gpt-oss. For example. to run gtp-oss:20b with tool calling and
embeddings:

```shell
llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
```

## Examples

### 1. Basic OneShotAgent (`oneshot/BasicOneShotExample.scala`)

Demonstrates stateless, single request-response agents ideal for specific tasks.

**Key Concepts**:

- Creating agents with custom system prompts
- Using `OneShotAgent.forTask` factory method
- Template variables with `execute()`
- Multiple single-purpose agents

**Run**:

```bash
sbt "examples/runMain hollywood.oneshot.BasicOneShotExample"
```

**Use Cases**:

- Text summarization
- Sentiment analysis
- Code explanation
- Any single-turn task without conversation state

---

### 2. ConversationalAgent (`conversational/ConversationalAgentExample.scala`)

Shows stateful agents that maintain conversation history across multiple turns.

**Key Concepts**:

- Maintaining conversation state
- Custom `ConversationState` implementations
- Message history limits with `InMemoryState`
- Turn callbacks for monitoring
- System messages for role-playing

**Run**:

```bash
sbt "examples/runMain hollywood.conversational.ConversationalAgentExample"
```

**Use Cases**:

- Chat applications
- Interactive assistants
- Any scenario requiring context across messages

---

### 3. Agent with Tools (`tools/AgentWithToolsExample.scala`)

Demonstrates function calling - agents that can use external tools.

**Key Concepts**:

- Defining tools with `CallableTool` trait
- Registering tools with `ToolRegistry`
- Automatic tool schema generation
- Multiple tool types
- Providing `JsonEncoder` instances for tool return types

**Run**:

```bash
sbt "examples/runMain hollywood.tools.AgentWithToolsExample"
```

**Use Cases**:

- Agents that need to perform calculations
- Weather/API lookups
- Database searches
- Any external action beyond text generation

---

### 4. Multi-Agent Systems (`../dev/alteration/branch/hollywood/examples/MultiAgentExample.scala`)

Shows agent-to-agent communication using `Agent.deriveAgentTool`.

**Key Concepts**:

- Creating specialist agents
- Converting agents to tools with `deriveAgentTool`
- Orchestrator pattern (coordinator + specialists)
- Multi-step agent workflows
- Hierarchical agent systems

**Run**:

```bash
sbt "examples/runMain dev.alteration.branch.hollywood.examples.MultiAgentExample"
```

**Use Cases**:

- Domain-specific expert agents (Python, SQL, etc.)
- Fact-checking workflows
- Writer + Editor collaboration
- Any task requiring specialized expertise

---

### 5. RAG Agent (`rag/RagAgentExample.scala`)

Demonstrates Retrieval-Augmented Generation for document-based question answering.

**Key Concepts**:

- Creating and indexing a vector store
- Using `DocumentIndexer` for automatic embedding generation
- Document chunking with `DocumentChunker`
- Different chunking strategies (Sentence, Paragraph, Character, Token)
- Vector similarity search
- Retrieval relevance and context handling

**Run**:

```bash
sbt "examples/runMain hollywood.rag.RagAgentExample"
```

**Use Cases**:

- Knowledge base Q&A systems
- Document search and retrieval
- Context-aware chatbots
- Information extraction from large document collections

---

## Common Patterns

### Creating an Agent

```scala
// Simple agent
val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant"
)

// With tools
val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant",
  toolRegistry = Some(registry)
)

// Conversational
val agent = ConversationalAgent(
  conversationState = new InMemoryState()
)
```

### Defining Tools

```scala
case class MyTool(
                   param1: String,
                   param2: Int = 0 // Optional with default
                 ) extends CallableTool[ReturnType] {
  def apply(): ReturnType = {
    // Tool implementation
  }
}

// Register the tool
given JsonEncoder[ReturnType] =
... // Provide encoder for return type
val registry = ToolRegistry().register[MyTool]
```

### Agent-to-Agent Communication

```scala
// Create specialist agent
val specialistAgent = OneShotAgent(
  systemPrompt = "You are an expert in X"
)

// Derive tool from agent
val specialistTool = Agent.deriveAgentTool(
  specialistAgent,
  agentName = Some("SpecialistName"),
  description = "Description of what this agent does"
)

// Register in orchestrator's registry
val registry = ToolRegistry().register(specialistTool)

// Create orchestrator that can call the specialist
val orchestrator = OneShotAgent(
  systemPrompt = "Route questions to appropriate specialists",
  toolRegistry = Some(registry)
)
```

## Agent Types Comparison

| Agent Type            | State                         | Use Case                         | Max Turns   |
|-----------------------|-------------------------------|----------------------------------|-------------|
| `OneShotAgent`        | Stateless                     | Single task, can be used as tool | Default: 10 |
| `ConversationalAgent` | Stateful                      | Multi-turn conversations         | Default: 50 |
| `RagAgent`            | Stateless (uses vector store) | Q&A with document retrieval      | Default: 50 |

### RAG Components

| Component             | Purpose                                                |
|-----------------------|--------------------------------------------------------|
| `VectorStore`         | Stores document embeddings for similarity search       |
| `InMemoryVectorStore` | Simple in-memory implementation of VectorStore         |
| `EmbeddingClient`     | Generates vector embeddings for text                   |
| `DocumentIndexer`     | Automates embedding generation and storage             |
| `DocumentChunker`     | Splits long documents into chunks for better retrieval |

## Configuration

All agents support these common parameters:

- `model: String` - LLM model name (default: "gpt-oss")
- `maxTurns: Int` - Maximum conversation turns (for tool calling loops)
- `onTurn: Option[(Int, ChatMessage) => Unit]` - Callback for monitoring
- `toolRegistry: Option[ToolRegistry]` - Tools available to the agent
- `completionClient: ChatCompletionClient` - Custom HTTP client for completions

## Tips

1. **Start simple**: Begin with `OneShotAgent` for single tasks
2. **Add state when needed**: Use `ConversationalAgent` for context
3. **Tools for actions**: Add tools when agents need to perform operations
4. **Specialize agents**: Create focused agents for specific domains
5. **Compose with tools**: Use `deriveAgentTool` for agent collaboration

## Next Steps

- Explore the `RagAgent` for document-based Q&A
- Check the `ToolPolicy` system for tool access control
- Look at the `AgentConversationLoop` for custom implementations
- Review `ConversationState` for custom state management strategies
