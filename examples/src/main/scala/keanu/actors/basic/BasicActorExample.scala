package keanu.actors.basic

import dev.alteration.branch.keanu.actors.*
import dev.alteration.branch.macaroni.runtimes.BranchExecutors

import scala.concurrent.ExecutionContext

/** A basic example showing how to create actors and send messages.
  *
  * Keanu is an actor system library that provides:
  *   - Message passing with tell/ask patterns
  *   - Supervision strategies for fault tolerance
  *   - Actor hierarchies for organizing complex systems
  *
  * This example demonstrates:
  *   - Creating a simple actor
  *   - Sending messages with tell
  *   - Requesting responses with ask
  *   - Graceful shutdown
  *
  * To run this example: sbt "examples/runMain keanu.actors.basic.BasicActorExample"
  */
object BasicActorExample {

  // Define messages
  case class Greet(name: String)
  case class Add(a: Int, b: Int)
  case class GetCount()

  // A simple counter actor that handles greetings and arithmetic
  case class CounterActor() extends Actor {
    private var count = 0

    def onMsg: PartialFunction[Any, Any] = {
      case Greet(name) =>
        count += 1
        println(s"Hello, $name! You are visitor #$count")

      case Add(a, b) =>
        count += 1
        val result = a + b
        println(s"Computing: $a + $b = $result")
        result

      case ask: Ask[?] =>
        ask.message match {
          case GetCount() =>
            println(s"Count requested: $count")
            ask.complete(count)
          case Add(a, b)  =>
            count += 1
            ask.complete(a + b)
        }
    }

    override def preStart(): Unit = {
      println("CounterActor starting...")
    }

    override def postStop(): Unit = {
      println("CounterActor stopped. Final count: " + count) // This returns 0, need to investigate
    }
  }

  def main(args: Array[String]): Unit = {
    // This is a VirtualThread executor, but could be scala.concurrent.ExecutionContext.global
    given ExecutionContext = BranchExecutors.executionContext
    
    println("=== Keanu Basic Actor Example ===\n")

    // Create an actor system with console logging
    val system = new ActorSystem {
      override def logger: ActorLogger = ConsoleLogger()
    }

    // Register the actor type
    system.registerProp(ActorProps.props[CounterActor](EmptyTuple))

    println("--- Example 1: Sending messages with tell ---")
    // Create and send messages to the actor
    system.tell[CounterActor]("myCounter", Greet("Alice"))
    system.tell[CounterActor]("myCounter", Greet("Bob"))
    system.tell[CounterActor]("myCounter", Add(5, 3))

    // Give actors time to process
    Thread.sleep(100)
    println()

    println("--- Example 2: Requesting responses with ask ---")
    // Ask pattern for request-response
    import scala.concurrent.duration.*
    import scala.util.{Failure, Success}

    val countFuture = system.ask[CounterActor, Int]("myCounter", GetCount())
    countFuture.onComplete {
      case Success(count) => println(s"Current count via ask: $count")
      case Failure(e)     => println(s"Ask failed: ${e.getMessage}")
    }

    // Wait for the response
    Thread.sleep(100)
    println()

    println("--- Example 3: Ask with computation ---")
    val sumFuture =
      system.ask[CounterActor, Int]("myCounter", Add(10, 20), 2.seconds)
    sumFuture.onComplete {
      case Success(sum) => println(s"Sum via ask: $sum")
      case Failure(e)   => println(s"Ask failed: ${e.getMessage}")
    }

    // Wait for the response
    Thread.sleep(100)
    println()

    println("--- Example 4: Multiple actors ---")
    system.tell[CounterActor]("counter1", Greet("Charlie"))
    system.tell[CounterActor]("counter2", Greet("Diana"))
    system.tell[CounterActor]("counter1", Add(1, 1))
    system.tell[CounterActor]("counter2", Add(2, 2))

    // Give actors time to process
    Thread.sleep(100)
    println()

    // Shutdown the system
    println("--- Shutting down ---")
    val shutdownSuccess = system.shutdownAwait(5000)
    if (shutdownSuccess) {
      println("All actors terminated successfully")
    } else {
      println("Shutdown timed out")
    }

    println("\n=== Example Complete ===")
  }
}
