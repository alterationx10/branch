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

## Overview

Keanu consists of two major components:

- **[EventBus](/keanu/eventbus)** - Publish-subscribe messaging system with typed messages and optional topic filtering
- **[ActorSystem](/keanu/actors)** - Local actor-based concurrency with supervision and lifecycle management
- **[Advanced Features](/keanu/advanced)** - Supervision strategies, mailbox types, dead letters, and lifecycle management

## Quick Start

### EventBus Example

```scala
import dev.alteration.branch.keanu.eventbus.*

object IntEventBus extends EventBus[Int]

val subscriber = Subscriber[Int] { msg =>
  println(s"Got message: ${msg.payload}")
}

IntEventBus.subscribe(subscriber)
IntEventBus.publish("calculations", 42)
```

### ActorSystem Example

```scala
import dev.alteration.branch.keanu.actors.*

case class CounterActor() extends Actor {
  private var count = 0

  override def onMsg: PartialFunction[Any, Any] = {
    case "increment" =>
      count += 1
      count
    case "get" => count
  }
}

val system = ActorSystem()
system.registerProp(ActorProps.props[CounterActor]())

system.tell[CounterActor]("counter1", "increment")
system.tell[CounterActor]("counter1", "get")
```

## Features

### EventBus
- Typed publish-subscribe messaging
- Optional topic filtering
- Per-subscriber virtual threads
- Error handling with `onError` callbacks
- AutoCloseable for resource management

### ActorSystem
- Type-safe actor creation and messaging
- Hierarchical actor paths
- Supervision strategies (restart, stop, backoff)
- Request-response pattern (ask)
- Dead letter queue for debugging
- Configurable mailbox types
- Lifecycle hooks

## Documentation

- **[EventBus](/keanu/eventbus)** - Publish-subscribe messaging system
- **[ActorSystem](/keanu/actors)** - Actor creation, TypedActor, StatefulActor
- **[Advanced Features](/keanu/advanced)** - Supervision, mailboxes, and lifecycle management

## Next Steps

- Learn about the [EventBus](/keanu/eventbus)
- Explore the [ActorSystem](/keanu/actors)
- Dive into [Advanced Features](/keanu/advanced)
