package dev.wishingtree.branch.keanu.eventbus

import java.util.concurrent.{CountDownLatch, TimeUnit}

class EventBusTest extends munit.FunSuite {

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
}
