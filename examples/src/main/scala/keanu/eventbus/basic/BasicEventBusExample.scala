package keanu.eventbus.basic

import dev.alteration.branch.keanu.eventbus.*

/** A basic example showing event bus pub/sub functionality.
  *
  * Keanu EventBus provides:
  *   - Publish-subscribe messaging pattern
  *   - Topic-based routing
  *   - Async message delivery via subscribers
  *   - Filter-based subscriptions
  *
  * This example demonstrates:
  *   - Creating an event bus
  *   - Publishing messages
  *   - Subscribing to receive messages
  *   - Using topics for routing
  *
  * To run this example: sbt "examples/runMain
  * keanu.eventbus.basic.BasicEventBusExample"
  */
object BasicEventBusExample {

  // Define event types
  case class UserLoggedIn(userId: String, timestamp: Long)
  case class OrderPlaced(orderId: String, amount: Double, userId: String)
  case class SystemAlert(level: String, message: String)

  // Union type for all events
  type AppEvent = UserLoggedIn | OrderPlaced | SystemAlert

  def main(args: Array[String]): Unit = {
    println("=== Keanu EventBus Basic Example ===\n")

    // Create an event bus
    val eventBus = new EventBus[AppEvent] {
      override def onPublishError(
          error: Throwable,
          message: EventBusMessage[AppEvent],
          subscriptionId: java.util.UUID
      ): Unit = {
        println(
          s"[EventBus] Error publishing message: ${error.getMessage}"
        )
      }
    }

    println("--- Example 1: Simple subscriber ---")
    // Create a subscriber that logs all events
    val loggingSubscriber = new Subscriber[AppEvent] {
      override def onMsg(msg: EventBusMessage[AppEvent]): Unit = {
        val topic   = if (msg.topic.isEmpty) "default" else msg.topic
        val payload = msg.payload
        println(s"[Logger] Topic: $topic, Event: $payload")
      }

      override def onError(
          error: Throwable,
          message: EventBusMessage[AppEvent]
      ): Unit = {
        println(s"[Logger] Error: ${error.getMessage}")
      }
    }

    // Subscribe to receive all events
    val loggerId = eventBus.subscribe(loggingSubscriber)
    println(s"Logger subscribed with ID: $loggerId\n")

    // Publish some events
    eventBus.publish(
      "auth",
      UserLoggedIn("user123", System.currentTimeMillis())
    )
    eventBus.publish(
      "orders",
      OrderPlaced("order456", 99.99, "user123")
    )
    eventBus.publishNoTopic(
      SystemAlert("INFO", "System running normally")
    )

    // Give subscribers time to process
    Thread.sleep(100)
    println()

    println("--- Example 2: Multiple subscribers ---")
    // Create a subscriber focused on orders
    val orderSubscriber = new Subscriber[AppEvent] {
      override def onMsg(msg: EventBusMessage[AppEvent]): Unit = {
        msg.payload match {
          case OrderPlaced(orderId, amount, userId) =>
            println(
              s"[OrderProcessor] Processing order $orderId: $$${amount} for user $userId"
            )
          case _                                    => // Ignore other events
        }
      }
    }

    // Create a subscriber for authentication events
    val authSubscriber = new Subscriber[AppEvent] {
      private var loginCount = 0
      override def onMsg(msg: EventBusMessage[AppEvent]): Unit = {
        msg.payload match {
          case UserLoggedIn(userId, _) =>
            loginCount += 1
            println(
              s"[AuthMonitor] User $userId logged in (total logins: $loginCount)"
            )
          case _                       => // Ignore other events
        }
      }
    }

    // Subscribe both
    val orderId = eventBus.subscribe(orderSubscriber)
    val authId  = eventBus.subscribe(authSubscriber)
    println(
      s"OrderProcessor subscribed: $orderId, AuthMonitor subscribed: $authId\n"
    )

    // Publish more events
    eventBus.publish(
      "auth",
      UserLoggedIn("user456", System.currentTimeMillis())
    )
    eventBus.publish(
      "orders",
      OrderPlaced("order789", 149.99, "user456")
    )
    eventBus.publish(
      "auth",
      UserLoggedIn("user789", System.currentTimeMillis())
    )
    eventBus.publish(
      "orders",
      OrderPlaced("order101", 49.99, "user123")
    )

    Thread.sleep(100)
    println()

    println("--- Example 3: Unsubscribing ---")
    // Unsubscribe the order processor
    eventBus.unsubscribe(orderId)
    println(s"OrderProcessor unsubscribed\n")

    // These events will only be seen by logger and auth monitor
    eventBus.publish(
      "auth",
      UserLoggedIn("user999", System.currentTimeMillis())
    )
    eventBus.publish(
      "orders",
      OrderPlaced("order202", 299.99, "user999")
    )

    Thread.sleep(100)
    println()

    println("--- Example 4: High volume events ---")
    println("Publishing 100 events...")
    for (i <- 1 to 100) {
      if (i % 3 == 0) {
        eventBus.publish(
          "auth",
          UserLoggedIn(s"user$i", System.currentTimeMillis())
        )
      } else {
        eventBus.publish(
          "orders",
          OrderPlaced(s"order$i", i * 10.0, s"user$i")
        )
      }
    }

    // Wait for processing
    Thread.sleep(500)
    println()

    // Cleanup
    println("--- Cleanup ---")
    eventBus.shutdown()
    println("EventBus shut down")

    println("\n=== Example Complete ===")
  }
}
