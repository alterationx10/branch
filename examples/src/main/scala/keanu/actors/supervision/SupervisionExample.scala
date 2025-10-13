package keanu.actors.supervision

import dev.alteration.branch.keanu.actors.*
import scala.concurrent.duration.*

/** An example showing supervision strategies and actor lifecycle.
  *
  * This example demonstrates:
  *   - RestartStrategy: Actor restarts after failure
  *   - StopStrategy: Actor stops after failure
  *   - RestartWithBackoff: Actor restarts with exponential backoff
  *   - Lifecycle hooks: preStart, postStop, preRestart, postRestart
  *
  * To run this example: sbt "examples/runMain
  * keanu.actors.supervision.SupervisionExample"
  */
object SupervisionExample {

  // Messages
  case class Process(value: Int)
  case class FailOnPurpose()
  case class GetProcessedCount()

  // Actor that restarts after failure (default strategy)
  case class ResilientActor() extends Actor {
    private var processedCount = 0

    def onMsg: PartialFunction[Any, Any] = {
      case Process(value) =>
        processedCount += 1
        println(
          s"[ResilientActor] Processing value: $value (count: $processedCount)"
        )

      case FailOnPurpose() =>
        println("[ResilientActor] Intentionally failing...")
        throw new RuntimeException("Intentional failure")

      case ask: Ask[?] =>
        ask.message match {
          case GetProcessedCount() =>
            ask.complete(processedCount)
        }
    }

    override def supervisorStrategy: SupervisionStrategy = RestartStrategy

    override def preStart(): Unit = {
      println("[ResilientActor] Starting")
    }

    override def postStop(): Unit = {
      println("[ResilientActor] Stopped")
    }

    override def preRestart(reason: Throwable): Unit = {
      println(s"[ResilientActor] Pre-restart due to: ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      println(s"[ResilientActor] Post-restart after: ${reason.getMessage}")
    }
  }

  // Actor that stops after failure
  case class FragileActor() extends Actor {
    def onMsg: PartialFunction[Any, Any] = {
      case Process(value) =>
        println(s"[FragileActor] Processing: $value")

      case FailOnPurpose() =>
        println("[FragileActor] Failing and stopping...")
        throw new RuntimeException("Fatal error")
    }

    override def supervisorStrategy: SupervisionStrategy = StopStrategy

    override def preStart(): Unit = {
      println("[FragileActor] Starting")
    }

    override def postStop(): Unit = {
      println("[FragileActor] Stopped permanently")
    }
  }

  // Actor that restarts with exponential backoff
  case class BackoffActor() extends Actor {
    private var failureCount = 0

    def onMsg: PartialFunction[Any, Any] = {
      case Process(value) =>
        println(s"[BackoffActor] Processing: $value")

      case FailOnPurpose() =>
        failureCount += 1
        println(s"[BackoffActor] Failure #$failureCount")
        throw new RuntimeException(s"Failure #$failureCount")
    }

    override def supervisorStrategy: SupervisionStrategy =
      RestartWithBackoff(
        minBackoff = 100.millis,
        maxBackoff = 2.seconds,
        maxRetries = Some(3),
        resetAfter = Some(5.seconds)
      )

    override def preStart(): Unit = {
      println("[BackoffActor] Starting")
    }

    override def postRestart(reason: Throwable): Unit = {
      println(s"[BackoffActor] Restarted after: ${reason.getMessage}")
    }

    override def postStop(): Unit = {
      println(
        "[BackoffActor] Stopped after exceeding max retries"
      )
    }
  }

  def main(args: Array[String]): Unit = {
    println("=== Keanu Supervision Strategy Example ===\n")

    val system = new ActorSystem {
      override def logger: ActorLogger = ConsoleLogger()
    }

    // Register actor types
    system.registerProp(ActorProps.props[ResilientActor](EmptyTuple))
    system.registerProp(ActorProps.props[FragileActor](EmptyTuple))
    system.registerProp(ActorProps.props[BackoffActor](EmptyTuple))

    println("--- Example 1: RestartStrategy ---")
    println("Actor restarts after failure and continues processing\n")
    system.tell[ResilientActor]("actor1", Process(1))
    Thread.sleep(50)
    system.tell[ResilientActor]("actor1", FailOnPurpose())
    Thread.sleep(100) // Give time for restart
    system.tell[ResilientActor]("actor1", Process(2))
    system.tell[ResilientActor]("actor1", Process(3))
    Thread.sleep(100)
    println()

    println("--- Example 2: StopStrategy ---")
    println("Actor stops permanently after failure\n")
    system.tell[FragileActor]("actor2", Process(1))
    Thread.sleep(50)
    system.tell[FragileActor]("actor2", FailOnPurpose())
    Thread.sleep(100)
    println("Attempting to send message to stopped actor:")
    try {
      // This will create a new actor since the old one stopped
      system.tell[FragileActor]("actor2", Process(2))
      Thread.sleep(50)
    } catch {
      case e: Exception => println(s"Error: ${e.getMessage}")
    }
    println()

    println("--- Example 3: RestartWithBackoff ---")
    println(
      "Actor restarts with increasing delays, stops after max retries\n"
    )
    system.tell[BackoffActor]("actor3", Process(1))
    Thread.sleep(50)

    // Trigger multiple failures
    println("Triggering failures with exponential backoff...")
    for (_ <- 1 to 4) {
      system.tell[BackoffActor]("actor3", FailOnPurpose())
      Thread.sleep(500) // Wait to observe backoff timing
    }
    Thread.sleep(1000)
    println()

    // Shutdown
    println("--- Shutting down ---")
    system.shutdownAwait(5000)

    println("\n=== Example Complete ===")
  }
}
