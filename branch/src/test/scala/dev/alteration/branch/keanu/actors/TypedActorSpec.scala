package dev.alteration.branch.keanu.actors

import munit.FunSuite

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration._

class TypedActorSpec extends FunSuite {

  // Test message types
  sealed trait CounterMessage
  case object Increment                      extends CounterMessage
  case object Decrement                      extends CounterMessage
  case class AddValue(value: Int)            extends CounterMessage
  case class GetValue(latch: CountDownLatch) extends CounterMessage

  test("TypedActor should handle typed messages") {
    @volatile var counter = 0
    val latch             = new CountDownLatch(1)

    case class TestActor() extends TypedActor[CounterMessage] {
      override def typedOnMsg: PartialFunction[CounterMessage, Any] = {
        case Increment       =>
          counter += 1
        case Decrement       =>
          counter -= 1
        case AddValue(value) =>
          counter += value
        case GetValue(l)     =>
          l.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    as.tell[TestActor]("test", Increment)
    as.tell[TestActor]("test", Increment)
    as.tell[TestActor]("test", Decrement)
    as.tell[TestActor]("test", AddValue(5))
    as.tell[TestActor]("test", GetValue(latch))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(counter, 6, "Counter should be 6 (0 + 1 + 1 - 1 + 5)")
  }

  test("TypedActor should reject non-matching message types") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[String]()

    sealed trait MessageA
    case class ValidMessage(value: String) extends MessageA

    case class TestActor() extends TypedActor[MessageA] {
      override def typedOnMsg: PartialFunction[MessageA, Any] = {
        case ValidMessage(v) =>
          receivedMessages.synchronized {
            receivedMessages += v
          }
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    as.tell[TestActor]("test", ValidMessage("hello"))
    as.tell[TestActor]("test", "raw string") // Should go to dead letters
    Thread.sleep(20)

    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    assertEquals(messages, List("hello"), "Should only process typed messages")

    val deadLetters = as.getDeadLetters(10)
    assert(
      deadLetters.exists(_.message == "raw string"),
      "Untyped message should be in dead letters"
    )
  }

  test("TypedActor should work with sealed trait hierarchies") {
    @volatile var result = ""
    val latch            = new CountDownLatch(3)

    sealed trait Command
    case class DoA(value: String) extends Command
    case class DoB(value: Int)    extends Command
    case class DoC(flag: Boolean) extends Command

    case class TestActor() extends TypedActor[Command] {
      override def typedOnMsg: PartialFunction[Command, Any] = {
        case DoA(v) =>
          result += s"A:$v,"
          latch.countDown()
        case DoB(v) =>
          result += s"B:$v,"
          latch.countDown()
        case DoC(v) =>
          result += s"C:$v,"
          latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    as.tell[TestActor]("test", DoA("hello"))
    as.tell[TestActor]("test", DoB(42))
    as.tell[TestActor]("test", DoC(true))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(
      result,
      "A:hello,B:42,C:true,",
      "Should handle all message types in hierarchy"
    )
  }

  test("TypedActor should support partial message handling") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[String]()

    sealed trait Message
    case class Handled(value: String)   extends Message
    case class Unhandled(value: String) extends Message

    case class SelectiveActor() extends TypedActor[Message] {
      override def typedOnMsg: PartialFunction[Message, Any] = {
        case Handled(v) =>
          receivedMessages.synchronized {
            receivedMessages += v
          }
        // Unhandled intentionally not matched
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SelectiveActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SelectiveActor]("test", Handled("processed"))
    as.tell[SelectiveActor]("test", Unhandled("ignored"))
    Thread.sleep(20)

    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    assertEquals(
      messages,
      List("processed"),
      "Should only handle matched cases"
    )

    val deadLetters = as.getDeadLetters(10)
    assert(
      deadLetters.exists(dl =>
        dl.message.asInstanceOf[Unhandled].value == "ignored"
      ),
      "Unhandled message should be in dead letters"
    )
  }

  test("TypedActor should work with ActorContext") {
    @volatile var selfPath: Option[String] = None
    val latch                              = new CountDownLatch(1)

    sealed trait TestMessage
    case object GetSelf extends TestMessage

    case class ContextActor() extends TypedActor[TestMessage] {
      override def typedOnMsg: PartialFunction[TestMessage, Any] = {
        case GetSelf =>
          selfPath = Some(context.self.path.toString)
          latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[ContextActor](EmptyTuple)
    as.registerProp(props)

    as.actorOf[ContextActor]("myactor")
    as.tellPath("/user/myactor", GetSelf)

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(selfPath.isDefined, "Should have captured self path")
    assertEquals(selfPath.get, "/user/myactor")
  }

  test("TypedActor should work with lifecycle hooks") {
    @volatile var startCalled   = false
    @volatile var stopCalled    = false
    @volatile var restartCalled = false
    val latch                   = new CountDownLatch(1)

    sealed trait TestMessage
    case object Fail extends TestMessage
    case object Done extends TestMessage

    case class LifecycleActor() extends TypedActor[TestMessage] {
      override def preStart(): Unit                              = startCalled = true
      override def postStop(): Unit                              = stopCalled = true
      override def postRestart(reason: Throwable): Unit          = restartCalled = true
      override def typedOnMsg: PartialFunction[TestMessage, Any] = {
        case Fail => throw new RuntimeException("Intentional failure")
        case Done => latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[LifecycleActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LifecycleActor]("test", Fail)
    Thread.sleep(30) // Wait for restart
    as.tell[LifecycleActor]("test", Done)
    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(startCalled, "preStart should be called")
    assert(restartCalled, "postRestart should be called after failure")
    assert(stopCalled, "postStop should be called on shutdown")
  }

  test("TypedActor should work with supervision strategies") {
    @volatile var restartCount = 0
    val latch                  = new CountDownLatch(1)

    sealed trait TestMessage
    case object Fail extends TestMessage
    case object Done extends TestMessage

    case class SupervisedActor() extends TypedActor[TestMessage] {
      restartCount += 1
      override def supervisorStrategy: SupervisionStrategy       = RestartStrategy
      override def typedOnMsg: PartialFunction[TestMessage, Any] = {
        case Fail => throw new RuntimeException("Fail")
        case Done => latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SupervisedActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SupervisedActor]("test", Fail)
    Thread.sleep(30)
    as.tell[SupervisedActor]("test", Done)
    latch.await(2, SECONDS)
    as.shutdownAwait()

    assert(restartCount >= 2, "Actor should have restarted after failure")
  }

  sealed trait QueryMessage
  case class Query(value: String) extends QueryMessage
  test("TypedActor should work with Ask pattern") {
    import scala.concurrent.Await

    case class QueryActor() extends TypedActor[Any] {
      override def typedOnMsg: PartialFunction[Any, Any] = { case ask: Ask[?] =>
        ask.message match {
          case Query(v) => ask.complete(s"Response: $v")
          case _        => ()
        }
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[QueryActor](EmptyTuple)
    as.registerProp(props)

    val future = as.ask[QueryActor, String]("test", Query("hello"), 2.seconds)
    val result = Await.result(future, 3.seconds)

    as.shutdownAwait()

    assertEquals(result, "Response: hello")
  }

  sealed trait PriorityMessage
  case class Priority(value: Int) extends PriorityMessage
  case object Start               extends PriorityMessage
  case object Done                extends PriorityMessage
  test("TypedActor should work with mailbox types") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[Int]()
    val startLatch       = new CountDownLatch(1)
    val latch            = new CountDownLatch(3)

    case class PriorityActor() extends TypedActor[PriorityMessage] {
      override def typedOnMsg: PartialFunction[PriorityMessage, Any] = {
        case Start       =>
          // Block to let all priority messages queue up
          startLatch.countDown()
          Thread.sleep(50)
        case Priority(v) =>
          receivedMessages.synchronized {
            receivedMessages += v
          }
          latch.countDown()
        case Done        =>
          latch.countDown()
      }
    }

    given Ordering[Any] = new Ordering[Any] {
      def compare(x: Any, y: Any): Int = (x, y) match {
        case (Priority(a), Priority(b)) =>
          b.compare(a) // Reverse order (higher is higher priority)
        case (Start, _)                 => -1 // Process Start first
        case (_, Start)                 => 1
        case (Done, _)                  => 1  // Process Done last
        case (_, Done)                  => -1
        case _                          => 0
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[PriorityActor](
      EmptyTuple,
      PriorityMailbox(summon[Ordering[Any]])
    )
    as.registerProp(props)

    // Send Start first to block the actor
    as.tell[PriorityActor]("test", Start)
    startLatch.await(2, SECONDS) // Wait for actor to start blocking

    // Now send priority messages while actor is blocked
    as.tell[PriorityActor]("test", Priority(1))
    as.tell[PriorityActor]("test", Priority(5))
    as.tell[PriorityActor]("test", Priority(3))
    as.tell[PriorityActor]("test", Done)

    // Messages should now be queued and reordered by priority

    latch.await(3, SECONDS)
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    // With priority ordering, highest value (5) should be processed first
    assertEquals(
      messages.headOption,
      Some(5),
      "Should process highest priority first"
    )
    assertEquals(messages, List(5, 3, 1), "Should process in priority order")
  }

  test("TypedActor should handle generic message types") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[String]()
    val latch            = new CountDownLatch(2)

    case class GenericActor() extends TypedActor[String] {
      override def typedOnMsg: PartialFunction[String, Any] = {
        case msg: String =>
          receivedMessages.synchronized {
            receivedMessages += msg
          }
          latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[GenericActor](EmptyTuple)
    as.registerProp(props)

    as.tell[GenericActor]("test", "hello")
    as.tell[GenericActor]("test", "world")

    latch.await(2, SECONDS)
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    assertEquals(messages, List("hello", "world"))
  }
}
