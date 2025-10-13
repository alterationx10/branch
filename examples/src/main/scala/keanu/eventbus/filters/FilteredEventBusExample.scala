package keanu.eventbus.filters

import dev.alteration.branch.keanu.eventbus.*

/** An example showing event bus filtering capabilities.
  *
  * This example demonstrates:
  *   - Topic-based filtering
  *   - Custom predicate filters
  *   - Priority-based event handling
  *   - Multiple filters per subscriber
  *
  * To run this example: sbt "examples/runMain
  * keanu.eventbus.filters.FilteredEventBusExample"
  */
object FilteredEventBusExample {

  // Define event types with severity levels
  sealed trait LogLevel
  case object DEBUG   extends LogLevel
  case object INFO    extends LogLevel
  case object WARNING extends LogLevel
  case object ERROR   extends LogLevel

  case class LogEvent(
      level: LogLevel,
      service: String,
      message: String,
      timestamp: Long = System.currentTimeMillis()
  )

  def main(args: Array[String]): Unit = {
    println("=== Keanu EventBus Filtering Example ===\n")

    // Create an event bus for log events
    val logBus = new EventBus[LogEvent] {}

    println("--- Example 1: Topic-based filtering ---")
    // Subscriber that only receives "auth" service logs
    val authLogSubscriber = Subscriber[LogEvent] { msg =>
      println(
        s"[AuthLogger] ${msg.payload.level}: ${msg.payload.message}"
      )
    }

    logBus.subscribe(
      authLogSubscriber,
      filter = msg => msg.topic == "auth"
    )

    // Subscriber that only receives "database" service logs
    val dbLogSubscriber = Subscriber[LogEvent] { msg =>
      println(
        s"[DBLogger] ${msg.payload.level}: ${msg.payload.message}"
      )
    }

    logBus.subscribe(
      dbLogSubscriber,
      filter = msg => msg.topic == "database"
    )

    // Publish logs from different services
    logBus.publish(
      "auth",
      LogEvent(INFO, "auth", "User login successful")
    )
    logBus.publish(
      "database",
      LogEvent(INFO, "database", "Connection established")
    )
    logBus.publish(
      "auth",
      LogEvent(WARNING, "auth", "Failed login attempt")
    )
    logBus.publish(
      "database",
      LogEvent(ERROR, "database", "Query timeout")
    )

    Thread.sleep(100)
    println()

    println("--- Example 2: Severity-based filtering ---")
    // Subscriber that only receives ERROR level logs
    val errorAlertSubscriber = Subscriber[LogEvent] { msg =>
      println(
        s"[ALERT] ${msg.topic.toUpperCase}: ${msg.payload.message}"
      )
    }

    logBus.subscribe(
      errorAlertSubscriber,
      filter = msg => msg.payload.level == ERROR
    )

    // Subscriber that receives WARNING and ERROR logs
    val warningSubscriber = Subscriber[LogEvent] { msg =>
      println(
        s"[Warning Monitor] ${msg.payload.service}: ${msg.payload.message}"
      )
    }

    logBus.subscribe(
      warningSubscriber,
      filter = msg =>
        msg.payload.level == WARNING || msg.payload.level == ERROR
    )

    // Publish logs with different severities
    logBus.publish(
      "api",
      LogEvent(DEBUG, "api", "Request received")
    )
    logBus.publish(
      "api",
      LogEvent(INFO, "api", "Request processed")
    )
    logBus.publish(
      "api",
      LogEvent(WARNING, "api", "Slow response time")
    )
    logBus.publish(
      "api",
      LogEvent(ERROR, "api", "Internal server error")
    )

    Thread.sleep(100)
    println()

    println("--- Example 3: Complex filtering logic ---")
    // Subscriber with complex business logic filter
    val criticalEventsSubscriber = Subscriber[LogEvent] { msg =>
      val event = msg.payload
      println(
        s"[Critical Monitor] ${event.service.toUpperCase()} ${event.level}: ${event.message}"
      )
    }

    // Filter for critical events: ERRORs from production services
    val productionServices = Set("auth", "database", "payment")
    logBus.subscribe(
      criticalEventsSubscriber,
      filter = msg =>
        msg.payload.level == ERROR && productionServices.contains(
          msg.payload.service
        )
    )

    // Publish various events
    logBus.publish(
      "payment",
      LogEvent(ERROR, "payment", "Payment processing failed")
    )
    logBus.publish(
      "test",
      LogEvent(ERROR, "test", "Test suite error")
    ) // Not production
    logBus.publish(
      "auth",
      LogEvent(ERROR, "auth", "Authentication service down")
    )
    logBus.publish(
      "database",
      LogEvent(WARNING, "database", "High connection count")
    ) // Not ERROR

    Thread.sleep(100)
    println()

    println("--- Example 4: Time-based filtering ---")
    val startTime                 = System.currentTimeMillis()
    // Subscriber that only receives recent events (within last 500ms)
    val recentEventsSubscriber    = Subscriber[LogEvent] { msg =>
      val age = System.currentTimeMillis() - msg.payload.timestamp
      println(
        s"[Recent Events] ${msg.payload.service}: ${msg.payload.message} (${age}ms ago)"
      )
    }

    logBus.subscribe(
      recentEventsSubscriber,
      filter = msg =>
        (System.currentTimeMillis() - msg.payload.timestamp) < 500
    )

    // Publish events with different timestamps
    logBus.publish(
      "service1",
      LogEvent(
        INFO,
        "service1",
        "Old event",
        startTime - 1000
      )
    ) // Too old
    Thread.sleep(100)
    logBus.publish(
      "service2",
      LogEvent(INFO, "service2", "Recent event 1")
    ) // Recent
    Thread.sleep(100)
    logBus.publish(
      "service3",
      LogEvent(INFO, "service3", "Recent event 2")
    ) // Recent

    Thread.sleep(100)
    println()

    println("--- Example 5: Count statistics ---")
    // Subscriber that tracks message counts by topic
    val statsSubscriber = new Subscriber[LogEvent] {
      private val counts =
        scala.collection.mutable.Map[String, Int]().withDefaultValue(0)

      override def onMsg(msg: EventBusMessage[LogEvent]): Unit = {
        val topic = if (msg.topic.isEmpty) "default" else msg.topic
        counts(topic) += 1
        println(s"[Stats] $topic: ${counts(topic)} messages")
      }
    }

    logBus.subscribe(statsSubscriber)

    // Publish a burst of events
    for (i <- 1 to 3) {
      logBus.publish("auth", LogEvent(INFO, "auth", s"Event $i"))
      logBus.publish("database", LogEvent(INFO, "database", s"Event $i"))
      logBus.publish("api", LogEvent(INFO, "api", s"Event $i"))
    }

    Thread.sleep(200)
    println()

    // Cleanup
    println("--- Cleanup ---")
    logBus.shutdown()
    println("EventBus shut down")

    println("\n=== Example Complete ===")
  }
}
