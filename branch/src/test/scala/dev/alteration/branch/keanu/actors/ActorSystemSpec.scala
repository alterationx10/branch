package dev.alteration.branch.keanu.actors

import munit.FunSuite

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration._

class ActorSystemSpec extends FunSuite {

  test("Actor can be restarted") {
    @volatile var counter = 0
    val latch             = new CountDownLatch(3)
    case class BoomActor(latch: CountDownLatch) extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "countDown" =>
          counter += 1
          latch.countDown()
        case _           =>
          counter += 1
          val kaboom = 1 / 0
          counter += 1 // This should not be reached because of the kaboom
          kaboom
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[BoomActor](Tuple1(latch))
    as.registerProp(props)
    as.tell[BoomActor]("boom", "countDown")
    as.tell[BoomActor]("boom", "countDown")
    as.tell[BoomActor]("boom", 42)
    as.tell[BoomActor]("boom", "countDown")
    latch.await(5, SECONDS) // Add timeout to prevent test hanging
    as.shutdownAwait()
    assertEquals(counter, 4)
  }

  test("Actor can process messages in order") {
    val messages = scala.collection.mutable.ArrayBuffer[String]()

    case class OrderedActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case msg: String =>
        messages += msg
        msg
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[OrderedActor](EmptyTuple)
    as.registerProp(props)

    as.tell[OrderedActor]("test", "msg1")
    as.tell[OrderedActor]("test", "msg2")
    as.tell[OrderedActor]("test", "msg3")

    // Give some time for messages to be processed
    Thread.sleep(100)
    as.shutdownAwait()

    assertEquals(messages.toList, List("msg1", "msg2", "msg3"))
  }

  test("ActorSystem shutdown prevents new messages") {
    var messageReceived = false

    case class ShutdownActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ =>
        messageReceived = true
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[ShutdownActor](EmptyTuple)
    as.registerProp(props)

    as.shutdownAwait()

    intercept[IllegalStateException] {
      as.tell[ShutdownActor]("test", "msg")
    }

    assertEquals(messageReceived, false)
  }

  test("Multiple actors can run concurrently") {
    val latch = new CountDownLatch(2)

    case class ConcurrentActor(id: Int, latch: CountDownLatch) extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case "start" =>
        Thread.sleep(100) // Simulate some work
        latch.countDown()
        id
      }
    }

    val as     = ActorSystem()
    val props1 = ActorProps.props[ConcurrentActor]((1, latch))
    val props2 = ActorProps.props[ConcurrentActor]((2, latch))
    as.registerProp(props1)
    as.registerProp(props2)

    as.tell[ConcurrentActor]("actor1", "start")
    as.tell[ConcurrentActor]("actor2", "start")

    assert(latch.await(1, SECONDS), "Actors should complete within timeout")
    as.shutdownAwait()
  }

  // Dead Letter Queue Tests

  test("Unhandled messages should be recorded in dead letter queue") {
    case class SelectiveActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "handled" => "ok"
        // "unhandled" message not defined
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SelectiveActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SelectiveActor]("test", "handled")
    as.tell[SelectiveActor]("test", "unhandled")
    as.tell[SelectiveActor]("test", 42)

    Thread.sleep(100) // Give time for messages to be processed

    val deadLetters = as.getDeadLetters(10)
    assertEquals(deadLetters.size, 2, "Should have 2 unhandled messages")

    val messages = deadLetters.map(_.message)
    assert(messages.contains("unhandled"), "Should contain 'unhandled' message")
    assert(messages.contains(42), "Should contain '42' message")

    deadLetters.foreach { dl =>
      assertEquals(dl.reason, UnhandledMessage, "Reason should be UnhandledMessage")
    }

    as.shutdownAwait()
  }

  test("getDeadLetters should respect limit parameter") {
    case class NoOpActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = PartialFunction.empty
    }

    val as    = ActorSystem()
    val props = ActorProps.props[NoOpActor](EmptyTuple)
    as.registerProp(props)

    // Send 10 unhandled messages
    (1 to 10).foreach(i => as.tell[NoOpActor]("test", s"msg$i"))
    Thread.sleep(100)

    val limited = as.getDeadLetters(5)
    assertEquals(limited.size, 5, "Should respect limit of 5")

    val all = as.getDeadLetters(100)
    assertEquals(all.size, 10, "Should return all 10 messages")

    as.shutdownAwait()
  }

  test("getDeadLetters should reject negative limit") {
    val as = ActorSystem()
    intercept[IllegalArgumentException] {
      as.getDeadLetters(-1)
    }
    as.shutdownAwait()
  }

  test("getDeadLetters should reject zero limit") {
    val as = ActorSystem()
    intercept[IllegalArgumentException] {
      as.getDeadLetters(0)
    }
    as.shutdownAwait()
  }

  // Phase 1: Input Validation Tests

  test("tell should reject null actor name") {
    case class TestActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    intercept[IllegalArgumentException] {
      as.tell[TestActor](null, "message")
    }

    as.shutdownAwait()
  }

  test("tell should reject empty actor name") {
    case class TestActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    intercept[IllegalArgumentException] {
      as.tell[TestActor]("", "message")
    }

    as.shutdownAwait()
  }

  test("tell should reject null message") {
    case class TestActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    intercept[IllegalArgumentException] {
      as.tell[TestActor]("test", null)
    }

    as.shutdownAwait()
  }

  test("registerProp should reject null prop") {
    val as = ActorSystem()

    intercept[IllegalArgumentException] {
      as.registerProp(null)
    }

    as.shutdownAwait()
  }

  // Phase 1: InstantiationException Tests

  test("Actor instantiation failure should terminate actor gracefully") {
    case class FailingActor(shouldFail: Boolean) extends Actor {
      if (shouldFail) throw new RuntimeException("Instantiation failed")
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[FailingActor](Tuple1(true))
    as.registerProp(props)

    // This should not crash the system
    as.tell[FailingActor]("failing", "message")

    Thread.sleep(100) // Give time for actor to fail

    // System should still be operational
    assert(!as.isShutdown, "System should still be running")

    as.shutdownAwait()
  }

  // Phase 1: Shutdown Timeout Tests

  test("shutdownAwait should timeout if actors don't terminate") {
    case class SlowActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "slow" =>
          Thread.sleep(5000) // Sleep longer than timeout
          ()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[SlowActor](EmptyTuple)
    as.registerProp(props)

    as.tell[SlowActor]("slow", "slow")
    Thread.sleep(50) // Give time for message to be taken

    val result = as.shutdownAwait(500) // 500ms timeout
    assertEquals(result, false, "Shutdown should timeout and return false")
  }

  test("shutdownAwait should return true when all actors terminate") {
    case class FastActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[FastActor](EmptyTuple)
    as.registerProp(props)

    as.tell[FastActor]("fast", "message")
    Thread.sleep(50)

    val result = as.shutdownAwait(1000)
    assertEquals(result, true, "Shutdown should complete successfully")
  }

  test("shutdownAwait should be idempotent") {
    val as = ActorSystem()

    val result1 = as.shutdownAwait(1000)
    assertEquals(result1, true, "First shutdown should succeed")

    val result2 = as.shutdownAwait(1000)
    assertEquals(result2, true, "Second shutdown should also return true")
  }
}
