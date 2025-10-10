---
title: Keanu
description: EventBus and ActorSystem
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags: 
  - eventbus
  - actor
---
# Keanu

This module provides a typed EventBus implementation and a local ActorSystem for message-based concurrency patterns.

## EventBus

The EventBus provides a publish-subscribe messaging system with typed messages and optional topic filtering.

### Basic Usage

Extend `EventBus[T]` for your message type:

```scala
object IntEventBus extends EventBus[Int]
```

Create subscribers by implementing the `Subscriber[T]` trait or using the factory method:

```scala
// Using a class
class LoggingSubscriber extends Subscriber[Int] {
  override def onMsg(msg: EventBusMessage[Int]): Unit =
    println(s"Got message on topic '${msg.topic}': ${msg.payload}")

  // Optional: handle errors in message processing
  override def onError(error: Throwable, message: EventBusMessage[Int]): Unit =
    println(s"Error processing message: ${error.getMessage}")
}

// Using the factory method
val subscriber = Subscriber[Int] { msg =>
  println(s"Got message: ${msg.payload}")
}
IntEventBus.subscribe(subscriber)
```

### Publishing Messages

```scala
// Publish with a topic
IntEventBus.publish("calculations", 42)

// Publish without a topic
IntEventBus.publishNoTopic(42)
```

### Filtered Subscriptions

Subscribe with a filter to only receive specific messages:

```scala
// Only receive messages with topic "important"
IntEventBus.subscribe(
  new LoggingSubscriber, 
  msg => msg.topic == "important"
)
```

### Unsubscribing and Cleanup

```scala
// Unsubscribe by subscriber instance
IntEventBus.unsubscribe(subscriber)

// Unsubscribe by subscription ID
val subId = IntEventBus.subscribe(subscriber)
IntEventBus.unsubscribe(subId)

// Shutdown the entire event bus (stops all subscribers)
IntEventBus.shutdown()

// Or use try-with-resources pattern
object MyEventBus extends EventBus[String]

def example(): Unit = {
  val subscriber = Subscriber[String](msg => println(msg.payload))
  MyEventBus.subscribe(subscriber)
  try {
    MyEventBus.publish("events", "Hello!")
  } finally {
    MyEventBus.close() // AutoCloseable
  }
}
```

### Error Handling

Override `onPublishError` to handle errors during message publishing:

```scala
object MonitoredEventBus extends EventBus[Int] {
  override def onPublishError(
      error: Throwable,
      message: EventBusMessage[Int],
      subscriptionId: UUID
  ): Unit = {
    logger.warn(s"Failed to publish message to $subscriptionId: ${error.getMessage}")
    metrics.incrementCounter("eventbus.publish.errors")
  }
}
```

### Implementation Details

- Each subscriber gets its own message queue and virtual thread for processing
- Messages are processed asynchronously but in order for each subscriber
- Exceptions in `onMsg` are caught and passed to `onError` (defaults to no-op)
- Exceptions in filters or mailbox operations are caught and passed to `onPublishError`
- New subscribers may miss in-flight messages
- Unsubscribing properly shuts down the subscriber's thread
- EventBus and Subscriber both implement AutoCloseable for resource management

## ActorSystem

The ActorSystem provides local actor-based concurrency with supervision and lifecycle management.

### Creating Actors

Define actors by extending the `Actor` trait:

```scala
case class EchoActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case msg => println(s"Echo: $msg")
  }
}

case class CounterActor(actorSystem: ActorSystem) extends Actor {
  private var count = 0
  
  override def onMsg: PartialFunction[Any, Any] = {
    case n: Int =>
      count += n
      actorSystem.tell[EchoActor]("echo", s"Count is now $count")
    case "get" => count
    case "print" => println(s"Count is $count")
  }
}
```

### Setting Up the ActorSystem

```scala
// Create a new ActorSystem
val system = ActorSystem()

// Register actor types with their constructor arguments
system.registerProp(ActorProps.props[EchoActor]())
system.registerProp(ActorProps.props[CounterActor](system))
```

### Sending Messages

```scala
// Send messages to named actor instances
system.tell[CounterActor]("counter1", 5)
system.tell[CounterActor]("counter1", "get")
system.tell[EchoActor]("echo1", "Hello!")

// Helper for repeated messages to same actor
val counter = system.tell[CounterActor]("counter1", _)
counter(1)
counter(2)
counter("print")
```

### Request-Response Pattern (Ask)

The `ask` method enables request-response communication with actors, returning a `Future` with the response:

```scala
import scala.concurrent.duration.*
import scala.concurrent.Await

case class QueryActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case ask: Ask[?] =>
      ask.message match {
        case "count" => ask.complete(42)
        case query: String => ask.complete(s"Result for: $query")
        case _ => ask.fail(new IllegalArgumentException("Unknown message"))
      }
  }
}

// Register the actor
system.registerProp(ActorProps.props[QueryActor]())

// Ask for a response with a timeout
val future = system.ask[QueryActor, Int]("query1", "count", 5.seconds)
val result = Await.result(future, 5.seconds) // result: Int = 42

// Handle responses asynchronously
system.ask[QueryActor, String]("query1", "search", 5.seconds).foreach { result =>
  println(s"Got result: $result")
}
```

Actors should pattern match on `Ask[?]` to handle request-response messages. Use the `complete` method to send a successful response or `fail` to send an error.

### Actor Hierarchy and Context

Each actor has an `ActorContext` that provides access to its identity, hierarchy, and lifecycle management:

```scala
case class SupervisorActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case "create-workers" =>
      // Create child actors under this supervisor
      val worker1 = context.actorOf[WorkerActor]("worker1")
      val worker2 = context.actorOf[WorkerActor]("worker2")

      println(s"Created workers: ${context.children}")

    case "stop-worker" =>
      // Stop a specific child
      context.children.headOption.foreach(child => context.stop(child))

    case msg =>
      // Send message to all children
      context.children.foreach(child => context.system.tell(child, msg))
  }
}

case class WorkerActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case "parent" =>
      println(s"My parent is: ${context.parent}")
    case "self" =>
      println(s"I am: ${context.self}")
    case msg =>
      println(s"Worker ${context.self.path.name} processing: $msg")
  }
}
```

The `ActorContext` provides:
- `self` - Reference to this actor's identity
- `parent` - Reference to the parent actor (if any)
- `children` - List of all child actors
- `actorOf[A](name)` - Create a child actor
- `stop(child)` - Stop a child actor
- `system` - Access to the ActorSystem

### Actor Paths

Actors are organized in a hierarchical path structure similar to file systems. All user actors live under the `/user` root:

```scala
// Top-level actors
val actor1 = system.actorOf[MyActor](ActorPath.user("myActor"))
println(actor1.path) // /user/myActor

// Child actors
val supervisor = system.actorOf[SupervisorActor](ActorPath.user("supervisor"))
// Inside SupervisorActor:
// context.actorOf[WorkerActor]("worker1") creates /user/supervisor/worker1

// Path operations
val path = ActorPath.user("parent") / "child" / "grandchild"
println(path)                    // /user/parent/child/grandchild
println(path.name)               // "grandchild"
println(path.parent)             // Some(/user/parent/child)
println(path.isChildOf(ActorPath.user("parent")))  // false
println(path.isDescendantOf(ActorPath.user("parent"))) // true
```

### Key Features

- Actors are uniquely identified by hierarchical paths
- Parent-child relationships with supervision
- Automatic actor restart on failures
- Message delivery guarantees within a single actor
- Graceful shutdown with PoisonPill messages
- Type-safe actor props system for constructor arguments
- Request-response messaging with the ask pattern

### Lifecycle Management

The ActorSystem provides several lifecycle events and supervision features:

- Actors automatically restart on unhandled exceptions
- PoisonPill message for graceful termination
- Shutdown coordination with `shutdownAwait()`
- Various termination states tracked (initialization failure, interruption, etc.)

To shut down the ActorSystem:

```scala
system.shutdownAwait()
```

This will send PoisonPill to all actors and wait for them to terminate.