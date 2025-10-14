# Keanu Examples

Keanu is a lightweight actor system library for Scala that provides:
- Message passing with tell/ask patterns
- Supervision strategies for fault tolerance
- Actor hierarchies for organizing complex systems
- EventBus for publish-subscribe messaging

## Prerequisites

No external dependencies required. All examples are self-contained and run in-memory.

## Available Examples

### Actor Examples

#### 1. Basic Actor Usage
**Location:** `actors/basic/BasicActorExample.scala`

Demonstrates fundamental actor operations:
- Creating actors with `ActorSystem`
- Sending fire-and-forget messages with `tell`
- Request-response pattern with `ask` and Futures
- Actor lifecycle hooks (preStart, postStop)
- Multiple independent actors
- Graceful system shutdown

**Key Concepts:**
- `Actor` trait with `onMsg` message handler
- `ActorProps.props[T]` for actor registration
- `system.tell[ActorType]` for fire-and-forget messaging
- `system.ask[ActorType, ResponseType]` for request-response
- Pattern matching on messages and Ask patterns
- ConsoleLogger for debugging

**Run:**
```bash
sbt "examples/runMain keanu.actors.basic.BasicActorExample"
```

#### 2. Supervision Strategies
**Location:** `actors/supervision/SupervisionExample.scala`

Illustrates fault tolerance and error recovery:
- **RestartStrategy:** Actor restarts after failure, preserving mailbox
- **StopStrategy:** Actor stops permanently after failure
- **RestartWithBackoff:** Exponential backoff with retry limits
- Lifecycle hooks: preRestart, postRestart
- Custom error handling per actor type

**Key Concepts:**
- `supervisorStrategy` override for fault tolerance
- `RestartStrategy` - default resilient behavior
- `StopStrategy` - fail-fast for fatal errors
- `RestartWithBackoff(minBackoff, maxBackoff, maxRetries, resetAfter)`
- Lifecycle hook execution order during restarts
- Actor state persistence across restarts

**Run:**
```bash
sbt "examples/runMain keanu.actors.supervision.SupervisionExample"
```

#### 3. Actor Hierarchies
**Location:** `actors/hierarchy/HierarchyExample.scala`

Shows parent-child relationships and organization:
- Creating child actors from parent actors
- Hierarchical addressing with ActorPath
- Path-based actor selection
- Broadcasting messages to all children
- Nested hierarchies (children of children)
- Listing and managing child actors

**Key Concepts:**
- `ActorPath` with `/` operator for child creation
- `context.system.actorOf[T](path)` for child creation
- `system.actorSelection(pathString)` for finding actors
- `system.getChildren(path)` for listing children
- `context.children` for accessing children from parent
- Path formats: `/user/parent/child`

**Run:**
```bash
sbt "examples/runMain keanu.actors.hierarchy.HierarchyExample"
```

### EventBus Examples

#### 4. Basic EventBus
**Location:** `eventbus/basic/BasicEventBusExample.scala`

Demonstrates publish-subscribe messaging:
- Creating an EventBus for specific event types
- Publishing messages with topics
- Creating subscribers with message handlers
- Multiple subscribers receiving the same events
- Unsubscribing from event streams
- High-volume event handling (100+ events)

**Key Concepts:**
- `EventBus[EventType]` for type-safe pub/sub
- `Subscriber[T]` with `onMsg` handler
- `eventBus.publish(topic, event)` for sending events
- `eventBus.subscribe(subscriber)` for receiving events
- `eventBus.unsubscribe(subscriptionId)` to stop receiving
- `EventBusMessage[T]` containing topic and payload

**Run:**
```bash
sbt "examples/runMain keanu.eventbus.basic.BasicEventBusExample"
```

#### 5. Filtered EventBus
**Location:** `eventbus/filters/FilteredEventBusExample.scala`

Showcases advanced filtering capabilities:
- Topic-based filtering (only "auth" or "database" events)
- Severity-based filtering (ERROR, WARNING levels)
- Complex business logic filters (production services only)
- Time-based filtering (recent events only)
- Predicate-based subscriber filters
- Statistics and monitoring patterns

**Key Concepts:**
- `eventBus.subscribe(subscriber, filter = msg => condition)`
- Filter predicates accessing `msg.topic` and `msg.payload`
- Combining multiple conditions (AND, OR logic)
- Custom filtering logic per subscriber
- Zero-copy filtering (events not delivered if filtered)

**Run:**
```bash
sbt "examples/runMain keanu.eventbus.filters.FilteredEventBusExample"
```

## Common Patterns

### Creating an Actor System

```scala
val system = new ActorSystem {
  override def logger: ActorLogger = ConsoleLogger()
}

// Register actor types before use
system.registerProp(ActorProps.props[MyActor](EmptyTuple))
```

### Defining an Actor

```scala
case class MyActor() extends Actor {
  def onMsg: PartialFunction[Any, Any] = {
    case SomeMessage(data) =>
      println(s"Received: $data")
      // Optional: return response value

    case ask: Ask[?] =>
      ask.message match {
        case GetData() =>
          ask.complete(someData)  // Send response back
      }
  }

  override def preStart(): Unit = {
    println("Actor starting")
  }
}
```

### Message Patterns

**Fire-and-Forget (Tell):**
```scala
system.tell[MyActor]("myActor", SomeMessage("hello"))
```

**Request-Response (Ask):**
```scala
val future: Future[ResponseType] =
  system.ask[MyActor, ResponseType]("myActor", GetData())

future.onComplete {
  case Success(data) => println(s"Got: $data")
  case Failure(err)  => println(s"Failed: $err")
}
```

### Actor Hierarchies

```scala
// Create child from parent
case class ParentActor() extends Actor {
  def onMsg: PartialFunction[Any, Any] = {
    case CreateChild(name) =>
      val childPath = context.self.path / name
      context.system.actorOf[ChildActor](childPath)
  }
}

// Select and message children
system.actorSelection("/user/parent/child").foreach { childRef =>
  system.tell(childRef, SomeMessage("hello"))
}
```

### EventBus Usage

```scala
// Define event types
case class UserEvent(userId: String, action: String)

// Create bus
val eventBus = new EventBus[UserEvent] {}

// Create subscriber
val subscriber = Subscriber[UserEvent] { msg =>
  println(s"${msg.topic}: ${msg.payload}")
}

// Subscribe with optional filter
eventBus.subscribe(
  subscriber,
  filter = msg => msg.topic == "important"
)

// Publish events
eventBus.publish("important", UserEvent("user123", "login"))
```

### Supervision Strategies

```scala
case class ResilientActor() extends Actor {
  def onMsg: PartialFunction[Any, Any] = {
    case Work(data) if data.isInvalid =>
      throw new IllegalArgumentException("Bad data")
    case Work(data) =>
      processWork(data)
  }

  // Choose supervision strategy
  override def supervisorStrategy: SupervisionStrategy =
    RestartStrategy  // Default: restart on any error
    // StopStrategy  // Alternative: stop on any error
    // RestartWithBackoff(100.millis, 2.seconds, maxRetries = Some(3))
}
```

## Key Features

### Virtual Threads
- Actors run on virtual threads (Project Loom)
- Lightweight and efficient - thousands of actors with minimal overhead
- No thread pool configuration needed

### Type Safety
- Type-safe message passing
- Compile-time verification of actor types and message types
- Generic ask patterns with return type checking

### Fault Tolerance
- Configurable supervision strategies per actor
- Automatic restart on failure (RestartStrategy)
- Exponential backoff for transient failures
- Lifecycle hooks for cleanup and reinitialization

### Hierarchical Organization
- Organize actors in parent-child trees
- Path-based addressing (`/user/parent/child`)
- Children lifecycle tied to parent
- Supervisor pattern for managing worker pools

### Event-Driven Communication
- EventBus for publish-subscribe patterns
- Topic-based routing
- Predicate filtering for selective delivery
- Async message delivery via virtual threads

## Error Handling

### Actor Failures
Actors can fail during message processing. The supervision strategy determines what happens:

```scala
// RestartStrategy: Actor restarts, preserving mailbox
override def supervisorStrategy = RestartStrategy

// StopStrategy: Actor stops, must be recreated manually
override def supervisorStrategy = StopStrategy

// RestartWithBackoff: Retries with increasing delays
override def supervisorStrategy = RestartWithBackoff(
  minBackoff = 100.millis,
  maxBackoff = 2.seconds,
  maxRetries = Some(3),
  resetAfter = Some(5.seconds)
)
```

### EventBus Errors
EventBus provides error callbacks:

```scala
val eventBus = new EventBus[MyEvent] {
  override def onPublishError(
    error: Throwable,
    message: EventBusMessage[MyEvent],
    subscriptionId: UUID
  ): Unit = {
    println(s"Error publishing to $subscriptionId: ${error.getMessage}")
  }
}

val subscriber = new Subscriber[MyEvent] {
  override def onError(
    error: Throwable,
    message: EventBusMessage[MyEvent]
  ): Unit = {
    println(s"Error handling message: ${error.getMessage}")
  }

  override def onMsg(msg: EventBusMessage[MyEvent]): Unit = {
    // Process message
  }
}
```

## Lifecycle Hooks

Actors provide hooks for managing state:

```scala
case class MyActor() extends Actor {
  override def preStart(): Unit = {
    // Called once when actor first starts
    // Initialize resources, open connections, etc.
  }

  override def postStop(): Unit = {
    // Called once when actor stops
    // Clean up resources, close connections, etc.
  }

  override def preRestart(reason: Throwable): Unit = {
    // Called before restarting due to failure
    // Default: calls postStop()
  }

  override def postRestart(reason: Throwable): Unit = {
    // Called after restarting due to failure
    // Default: calls preStart()
  }
}
```

## Additional Resources

- **Source Code:** `branch/src/main/scala/dev/alteration/branch/keanu/`
- **Tests:** `branch/src/test/scala/dev/alteration/branch/keanu/`
- **Documentation:** See inline scaladoc in source files

## Tips

1. **Use tell for fire-and-forget** messaging when you don't need a response
2. **Use ask for request-response** when you need to wait for a result
3. **Choose the right supervision strategy** for your use case:
   - RestartStrategy for transient failures
   - StopStrategy for fatal errors
   - RestartWithBackoff for external service failures
4. **Organize actors hierarchically** to model your domain
5. **Use EventBus for pub/sub** when multiple components need the same events
6. **Add filters to subscribers** to reduce unnecessary message processing
7. **Use ConsoleLogger** during development for visibility
8. **Shut down gracefully** with `system.shutdownAwait()` to ensure cleanup
9. **Keep actors focused** - one responsibility per actor type
10. **Make actors stateful** - they're meant to encapsulate mutable state safely
