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
import dev.alteration.branch.blammo.JsonFormatter

val logger = Logger.getLogger("MyApp")
val handler = new ConsoleHandler()
handler.setFormatter(new JsonFormatter())
logger.addHandler(handler)
```

### Using the JsonConsoleLogger Trait

```scala
import dev.alteration.branch.blammo.JsonConsoleLogger

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

## Metrics

Blammo provides JMX-based metrics collection with support for gauges, counters, and histograms. Metrics are exposed via JMX and can be monitored with tools like JConsole, VisualVM, or collected by monitoring systems.

### Defining Metrics

```scala
import dev.alteration.branch.blammo.Metrics
import java.util.concurrent.atomic.AtomicLong

object MyService {
  private val requestCounter = new AtomicLong(0)
  private val activeConnections = new AtomicLong(0)
  private var avgResponseTime = 0.0

  // Define and register metrics
  val metrics = Metrics("MyService")
    .counter("RequestCount") { requestCounter.get() }
    .gauge("ActiveConnections") { activeConnections.get() }
    .histogram("AvgResponseTime") { avgResponseTime }
    .register()

  def handleRequest(): Unit = {
    requestCounter.incrementAndGet()
    activeConnections.incrementAndGet()
    try {
      // Handle request...
      avgResponseTime = calculateAvgResponseTime()
    } finally {
      activeConnections.decrementAndGet()
    }
  }
}
```

### Metric Types

- **Gauge** - A value that can go up or down (e.g., active connections, memory usage)
- **Counter** - A monotonically increasing value (e.g., total requests, errors)
- **Histogram** - Typically used for timing or distribution data (e.g., response times, request sizes)

### Reading Metrics

Metrics can be read back via JMX after registration:

```scala
// Read individual metrics
val requestCount = metrics.getCounter("RequestCount")
val activeConns = metrics.getGauge("ActiveConnections")
val avgTime = metrics.getHistogram("AvgResponseTime")

// Read all metrics of a type
val allCounters = metrics.getAllCounters      // Map[String, Long]
val allGauges = metrics.getAllGauges          // Map[String, Any]
val allHistograms = metrics.getAllHistograms  // Map[String, Double]

// Unregister when done
metrics.unregister()
```

### Custom Domains

By default, metrics are registered under `dev.alteration.branch`. You can specify a custom domain for your application:

```scala
val appMetrics = Metrics("UserService", domain = "com.example.myapp")
  .counter("LoginAttempts") { loginCounter.get() }
  .register()
// Available at: com.example.myapp:type=UserService,name=Metrics
```

## Distributed Tracing

The `Tracer` trait provides distributed tracing capabilities via structured logging. Traces are logged as JSON events that can be collected and visualized by log aggregators like Loki, CloudWatch, or OpenSearch.

### Basic Usage

Mix in the `Tracer` trait along with `BaseLogger` (like `JsonConsoleLogger`):

```scala
import dev.alteration.branch.blammo.{JsonConsoleLogger, Tracer}

object MyService extends JsonConsoleLogger with Tracer {

  def handleRequest(userId: String): Response = traced("http.request") {
    val user = traced("db.query") {
      fetchUser(userId)
    }

    traced("cache.set") {
      cacheUser(user)
    }

    traced("render.response") {
      buildResponse(user)
    }
  }
}
```

This will generate JSON log events like:

```json
{"event":"span.start","trace_id":"abc-123","span_id":"span-1","operation":"http.request","timestamp":1706169600000}
{"event":"span.start","trace_id":"abc-123","span_id":"span-2","parent_span_id":"span-1","operation":"db.query","timestamp":1706169600100}
{"event":"span.end","trace_id":"abc-123","span_id":"span-2","success":true,"duration_ms":45.2,"timestamp":1706169600145}
{"event":"span.end","trace_id":"abc-123","span_id":"span-1","success":true,"duration_ms":230.5,"timestamp":1706169600230}
```

### Adding Custom Attributes

You can attach custom key-value attributes to spans:

```scala
import dev.alteration.branch.friday.Json.*

traced(
  "db.query",
  attributes = Map(
    "user_id" -> JsonString(userId),
    "table" -> JsonString("users")
  )
) {
  database.query(userId)
}
```

### Handling Errors

The `traced` method automatically tracks failures and includes error information:

```scala
try {
  traced("risky.operation") {
    // If this throws, the span will be marked with success=false
    // and include error details
    riskyOperation()
  }
} catch {
  case e: Exception =>
    // Error already logged in span
    handleError(e)
}
```

Use `tracedTry` for explicit Try-based error handling:

```scala
val result = tracedTry("operation") {
  mightFail()
}

result match {
  case Success(value) => // Span marked successful
  case Failure(error) => // Span marked failed with error details
}
```

### Span Events

Add events within a span to mark important milestones:

```scala
traced("process.job") {
  spanEvent("validation.complete")
  validateInput()

  spanEvent("transformation.start", Map("rows" -> JsonNumber(1000)))
  transformData()

  spanEvent("save.complete")
  saveResults()
}
```

### Manual Span Management

For more control, manually start and end spans:

```scala
val span = startSpan("long.running.task", Map("job_id" -> JsonString("123")))

try {
  // Do work...
  spanEvent("checkpoint", Map("progress" -> JsonNumber(50)))
  // More work...
  endSpan(span, success = true)
} catch {
  case e: Exception =>
    endSpan(span, success = false, Map("error" -> JsonString(e.getMessage)))
}
```

## Integration with Other Libraries

Blammo works seamlessly with other Branch libraries:

- [Lzy](/lzy) can use Blammo for its logging functionality
- [Piggy](/piggy) can use Blammo for SQL query logging
- [Ursula](/ursula) can use Blammo for CLI application logging
- [Keanu](/keanu) actors can use Tracer for distributed tracing of message flows

## Custom Formatter

You can customize the JSON output format by extending the `Formatter` class and overriding the `format` method similar to how the `JsonFormatter` works:

```scala
import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.*
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
