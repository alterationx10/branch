---
title: Keanu Advanced Features
description: Supervision strategies, mailbox types, dead letters, and lifecycle management
author: Mark Rudolph
published: 2025-01-25T04:36:00Z
lastUpdated: 2025-01-25T04:36:00Z
tags:
  - actor
  - supervision
  - lifecycle
---

# Advanced Features

## Supervision Strategies

Control what happens when an actor fails by overriding `supervisorStrategy`:

```scala
import scala.concurrent.duration.*

// Default: restart on failure
case class SimpleActor() extends Actor {
  override def supervisorStrategy = RestartStrategy
  override def onMsg: PartialFunction[Any, Any] = {
    case msg => processMessage(msg)
  }
}

// Stop on failure (don't restart)
case class CriticalActor() extends Actor {
  override def supervisorStrategy = StopStrategy
  override def onMsg: PartialFunction[Any, Any] = {
    case msg => processMessage(msg)
  }
}

// Restart with exponential backoff
case class ExternalServiceActor() extends Actor {
  override def supervisorStrategy = RestartWithBackoff(
    minBackoff = 1.second,      // Start with 1 second delay
    maxBackoff = 30.seconds,     // Max 30 seconds between retries
    maxRetries = Some(5),        // Stop after 5 failed restarts
    resetAfter = Some(1.minute)  // Reset failure count after 1 minute of success
  )

  override def onMsg: PartialFunction[Any, Any] = {
    case msg => callExternalService(msg)
  }
}
```

Supervision strategies:
- `RestartStrategy` - Recreate the actor and continue (default)
- `StopStrategy` - Remove the actor from the system
- `RestartWithBackoff` - Restart with exponential backoff and optional retry limits

## Dead Letters

Messages that cannot be delivered are recorded in the dead letter queue:

```scala
// Messages end up in dead letters when:
// 1. Actor's onMsg doesn't handle the message type
case class PickyActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case s: String => println(s)
    // Any other message type will be dead-lettered
  }
}

system.registerProp(ActorProps.props[PickyActor]())
system.tell[PickyActor]("picky", "Hello")  // OK
system.tell[PickyActor]("picky", 42)       // Dead letter (Int not handled)

// Check dead letters for debugging
val deadLetters = system.getDeadLetters(limit = 100)
deadLetters.foreach { dl =>
  println(s"Dead letter: ${dl.message} -> ${dl.recipient.path} (${dl.reason})")
}
```

## Mailbox Types

Configure how actors queue and process messages:

```scala
// Unbounded mailbox (default) - FIFO, no capacity limit
ActorProps.props[MyActor]()
  .withMailbox(UnboundedMailbox)

// Bounded mailbox - limit capacity with overflow strategy
ActorProps.props[MyActor]()
  .withMailbox(BoundedMailbox(
    capacity = 1000,
    overflowStrategy = DropOldest  // or DropNewest, Fail
  ))

// Priority mailbox - process high-priority messages first
case class PriorityMessage(priority: Int, data: String)

val priorityOrdering = Ordering.by[Any, Int] {
  case PriorityMessage(p, _) => -p  // Negative for highest-first
  case _                     => 0
}

ActorProps.props[MyActor]()
  .withMailbox(PriorityMailbox(priorityOrdering))
```

## Actor Selection and Creation

Create and find actors using paths:

```scala
// Create top-level actors
val supervisor = system.actorOf[SupervisorActor]("supervisor")
println(supervisor.path)  // /user/supervisor

// Create child actors within a parent
case class SupervisorActor() extends Actor {
  override def preStart(): Unit = {
    // Create children using actorOf
    val worker1 = context.actorOf[WorkerActor]("worker1")
    val worker2 = context.actorOf[WorkerActor]("worker2")
    println(worker1.path)  // /user/supervisor/worker1
  }

  override def onMsg: PartialFunction[Any, Any] = {
    case msg => context.children.foreach(child => context.system.tell(child, msg))
  }
}

// Find existing actors
val workerRef = system.actorSelection("/user/supervisor/worker1")
workerRef.foreach(ref => system.tell(ref, "work"))

// Send to actor by path
system.tellPath("/user/supervisor/worker1", "work")
```

## Lifecycle Management

The ActorSystem provides several lifecycle events and supervision features:

- Actors automatically restart on exceptions (based on supervision strategy)
- PoisonPill message for graceful termination
- Shutdown coordination with timeout
- Lifecycle hooks for initialization and cleanup
- Dead letter queue for undeliverable messages

### Shutting Down

To shut down the ActorSystem:

```scala
// Shutdown with 30 second timeout (default)
val success = system.shutdownAwait()

// Shutdown with custom timeout
val success = system.shutdownAwait(timeoutMillis = 60000)

if (!success) {
  println("Warning: Some actors did not terminate in time")
}
```

This will send PoisonPill to all actors and wait for them to terminate.

## Preserving State Across Restarts

By default, state resets after an actor restart. To preserve state:

```scala
case class PersistentCounterActor() extends StatefulActor[CounterState, CounterMessage] {
  private var savedState: Option[CounterState] = None

  override def preRestart(reason: Throwable): Unit = {
    // Save state before restart
    savedState = Some(state)
  }

  override def initialState: CounterState =
    // Restore saved state or use default
    savedState.getOrElse(CounterState(0, List.empty))

  override def statefulOnMsg: PartialFunction[CounterMessage, CounterState] = {
    case Increment => state.copy(count = state.count + 1)
    case Decrement => state.copy(count = state.count - 1)
  }
}
```

## Complex State Machine Example

StatefulActor excels at managing complex state machines:

```scala
sealed trait SessionState
case object Unauthenticated extends SessionState
case class Authenticated(userId: String, permissions: Set[String]) extends SessionState
case class Locked(attempts: Int, lockUntil: Long) extends SessionState

case class SessionActorState(
  status: SessionState,
  loginAttempts: Int,
  lastActivity: Long
)

sealed trait SessionMessage
case class Login(userId: String, password: String) extends SessionMessage
case object Logout extends SessionMessage
case class CheckPermission(permission: String) extends SessionMessage
case object RefreshActivity extends SessionMessage

case class SessionActor(authService: AuthService)
  extends StatefulActor[SessionActorState, SessionMessage] {

  override def initialState: SessionActorState =
    SessionActorState(Unauthenticated, 0, System.currentTimeMillis())

  override def statefulOnMsg: PartialFunction[SessionMessage, SessionActorState] = {
    case Login(userId, password) =>
      state.status match {
        case Locked(_, lockUntil) if System.currentTimeMillis() < lockUntil =>
          // Still locked
          state

        case _ =>
          if (authService.authenticate(userId, password)) {
            val permissions = authService.getPermissions(userId)
            state.copy(
              status = Authenticated(userId, permissions),
              loginAttempts = 0,
              lastActivity = System.currentTimeMillis()
            )
          } else {
            val newAttempts = state.loginAttempts + 1
            if (newAttempts >= 3) {
              state.copy(
                status = Locked(newAttempts, System.currentTimeMillis() + 300000),
                loginAttempts = newAttempts
              )
            } else {
              state.copy(loginAttempts = newAttempts)
            }
          }
      }

    case Logout =>
      state.copy(status = Unauthenticated, loginAttempts = 0)

    case CheckPermission(perm) =>
      state.status match {
        case Authenticated(_, permissions) if permissions.contains(perm) =>
          state.copy(lastActivity = System.currentTimeMillis())
        case _ =>
          state
      }

    case RefreshActivity =>
      state.copy(lastActivity = System.currentTimeMillis())
  }
}
```

## Key Features Summary

### EventBus
- Virtual threads per subscriber
- Message delivery guarantees within a subscriber
- Graceful shutdown with AutoCloseable
- Filtered subscriptions

### ActorSystem
- Actors are uniquely identified by hierarchical paths
- Parent-child relationships with supervision
- Automatic actor restart on failures
- Message delivery guarantees within a single actor
- Graceful shutdown with PoisonPill messages
- Type-safe actor props system for constructor arguments
- Request-response messaging with the ask pattern

## Next Steps

- Explore the [EventBus](/keanu/eventbus) for simpler pub/sub patterns
- Learn about [ActorSystem basics](/keanu/actors)
- Return to [Keanu Overview](/keanu)
