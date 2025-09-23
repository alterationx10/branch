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
}
