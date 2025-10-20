---
title: Keanu EventBus
description: Typed publish-subscribe messaging system
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - eventbus
  - pubsub
---

# EventBus

The EventBus provides a publish-subscribe messaging system with typed messages and optional topic filtering.

## Basic Usage

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

## Publishing Messages

```scala
// Publish with a topic
IntEventBus.publish("calculations", 42)

// Publish without a topic
IntEventBus.publishNoTopic(42)
```

## Filtered Subscriptions

Subscribe with a filter to only receive specific messages:

```scala
// Only receive messages with topic "important"
IntEventBus.subscribe(
  new LoggingSubscriber,
  msg => msg.topic == "important"
)
```

## Unsubscribing and Cleanup

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

## Error Handling

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

## Implementation Details

- Each subscriber gets its own message queue and virtual thread for processing
- Messages are processed asynchronously but in order for each subscriber
- Exceptions in `onMsg` are caught and passed to `onError` (defaults to no-op)
- Exceptions in filters or mailbox operations are caught and passed to `onPublishError`
- New subscribers may miss in-flight messages
- Unsubscribing properly shuts down the subscriber's thread
- EventBus and Subscriber both implement AutoCloseable for resource management

## Example: Chat System

```scala
case class ChatMessage(user: String, text: String, timestamp: Instant)

object ChatEventBus extends EventBus[ChatMessage]

class ChatLogger extends Subscriber[ChatMessage] {
  override def onMsg(msg: EventBusMessage[ChatMessage]): Unit = {
    val chat = msg.payload
    println(s"[${chat.timestamp}] ${chat.user}: ${chat.text}")
  }
}

class ChatNotifier extends Subscriber[ChatMessage] {
  override def onMsg(msg: EventBusMessage[ChatMessage]): Unit = {
    val chat = msg.payload
    sendNotification(s"New message from ${chat.user}")
  }

  // Only notify for messages in the "announcements" topic
  override def onError(error: Throwable, message: EventBusMessage[ChatMessage]): Unit = {
    println(s"Failed to send notification: ${error.getMessage}")
  }
}

// Setup
val logger = new ChatLogger()
val notifier = new ChatNotifier()

ChatEventBus.subscribe(logger)
ChatEventBus.subscribe(
  notifier,
  msg => msg.topic == "announcements"
)

// Use
ChatEventBus.publish(
  "announcements",
  ChatMessage("admin", "System maintenance at 10pm", Instant.now())
)
```

## Next Steps

- Explore the [ActorSystem](actors.md) for more complex concurrent patterns
- Learn about [Advanced Features](advanced.md)
- Return to [Keanu Overview](index.md)
