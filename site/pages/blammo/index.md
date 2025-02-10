---
title: Blammo
description: java.util.logging helpers
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - logging
  - json
---

# Blammo

Blammo provides enhanced functionality for `java.util.logging`, with a focus on structured logging through JSON formatting.

## Core Features

### JSON Formatter

The `JsonFormatter` class formats log records as JSON objects, making them easier to parse and analyze with log management tools. Each log entry includes:

- Timestamp
- Log level
- Logger name
- Message
- Thread information
- Stack traces (for errors)

Example JSON output:

```json
{
  "timestamp": "2025-01-25T04:36:00Z",
  "level": "INFO",
  "logger": "com.example.MyApp",
  "message": "Application started",
  "thread": "main"
}
```

### JsonConsoleLogger Trait

The `JsonConsoleLogger` trait provides a convenient way to add JSON logging to your classes:

```scala
class MyService extends JsonConsoleLogger {
  // Logger is automatically available
  logger.info("Service initialized")

  def doSomething(): Unit = {
    logger.fine("Performing operation")
    // ... your code ...
    logger.info("Operation completed")
  }
}
```

## Usage

### Setting up the JSON Formatter

```scala
import java.util.logging.{Logger, ConsoleHandler}
import dev.wishingtree.branch.blammo.JsonFormatter

val logger = Logger.getLogger("MyApp")
val handler = new ConsoleHandler()
handler.setFormatter(new JsonFormatter())
logger.addHandler(handler)
```

### Using the JsonConsoleLogger Trait

```scala
import dev.wishingtree.branch.blammo.JsonConsoleLogger

class MyApp extends JsonConsoleLogger {
  def start(): Unit = {
    logger.info("Starting application")
    try {
      // Your application code
    } catch {
      case e: Exception =>
        // Stack trace will be included in JSON output
        logger.severe(s"Application failed: ${e.getMessage}")
    }
  }
}
```

## Integration with Other Libraries

Blammo works seamlessly with other Branch libraries:

- [Lzy](/lzy) can use Blammo for its logging functionality
- [Piggy](/piggy) can use Blammo for SQL query logging
- [Ursula](/ursula) can use Blammo for CLI application logging

## Custom Formatter

You can customize the JSON output format by extending the `Formatter` class and overriding the `format` method similar to how the `JsonFormatter` works:

```scala
import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.*
import java.util.logging.{Formatter, LogRecord}

class CustomJsonFormatter extends Formatter {
  override def format(record: LogRecord): String = {
    Json.obj(
      "name" -> jsonOrNull(record.getLoggerName),
      "level" -> JsonString(record.getLevel.toString),
      "time" -> JsonString(record.getInstant.toString),
      "message" -> jsonOrNull(record.getMessage),
      "jsonMessage" -> Json.parse(record.getMessage).getOrElse(JsonNull),
      "error" -> Json.throwable(record.getThrown),
      // Add custom fields
      "environment" -> JsonString(sys.env.getOrElse("ENV", "development")),
      "application" -> JsonString("MyApp")
    ).toJsonString + System.lineSeparator()
  }
}
```

This will produce log entries with additional fields:

```json
{
  "name": "com.example.MyApp",
  "level": "INFO",
  "time": "2025-01-25T04:36:00Z",
  "message": "Application started",
  "jsonMessage": null,
  "error": null,
  "environment": "production",
  "application": "MyApp"
}
```
