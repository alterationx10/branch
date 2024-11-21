package dev.wishingtree.branch.keanu.eventbus

class EventBusTest extends munit.FunSuite {

  test("EventBus") {
    @volatile
    var counter = 0

    object TestEventBus extends EventBus[Int]

    TestEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
    })

    for (i <- 1 to 5) {
      TestEventBus.publishNoTopic(i)
    }

    TestEventBus.drainAwait

    assertEquals(counter, 15)

  }

  test("Eventbus topic filter") {
    @volatile
    var counter = 0

    object TestEventBus extends EventBus[Int]

    TestEventBus.subscribe(
      (msg: EventBusMessage[Int]) => {
        counter += msg.payload
      },
      _.topic == "a"
    )

    for (i <- 1 to 5) {
      val topic = if i % 2 == 0 then "a" else "b"
      TestEventBus.publish(topic, i)
    }

    // Race condition - this runs before 4 is added
    // need to investigate, but running twice holds for now
    TestEventBus.drainAwait
    TestEventBus.drainAwait

    assertEquals(counter, 6)
  }
}
