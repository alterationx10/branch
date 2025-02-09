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

Create subscribers by implementing the `Subscriber[T]` trait or using an anonymous function:

```scala
// Using a class
class LoggingSubscriber extends Subscriber[Int] {
  override def onMsg(msg: EventBusMessage[Int]): Unit = 
    println(s"Got message on topic '${msg.topic}': ${msg.payload}")
}

// Using an anonymous function
IntEventBus.subscribe((msg: EventBusMessage[Int]) => 
  println(s"Got message: ${msg.payload}"))
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

### Implementation Details

- Each subscriber gets its own message queue and virtual thread for processing
- Messages are processed asynchronously but in order for each subscriber
- Subscriber message handling is wrapped in Try to prevent exceptions from crashing the processing thread
- Subscribers can be unsubscribed using their UUID or subscriber instance

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

### Key Features

- Actors are uniquely identified by name and type
- Automatic actor restart on failures
- Message delivery guarantees within a single actor
- Graceful shutdown with PoisonPill messages
- Type-safe actor props system for constructor arguments

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