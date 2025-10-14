package keanu.actors.hierarchy

import dev.alteration.branch.keanu.actors.*

/** An example showing actor hierarchies and parent-child relationships.
  *
  * This example demonstrates:
  *   - Creating child actors from parent actors
  *   - Using ActorPath for hierarchical addressing
  *   - Actor selection by path
  *   - Getting children of an actor
  *
  * To run this example: sbt "examples/runMain
  * keanu.actors.hierarchy.HierarchyExample"
  */
object HierarchyExample {

  // Messages
  case class CreateChild(name: String)
  case class BroadcastToChildren(message: String)
  case class WorkItem(id: Int, data: String)

  // Parent actor that manages worker children
  case class SupervisorActor() extends Actor {
    def onMsg: PartialFunction[Any, Any] = {
      case CreateChild(name) =>
        println(s"[Supervisor] Creating child: $name")
        // Create a child actor
        val childPath = context.self.path / name
        context.system.actorOf[WorkerActor](childPath)
        println(s"[Supervisor] Child created at: $childPath")

      case BroadcastToChildren(msg) =>
        println(s"[Supervisor] Broadcasting message: $msg")
        val children = context.children
        println(s"[Supervisor] Found ${children.size} children")
        children.foreach { childRef =>
          context.system.tell(childRef, msg)
        }

      case other =>
        println(s"[Supervisor] Received: $other")
    }

    override def preStart(): Unit = {
      println(s"[Supervisor] Starting at path: ${context.self.path}")
    }
  }

  // Worker actor that processes work items
  case class WorkerActor() extends Actor {
    private var processedCount = 0

    def onMsg: PartialFunction[Any, Any] = {
      case WorkItem(id, data) =>
        processedCount += 1
        println(
          s"[Worker ${context.self.name}] Processing item #$id: $data (total: $processedCount)"
        )

      case msg: String =>
        println(s"[Worker ${context.self.name}] Received broadcast: $msg")
    }

    override def preStart(): Unit = {
      println(s"[Worker] Starting at path: ${context.self.path}")
    }

    override def postStop(): Unit = {
      println(
        s"[Worker ${context.self.name}] Stopped. Processed $processedCount items"
      )
    }
  }

  def main(args: Array[String]): Unit = {
    println("=== Keanu Actor Hierarchy Example ===\n")

    val system = ActorSystem()

    // Register actor types
    system.registerProp(ActorProps.props[SupervisorActor](EmptyTuple))
    system.registerProp(ActorProps.props[WorkerActor](EmptyTuple))

    println("--- Example 1: Creating a supervisor ---")
    val supervisorRef = system.actorOf[SupervisorActor]("taskSupervisor")
    Thread.sleep(50)
    println()

    println("--- Example 2: Creating child actors ---")
    system.tell(supervisorRef, CreateChild("worker1"))
    system.tell(supervisorRef, CreateChild("worker2"))
    system.tell(supervisorRef, CreateChild("worker3"))
    Thread.sleep(100)
    println()

    println("--- Example 3: Sending messages to specific children ---")
    // Select specific children by path and send work
    system.actorSelection("/user/taskSupervisor/worker1").foreach { worker1 =>
      system.tell(worker1, WorkItem(1, "Task A"))
      system.tell(worker1, WorkItem(2, "Task B"))
    }

    system.actorSelection("/user/taskSupervisor/worker2").foreach { worker2 =>
      system.tell(worker2, WorkItem(3, "Task C"))
    }
    Thread.sleep(100)
    println()

    println("--- Example 4: Broadcasting to all children ---")
    system.tell(supervisorRef, BroadcastToChildren("Status check"))
    Thread.sleep(100)
    println()

    println("--- Example 5: Listing children ---")
    // Use ActorPath.fromString to get children by full path
    val children = ActorPath.fromString("/user/taskSupervisor") match {
      case Some(path) => system.getChildren(path)
      case None       => List.empty
    }
    println(s"Supervisor has ${children.size} children:")
    children.foreach { child =>
      println(s"  - ${child.path}")
    }
    println()

    println("--- Example 6: More work distribution ---")
    system.tellPath("/user/taskSupervisor/worker1", WorkItem(4, "Task D"))
    system.tellPath("/user/taskSupervisor/worker2", WorkItem(5, "Task E"))
    system.tellPath("/user/taskSupervisor/worker3", WorkItem(6, "Task F"))
    Thread.sleep(100)
    println()

    println("--- Example 7: Creating nested hierarchy ---")
    // Create a child of worker1
    system.actorSelection("/user/taskSupervisor/worker1").foreach { worker1 =>
      val subWorkerPath = worker1.path / "subworker"
      system.actorOf[WorkerActor](subWorkerPath)
      println(s"Created nested worker at: $subWorkerPath")
    }
    Thread.sleep(50)

    // Send message to nested worker
    system.actorSelection("/user/taskSupervisor/worker1/subworker").foreach {
      subWorker =>
        system.tell(subWorker, WorkItem(7, "Nested task"))
    }
    Thread.sleep(100)
    println()

    // Shutdown
    println("--- Shutting down ---")
    system.shutdownAwait(5000)

    println("\n=== Example Complete ===")
  }
}
