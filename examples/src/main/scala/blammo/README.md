# Blammo Examples

Examples for the `blammo` module, which provides observability features for Scala applications including structured logging, metrics, and distributed tracing.

## What is Blammo?

Blammo provides three main observability features:

1. **Structured JSON Logging**: Environment-aware logging with JSON formatting
2. **JMX Metrics**: Expose application metrics via JMX (gauges, counters, histograms)
3. **Distributed Tracing**: Trace request flows through structured log events

## Examples

### Logging - Structured JSON Logs

**Location**: `blammo/logging/LoggingExample.scala`

Demonstrates how to:
- Use `JsonConsoleLogger` for structured logging
- Log JSON-formatted messages for easy parsing
- Leverage environment-aware log levels (DEV/TEST/PROD)
- Log exceptions with full context

**Run**:
```bash
# Run with default (DEV) environment
sbt "examples/runMain blammo.logging.LoggingExample"

# Run in production mode (fewer logs)
export SCALA_ENV=PROD
sbt "examples/runMain blammo.logging.LoggingExample"

# Pretty-print JSON output with jq
sbt "examples/runMain blammo.logging.LoggingExample" 2>&1 | grep jsonMessage | jq '.jsonMessage'
```

**Log Levels by Environment**:
- **DEV**: `Level.ALL` (all logs including debug)
- **TEST/STAGING**: `Level.INFO` (info and above)
- **PROD**: `Level.WARNING` (warnings and errors only)

### Metrics - JMX Monitoring

**Location**: `blammo/metrics/MetricsExample.scala`

Demonstrates how to:
- Define gauges (values that fluctuate)
- Define counters (monotonically increasing values)
- Define histograms (distribution/summary data)
- Expose metrics via JMX
- Query metrics programmatically

**Run**:
```bash
# Start the example (runs for 30 seconds)
sbt "examples/runMain blammo.metrics.MetricsExample"

# In another terminal, connect with JConsole
jconsole
# Navigate to: MBeans -> com.example.myapp -> type=ApiService -> Metrics
```

**Metric Types**:
- **Gauge**: Current connections, queue size, memory usage
- **Counter**: Total requests, errors, items processed
- **Histogram**: Average response time, percentiles, latency

**JMX Integration**:
Metrics can be accessed by:
- JConsole (built-in JDK tool)
- VisualVM
- Prometheus JMX Exporter
- Any JMX-compatible monitoring tool

### Tracing - Distributed Traces

**Location**: `blammo/tracing/TracerExample.scala`

Demonstrates how to:
- Create traced operations with `traced()`
- Nest spans to show call hierarchies
- Add custom attributes to spans
- Log span events
- Handle errors in traced operations with `tracedTry()`

**Run**:
```bash
# Run the example
sbt "examples/runMain blammo.tracing.TracerExample"

# Pretty-print trace spans with jq
sbt "examples/runMain blammo.tracing.TracerExample" 2>&1 | grep jsonMessage | jq '.jsonMessage'
```

**Trace Events**:
Each trace generates structured JSON logs:
- `span.start`: Operation begins (includes traceId, spanId, parentSpanId)
- `span.event`: Events within a span
- `span.end`: Operation completes (includes duration, success status, errors)

**Integration with Log Aggregators**:
The trace logs can be collected and visualized by:
- Grafana Loki
- AWS CloudWatch Insights
- OpenSearch/Elasticsearch
- Splunk
- Any JSON log aggregator

## Combining Features

You can use all three features together:

```scala
object MyService extends JsonConsoleLogger with Tracer {

  val metrics = Metrics("MyService")
    .counter("Requests") { requestCount }
    .register()

  def handleRequest(): Response = traced("handle.request") {
    logger.info("Processing request")
    // Your logic here
  }
}
```

## Environment Configuration

Blammo integrates with the `veil` module for environment detection via the `SCALA_ENV` environment variable:

- **DEV** (default): Maximum logging, all traces
- **TEST**: Reduced logging, all traces
- **STAGING**: Reduced logging, all traces
- **PROD**: Minimal logging (warnings/errors), all traces

Set the environment:
```bash
export SCALA_ENV=PROD
```

## Tips

1. **Use structured logging**: Log JSON objects instead of strings for better parsing
2. **Name metrics clearly**: Use descriptive names like "ActiveConnections" not "ac"
3. **Trace critical paths**: Focus tracing on important request flows
4. **Set appropriate log levels**: Use `logger.fine()` for debug, `logger.info()` for normal operation
5. **Monitor metrics in production**: Connect JMX to your monitoring system
