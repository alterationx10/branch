# Hollywood Tools Roadmap

This document outlines planned tools to expand the Hollywood LLM agent toolkit.

**Constraint**: All tools must be implemented with **zero external dependencies** (using only JVM stdlib and existing Branch modules).

## Current Tools

- **WebFetch** - Fetch webpage content by URL
- **SearXNGTool** - Web search via SearXNG instance

## Planned Tools (Feasible with Zero Dependencies)

### Content Processing & Extraction

#### 1. HTML Parser Tool ✅
- Extract text, tables, or basic metadata from HTML using string operations
- Simple tag/attribute extraction with regex
- Return structured data from web pages
- **Implementation**: String manipulation, regex patterns

#### 2. JSON Query Tool ✅
- Query and transform JSON data (leverage existing friday module)
- JSONPath-like queries
- Schema validation
- **Implementation**: Extend existing JSON support

#### 3. Regex Tool ✅
- Pattern matching and text extraction
- Named capture groups
- Find and replace operations
- **Implementation**: java.util.regex.Pattern/Matcher

### Data Storage & Retrieval

#### 4. File System Tool ✅
- Read/write/list files with safety constraints
- Support for text and common file formats
- Sandboxed operations with configurable permissions
- **Implementation**: java.nio.file.Files, Path

#### 5. Cache Tool ✅
- Store and retrieve temporary data between agent calls
- TTL support for automatic expiration
- Key-value interface
- **Implementation**: scala.collection.mutable.Map with timestamps

#### 6. CSV Tool ✅
- Read and parse CSV/TSV tabular data
- Query and filter capabilities
- **Implementation**: String.split() with quote handling

### External Integrations

#### 7. HTTP Client Tool ✅
- Generic REST API client with configurable methods/headers
- Support for authentication (Bearer, API key, etc.)
- Request/response logging
- **Implementation**: java.net.http.HttpClient (already in use)

#### 8. GitHub Tool ✅
- Search repositories, read issues and pull requests
- Access repository content via GitHub API
- **Implementation**: HTTP calls to GitHub REST API

#### 9. Slack/Discord Webhook Tool ✅
- Post messages to channels via webhooks
- Message formatting
- **Implementation**: HTTP POST to webhook URLs

#### 10. Translation Tool ✅
- Translate text between languages
- Integration with translation APIs (LibreTranslate, etc.)
- **Implementation**: HTTP API calls

### Computation & Transformation

#### 11. Math/Calculator Tool ✅
- Perform mathematical calculations
- Basic unit conversions
- Formula evaluation
- **Implementation**: Math stdlib functions, expression parsing

#### 12. Text Summarization Tool ✅
- Summarize long content using LLM calls
- Configurable summary length
- Multiple summarization strategies
- **Implementation**: Calls to LLM endpoint with summarization prompt

#### 13. Code Executor Tool ⚠️
- Execute JavaScript code snippets
- Timeout and resource limits
- **Implementation**: javax.script.ScriptEngine (limited to JavaScript)
- **Note**: Limited language support without dependencies

### RAG Enhancements

#### 14. Document Chunker Tool ✅
- Split documents intelligently for indexing
- Respect sentence/paragraph boundaries
- Configurable chunk size and overlap
- **Implementation**: String operations, sentence detection

#### 15. Hybrid Search Tool ✅
- Combine keyword and vector search
- BM25 algorithm + semantic search
- Configurable weighting
- **Implementation**: BM25 algorithm from scratch, combine with existing vector search

### Workflow & Orchestration

#### 16. Multi-Agent Tool ✅
- Coordinate multiple specialized agents
- Agent routing and delegation
- Result aggregation
- **Implementation**: Use existing Agent.deriveAgentTool, orchestration logic

#### 17. Workflow Tool ✅
- Execute predefined sequences of actions
- Conditional branching
- Error handling and recovery
- **Implementation**: Pure Scala control flow, tool chaining

#### 18. Validation Tool ✅
- Validate outputs against schemas/rules
- JSON Schema validation
- Custom validation logic
- **Implementation**: Pattern matching, JSON schema validation logic

#### 19. Retry Tool ✅
- Automatic retry logic with exponential backoff
- Configurable retry conditions
- Circuit breaker pattern
- **Implementation**: Pure logic with Thread.sleep, state tracking

### Observability

#### 20. Logging Tool ✅
- Structured logging for agent actions
- Multiple log levels
- **Implementation**: java.util.logging or System.err with formatting

#### 21. Metrics Tool ✅
- Track and report usage metrics
- Performance monitoring
- Custom metric collection
- **Implementation**: In-memory counters, timestamps

#### 22. Tracing Tool ✅
- Distributed tracing for agent workflows
- Span tracking with parent/child relationships
- **Implementation**: Manual span tracking with IDs, timestamps

## Priority Recommendations

### Phase 1: Core Utilities (Quick Wins)
1. **HTTP Client Tool** ✅ **COMPLETED** - Generic REST API client (extends WebFetch)
2. **File System Tool** ✅ **COMPLETED** - Read/write/list files with safety
3. **Regex Tool** ✅ **COMPLETED** - Text extraction and pattern matching
4. **JSON Query Tool** ✅ **COMPLETED** - Query and transform JSON data
5. **Math/Calculator Tool** - Calculations and formula evaluation

### Phase 2: External Integrations
6. **GitHub Tool** - Repository and issue management via API
7. **Slack/Discord Webhook Tool** - Notifications and messaging
8. **Translation Tool** - API-based translation service
9. **HTML Parser Tool** - Basic web scraping and extraction

### Phase 3: RAG & Advanced Features
10. **Document Chunker Tool** - Intelligent document splitting
11. **CSV Tool** - Tabular data processing
12. **Cache Tool** - Inter-call data persistence
13. **Hybrid Search Tool** - BM25 + vector search
14. **Text Summarization Tool** - LLM-based summarization

### Phase 4: Workflow & Observability
15. **Validation Tool** - Schema and rule validation
16. **Retry Tool** - Resilient tool execution
17. **Workflow Tool** - Action sequencing and orchestration
18. **Multi-Agent Tool** - Agent coordination (already partially exists)
19. **Logging Tool** - Structured logging
20. **Metrics Tool** - Usage tracking
21. **Tracing Tool** - Workflow tracing
22. **Code Executor Tool** - JavaScript execution (limited scope)

## Tools Excluded (Require External Dependencies)

The following tools are **not feasible** with zero-dependency constraint:

- ❌ **PDF Reader Tool** - PDF parsing requires specialized libraries (Apache PDFBox, etc.)
- ❌ **Image Analysis Tool** - OCR and vision require ML libraries/models
- ❌ **XML Parser Tool** - Complex, would need proper parser library
- ❌ **Excel Tool** - Binary Excel format requires Apache POI or similar
- ❌ **Database Query Tool** - Requires JDBC drivers for each database
- ❌ **Reranking Tool** - Requires ML models (cross-encoders)
- ❌ **Email Tool** - May require JavaMail if not in JDK

## Implementation Notes

- All tools should extend `CallableTool[A]` trait
- Use `@schema.Tool` and `@Param` annotations for schema generation
- Include comprehensive error handling with `Try[A]` return types
- Write unit tests for each tool (see existing `*Spec.scala` files)
- Document tools with usage examples
- Consider security implications (sandboxing, rate limiting, access control)
- **Zero dependencies**: Use only JVM stdlib and existing Branch modules
