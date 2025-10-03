---
title: Hollywood
description: LLM Agents
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - llm
  - agent
  - llama-server
  - llama.cpp
---

# Hollywood

A library for LLM Agents.

Focused on local LLMs. It is built to use with llama-server, and primarily using gpt-oss. llama-server is supposed to
be "open ai compatible", so presumably this library would work with others llm servers that are as well, though no real
testing has gone into that.

Here is what I am using to start llama-server, which allows gpt-oss to completions and embeddings:

```bash
`llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean`
```

## Agent

### One Shot Agent

### RAG Agent

#### Vector Store

### Conversational Agent

#### Conversation State

## Tools

### CallableTool

### Tool Executor

### Tool Registry

#### Tool Schema

#### Agents as Tools

Talk about Agent.deriveAgentTool

## Tests

Tests are there, but ignored by default due to spinning up llama server.
