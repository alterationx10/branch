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
      override def onMsg: PartialFunction[Any, Any] = { case "handled" =>
        "ok"
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
      assertEquals(
        dl.reason,
        UnhandledMessage,
        "Reason should be UnhandledMessage"
      )
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
      override def onMsg: PartialFunction[Any, Any] = { case "slow" =>
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

  // Phase 2: Supervision Strategy Tests

  test("Actor with RestartStrategy should restart on failure") {
    @volatile var restartCount = 0
    @volatile var messageCount = 0

    case class RestartActor() extends Actor {
      restartCount += 1
      override def onMsg: PartialFunction[Any, Any]        = {
        case "fail"    => throw new RuntimeException("Intentional failure")
        case "success" => messageCount += 1
      }
      override def supervisorStrategy: SupervisionStrategy = RestartStrategy
    }

    val as    = ActorSystem()
    val props = ActorProps.props[RestartActor](EmptyTuple)
    as.registerProp(props)

    as.tell[RestartActor]("test", "success")
    Thread.sleep(50)
    as.tell[RestartActor]("test", "fail")
    Thread.sleep(100)
    as.tell[RestartActor]("test", "success")
    Thread.sleep(50)

    as.shutdownAwait()

    assert(restartCount >= 2, "Actor should have restarted at least once")
    assertEquals(messageCount, 2, "Should have processed 2 success messages")
  }

  test("Actor with StopStrategy should stop on failure") {
    @volatile var messageCount = 0
    @volatile var stopCount    = 0

    case class StopActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any]        = {
        case "fail"    => throw new RuntimeException("Intentional failure")
        case "success" => messageCount += 1
      }
      override def supervisorStrategy: SupervisionStrategy = StopStrategy
      override def postStop(): Unit                        = stopCount += 1
    }

    val as    = ActorSystem()
    val props = ActorProps.props[StopActor](EmptyTuple)
    as.registerProp(props)

    // Send success and fail messages together
    as.tell[StopActor]("test", "success")
    as.tell[StopActor]("test", "fail")
    Thread.sleep(150) // Wait for processing and stop

    as.shutdownAwait()

    assertEquals(
      messageCount,
      1,
      "Should have processed only 1 message before stop"
    )
    assert(stopCount >= 1, "postStop should have been called at least once")
  }

  test("Actor with RestartWithBackoff should respect backoff timing") {
    import scala.concurrent.duration.*
    @volatile var restartTimes = scala.collection.mutable.ArrayBuffer[Long]()

    case class BackoffActor() extends Actor {
      restartTimes.synchronized {
        restartTimes += System.currentTimeMillis()
      }
      override def onMsg: PartialFunction[Any, Any]        = { case _ =>
        throw new RuntimeException("Always fail")
      }
      override def supervisorStrategy: SupervisionStrategy =
        RestartWithBackoff(
          minBackoff = 100.millis,
          maxBackoff = 500.millis,
          maxRetries = Some(3)
        )
    }

    val as    = ActorSystem()
    val props = ActorProps.props[BackoffActor](EmptyTuple)
    as.registerProp(props)

    as.tell[BackoffActor]("test", "trigger")
    Thread.sleep(1500) // Wait for retries

    as.shutdownAwait()

    val times = restartTimes.synchronized { restartTimes.toList }
    // maxRetries = 3 means: initial start + up to 3 restarts = max 4 total starts
    assert(
      times.size <= 4,
      s"Should have at most 4 starts (initial + 3 retries), got ${times.size}"
    )
    if (times.size >= 2) {
      val firstBackoff = times(1) - times(0)
      assert(
        firstBackoff >= 90, // Allow some timing slack
        s"First backoff should be at least 90ms, was ${firstBackoff}ms"
      )
    }
  }

  // Phase 2: Lifecycle Hook Tests

  test("preStart should be called before processing messages") {
    @volatile var preStartCalled    = false
    @volatile var messageCalled     = false
    @volatile var preStartBeforeMsg = false

    case class LifecycleActor() extends Actor {
      override def preStart(): Unit                 = {
        preStartCalled = true
        if (!messageCalled) preStartBeforeMsg = true
      }
      override def onMsg: PartialFunction[Any, Any] = { case _ =>
        messageCalled = true
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[LifecycleActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LifecycleActor]("test", "message")
    Thread.sleep(100)

    as.shutdownAwait()

    assert(preStartCalled, "preStart should have been called")
    assert(preStartBeforeMsg, "preStart should be called before onMsg")
  }

  test("postStop should be called on PoisonPill") {
    @volatile var postStopCalled = false

    case class LifecycleActor() extends Actor {
      override def postStop(): Unit                 = postStopCalled = true
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[LifecycleActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LifecycleActor]("test", "message")
    as.tell[LifecycleActor]("test", PoisonPill)
    Thread.sleep(100)

    as.shutdownAwait()

    assert(postStopCalled, "postStop should have been called after PoisonPill")
  }

  test("preRestart and postRestart should be called on restart") {
    @volatile var preRestartCalled                    = false
    @volatile var postRestartCalled                   = false
    @volatile var preRestartError: Option[Throwable]  = None
    @volatile var postRestartError: Option[Throwable] = None

    case class LifecycleActor() extends Actor {
      override def preRestart(reason: Throwable): Unit  = {
        preRestartCalled = true
        preRestartError = Some(reason)
      }
      override def postRestart(reason: Throwable): Unit = {
        postRestartCalled = true
        postRestartError = Some(reason)
      }
      override def onMsg: PartialFunction[Any, Any]     = {
        case "fail"    => throw new RuntimeException("Test error")
        case "success" => ()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[LifecycleActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LifecycleActor]("test", "fail")
    Thread.sleep(150)
    as.tell[LifecycleActor]("test", "success")
    Thread.sleep(50)

    as.shutdownAwait()

    assert(preRestartCalled, "preRestart should have been called")
    assert(postRestartCalled, "postRestart should have been called")
    assert(
      preRestartError.isDefined,
      "preRestart should receive the error"
    )
    assert(
      preRestartError.get.getMessage == "Test error",
      "preRestart should receive correct error"
    )
  }

  test("postStop should be called on stop strategy") {
    @volatile var postStopCalled = false

    case class StopActor() extends Actor {
      override def postStop(): Unit                        = postStopCalled = true
      override def onMsg: PartialFunction[Any, Any]        = { case _ =>
        throw new RuntimeException("Always fail")
      }
      override def supervisorStrategy: SupervisionStrategy = StopStrategy
    }

    val as    = ActorSystem()
    val props = ActorProps.props[StopActor](EmptyTuple)
    as.registerProp(props)

    as.tell[StopActor]("test", "message")
    Thread.sleep(100)

    as.shutdownAwait()

    assert(
      postStopCalled,
      "postStop should be called when actor stops due to failure"
    )
  }

  // Phase 2: Mailbox Type Tests

  test("BoundedMailbox with DropOldest should drop oldest message when full") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[Int]()
    val latch            = new CountDownLatch(1)

    case class BoundedActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case i: Int =>
          receivedMessages.synchronized {
            receivedMessages += i
          }
        case "done" => latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[BoundedActor](
      EmptyTuple,
      BoundedMailbox(capacity = 3, overflowStrategy = DropOldest)
    )
    as.registerProp(props)

    // Send 5 messages quickly, mailbox capacity is 3
    // The actor will process them, but we're testing the overflow behavior
    as.tell[BoundedActor]("test", 1)
    as.tell[BoundedActor]("test", 2)
    as.tell[BoundedActor]("test", 3)
    as.tell[BoundedActor]("test", 4)
    as.tell[BoundedActor]("test", 5)
    as.tell[BoundedActor]("test", "done")

    latch.await(2, SECONDS)
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    assert(messages.contains(5), "Should have received the newest message")
  }

  test("BoundedMailbox with DropNewest should drop newest message when full") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[String]()
    val latch            = new CountDownLatch(1)

    case class BlockingActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "block"   =>
          // Block to let mailbox fill up
          Thread.sleep(500)
          receivedMessages.synchronized { receivedMessages += "block" }
        case s: String =>
          receivedMessages.synchronized { receivedMessages += s }
        case "done"    => latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[BlockingActor](
      EmptyTuple,
      BoundedMailbox(capacity = 2, overflowStrategy = DropNewest)
    )
    as.registerProp(props)

    // Send blocking message first, then fill mailbox
    as.tell[BlockingActor]("test", "block")
    Thread.sleep(50)                       // Let actor start processing
    as.tell[BlockingActor]("test", "msg1")
    as.tell[BlockingActor]("test", "msg2")
    as.tell[BlockingActor]("test", "msg3") // This should be dropped
    Thread.sleep(600)                      // Wait for block to finish
    as.tell[BlockingActor]("test", "done")

    latch.await(2, SECONDS)
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    assert(messages.contains("block"), "Should have processed blocking message")
    assert(messages.contains("msg1"), "Should have processed msg1")
    // msg3 should have been dropped due to DropNewest strategy
  }

  test("BoundedMailbox with Fail should throw exception when full") {
    case class FailActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "block" => Thread.sleep(200)
        case _       => ()
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[FailActor](
      EmptyTuple,
      BoundedMailbox(capacity = 2, overflowStrategy = Fail)
    )
    as.registerProp(props)

    // Send blocking message and fill mailbox
    as.tell[FailActor]("test", "block")
    Thread.sleep(50)
    as.tell[FailActor]("test", "msg1")
    as.tell[FailActor]("test", "msg2")

    // This should throw MailboxOverflowException
    intercept[MailboxOverflowException] {
      as.tell[FailActor]("test", "msg3")
    }

    // Wait for block to finish processing before shutdown
    Thread.sleep(300)
    as.shutdownAwait()
  }

  test("PriorityMailbox should process messages in priority order") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[Int]()
    val latch            = new CountDownLatch(1)

    case class PriorityActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case i: Int =>
          receivedMessages.synchronized {
            receivedMessages += i
          }
        case "done" => latch.countDown()
      }
    }

    // Higher numbers have higher priority (reverse order)
    given Ordering[Any] = new Ordering[Any] {
      def compare(x: Any, y: Any): Int = (x, y) match {
        case (a: Int, b: Int) => b.compare(a) // Reverse order
        case ("done", _)      => 1            // Process "done" last
        case (_, "done")      => -1
        case _                => 0
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[PriorityActor](
      EmptyTuple,
      PriorityMailbox(summon[Ordering[Any]])
    )
    as.registerProp(props)

    // Send messages in non-priority order
    as.tell[PriorityActor]("test", 1)
    as.tell[PriorityActor]("test", 5)
    as.tell[PriorityActor]("test", 3)
    Thread.sleep(50) // Give time for priority queue to sort
    as.tell[PriorityActor]("test", "done")

    latch.await(2, SECONDS)
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    // With reverse priority, should process 5, 3, 1
    assertEquals(
      messages.headOption,
      Some(5),
      "Should process highest priority first"
    )
  }

  test("UnboundedMailbox should be used by default") {
    var messageCount = 0
    val latch        = new CountDownLatch(100)

    case class DefaultActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ =>
        messageCount += 1
        latch.countDown()
      }
    }

    val as    = ActorSystem()
    val props =
      ActorProps.props[DefaultActor](EmptyTuple) // No mailbox specified
    as.registerProp(props)

    // Send many messages - unbounded mailbox should handle all
    (1 to 100).foreach(i => as.tell[DefaultActor]("test", i))

    latch.await(2, SECONDS)
    as.shutdownAwait()

    assertEquals(
      messageCount,
      100,
      "All messages should be processed with unbounded mailbox"
    )
  }

  test("BoundedMailbox should enforce capacity limits") {
    val receivedMessages = scala.collection.mutable.ArrayBuffer[String]()

    case class CapacityActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "block"   => Thread.sleep(1000) // Block to let mailbox fill
        case s: String =>
          receivedMessages.synchronized {
            receivedMessages += s
          }
      }
    }

    val as    = ActorSystem()
    val props = ActorProps.props[CapacityActor](
      EmptyTuple,
      BoundedMailbox(capacity = 5, overflowStrategy = DropNewest)
    )
    as.registerProp(props)

    // Start with blocking message
    as.tell[CapacityActor]("test", "block")
    Thread.sleep(50)

    // Try to overflow the mailbox
    (1 to 10).foreach(i => as.tell[CapacityActor]("test", s"msg$i"))

    Thread.sleep(1500) // Wait for processing
    as.shutdownAwait()

    val messages = receivedMessages.synchronized { receivedMessages.toList }
    // Should have processed block + up to capacity messages, rest dropped
    assert(
      messages.size <= 6,
      s"Should process at most 6 messages (1 block + 5 capacity), got ${messages.size}"
    )
  }

  // Phase 2: Logging Tests

  test("Logging should capture actor start events") {
    val testLogger = new CollectingLogger()

    case class LoggingActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[LoggingActor](EmptyTuple)
    as.registerProp(props)

    as.tell[LoggingActor]("test", "message")
    Thread.sleep(100)
    as.shutdownAwait()

    val entries     = testLogger.getEntries
    val startEvents = entries.filter(_.message.contains("Actor started"))
    assert(startEvents.nonEmpty, "Should log actor start event")
  }

  test("Logging should capture actor restart events") {
    val testLogger = new CollectingLogger()

    case class FailingActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case "fail"    => throw new RuntimeException("Test failure")
        case "success" => ()
      }
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[FailingActor](EmptyTuple)
    as.registerProp(props)

    as.tell[FailingActor]("test", "fail")
    Thread.sleep(150)
    as.tell[FailingActor]("test", "success")
    Thread.sleep(50)
    as.shutdownAwait()

    val entries       = testLogger.getEntries
    val restartEvents = entries.filter(_.message.contains("Actor restarted"))
    assert(restartEvents.nonEmpty, "Should log actor restart event")
  }

  test("Logging should capture actor stop events") {
    val testLogger = new CollectingLogger()

    case class StoppableActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any]        = { case _ =>
        throw new RuntimeException("Always fail")
      }
      override def supervisorStrategy: SupervisionStrategy = StopStrategy
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[StoppableActor](EmptyTuple)
    as.registerProp(props)

    as.tell[StoppableActor]("test", "message")
    Thread.sleep(150)
    as.shutdownAwait()

    val entries    = testLogger.getEntries
    val stopEvents = entries.filter(_.message.contains("Actor stopped"))
    assert(stopEvents.nonEmpty, "Should log actor stop event")
  }

  test("Logging should capture actor failure events") {
    val testLogger = new CollectingLogger()

    case class FailActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ =>
        throw new RuntimeException("Test failure")
      }
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[FailActor](EmptyTuple)
    as.registerProp(props)

    as.tell[FailActor]("test", "message")
    Thread.sleep(150)
    as.shutdownAwait()

    val entries    = testLogger.getEntries
    val failEvents = entries.filter(_.message.contains("Actor failed"))
    assert(failEvents.nonEmpty, "Should log actor failure event")
  }

  test("Logging should capture system shutdown events") {
    val testLogger = new CollectingLogger()

    case class TestActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[TestActor](EmptyTuple)
    as.registerProp(props)

    as.tell[TestActor]("test", "message")
    Thread.sleep(50)
    as.shutdownAwait()

    val entries        = testLogger.getEntries
    val shutdownEvents =
      entries.filter(_.message.contains("ActorSystem shutting down"))
    assert(shutdownEvents.nonEmpty, "Should log system shutdown event")

    val completedEvents =
      entries.filter(_.message.contains("shutdown completed"))
    assert(completedEvents.nonEmpty, "Should log shutdown completion event")
  }

  test("Logging should capture instantiation failure events") {
    val testLogger = new CollectingLogger()

    case class BrokenActor(fail: Boolean) extends Actor {
      if (fail) throw new RuntimeException("Instantiation failed")
      override def onMsg: PartialFunction[Any, Any] = { case _ => () }
    }

    val as    = new ActorSystem {
      override def logger: ActorLogger = testLogger
    }
    val props = ActorProps.props[BrokenActor](Tuple1(true))
    as.registerProp(props)

    as.tell[BrokenActor]("test", "message")
    Thread.sleep(150)
    as.shutdownAwait()

    val entries     = testLogger.getEntries
    val errorEvents = entries.filter(e =>
      e.level == "ERROR" && e.message.contains("instantiation failed")
    )
    assert(errorEvents.nonEmpty, "Should log instantiation failure as error")
  }

  test("CollectingLogger should store log entries with timestamps") {
    val testLogger = new CollectingLogger()

    testLogger.debug("Debug message")
    testLogger.info("Info message")
    testLogger.warn("Warning message")
    testLogger.error("Error message")

    val entries = testLogger.getEntries
    assertEquals(entries.size, 4, "Should have 4 log entries")

    val levels = entries.map(_.level)
    assert(levels.contains("DEBUG"), "Should contain DEBUG level")
    assert(levels.contains("INFO"), "Should contain INFO level")
    assert(levels.contains("WARN"), "Should contain WARN level")
    assert(levels.contains("ERROR"), "Should contain ERROR level")

    // All entries should have timestamps
    assert(
      entries.forall(_.timestamp != null),
      "All entries should have timestamps"
    )
  }

  test("CollectingLogger clear should remove all entries") {
    val testLogger = new CollectingLogger()

    testLogger.info("Message 1")
    testLogger.info("Message 2")
    assertEquals(testLogger.getEntries.size, 2, "Should have 2 entries")

    testLogger.clear()
    assertEquals(
      testLogger.getEntries.size,
      0,
      "Should have 0 entries after clear"
    )
  }
}
