package dev.alteration.branch.keanu.eventbus

import java.util.concurrent.CountDownLatch
import scala.concurrent.duration._
class EventBusSpec extends munit.FunSuite {

  test("EventBus") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(5)

    object TestEventBus extends EventBus[Int]

    TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    for (i <- 1 to 5) {
      TestEventBus.publishNoTopic(i)
    }

    latch.await()
    assertEquals(counter, 15)

  }

  test("Eventbus topic filter") {
    @volatile
    var counter = 0

    val latch = new CountDownLatch(2)

    object TestEventBus extends EventBus[Int]

    TestEventBus.subscribe(
      (msg: EventBusMessage[Int]) => {
        counter += msg.payload
        latch.countDown()
      },
      _.topic == "a"
    )

    for (i <- 1 to 5) {
      val topic = if i % 2 == 0 then "a" else "b"
      TestEventBus.publish(topic, i)
    }

    latch.await()
    assertEquals(counter, 6)
  }

  test("unsubscribe by subscriber") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(1)

    object TestEventBus extends EventBus[Int]

    val subscriber = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        counter += msg.payload
        latch.countDown()
      }
    }

    TestEventBus.subscribe(subscriber)

    TestEventBus.publishNoTopic(1)
    TestEventBus.unsubscribe(subscriber)
    TestEventBus.publishNoTopic(2)

    latch.await(1, SECONDS)
    assertEquals(counter, 1)
  }

  test("unsubscribe by id") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(1)

    object TestEventBus extends EventBus[Int]

    val subId = TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    TestEventBus.publishNoTopic(1)
    TestEventBus.unsubscribe(subId)
    TestEventBus.publishNoTopic(2)

    latch.await(1, SECONDS)
    assertEquals(counter, 1)
  }

  test("multiple subscribers") {
    @volatile
    var counter1 = 0
    @volatile
    var counter2 = 0
    val latch    = new CountDownLatch(4)

    object TestEventBus extends EventBus[Int]

    TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter1 += msg.payload
      latch.countDown()
    })

    TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter2 += msg.payload * 2
      latch.countDown()
    })

    TestEventBus.publishNoTopic(1)
    TestEventBus.publishNoTopic(2)

    latch.await()
    assertEquals(counter1, 3)
    assertEquals(counter2, 6)
  }

  test("subscriber error handling") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(2)

    object TestEventBus extends EventBus[Int]

    // First subscriber throws an exception
    TestEventBus.subscribe((_: EventBusMessage[Int]) => {
      throw new RuntimeException("Test exception")
    })

    // Second subscriber should still receive messages
    TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    TestEventBus.publishNoTopic(1)
    TestEventBus.publishNoTopic(2)

    latch.await()
    assertEquals(counter, 3)
  }
}
