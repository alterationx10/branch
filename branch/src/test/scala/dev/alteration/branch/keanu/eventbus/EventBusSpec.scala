package dev.alteration.branch.keanu.eventbus

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

class EventBusSpec extends munit.FunSuite {

  test("EventBus") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(5)

    val testEventBus = new EventBus[Int] {}

    testEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    for (i <- 1 to 5) {
      testEventBus.publishNoTopic(i)
    }

    latch.await()
    assertEquals(counter, 15)

  }

  test("Eventbus topic filter") {
    @volatile
    var counter = 0

    val latch = new CountDownLatch(2)

    val testEventBus = new EventBus[Int] {}

    testEventBus.subscribe(
      (msg: EventBusMessage[Int]) => {
        counter += msg.payload
        latch.countDown()
      },
      _.topic == "a"
    )

    for (i <- 1 to 5) {
      val topic = if i % 2 == 0 then "a" else "b"
      testEventBus.publish(topic, i)
    }

    latch.await()
    assertEquals(counter, 6)
  }

  test("unsubscribe by subscriber") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(1)

    // Use a fresh instance to avoid state leakage between tests
    val testEventBus = new EventBus[Int] {}

    val subscriber = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        counter += msg.payload
        latch.countDown()
      }
    }

    testEventBus.subscribe(subscriber)

    testEventBus.publishNoTopic(1)
    latch.await(1, SECONDS) // Wait for first message to be processed
    testEventBus.unsubscribe(subscriber)
    testEventBus.publishNoTopic(2)

    Thread.sleep(
      20
    ) // Give time for any potential (unwanted) message processing
    assertEquals(counter, 1)
  }

  test("unsubscribe by id") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(1)

    // Use a fresh instance to avoid state leakage between tests
    val testEventBus = new EventBus[Int] {}

    val subId = testEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    testEventBus.publishNoTopic(1)
    latch.await(1, SECONDS) // Wait for first message to be processed
    testEventBus.unsubscribe(subId)
    testEventBus.publishNoTopic(2)

    Thread.sleep(
      20
    ) // Give time for any potential (unwanted) message processing
    assertEquals(counter, 1)
  }

  test("multiple subscribers") {
    @volatile
    var counter1 = 0
    @volatile
    var counter2 = 0
    val latch    = new CountDownLatch(4)

    val testEventBus = new EventBus[Int] {}

    testEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter1 += msg.payload
      latch.countDown()
    })

    testEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter2 += msg.payload * 2
      latch.countDown()
    })

    testEventBus.publishNoTopic(1)
    testEventBus.publishNoTopic(2)

    latch.await()
    assertEquals(counter1, 3)
    assertEquals(counter2, 6)
  }

  test("subscriber error handling") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(2)

    val testEventBus = new EventBus[Int] {}

    // First subscriber throws an exception
    testEventBus.subscribe((_: EventBusMessage[Int]) => {
      throw new RuntimeException("Test exception")
    })

    // Second subscriber should still receive messages
    testEventBus.subscribe((msg: EventBusMessage[Int]) => {
      counter += msg.payload
      latch.countDown()
    })

    testEventBus.publishNoTopic(1)
    testEventBus.publishNoTopic(2)

    latch.await()
    assertEquals(counter, 3)
  }

  test("shutdown cleans up all subscribers") {
    val counter      = new AtomicInteger(0)
    val messageLatch = new CountDownLatch(2)
    val readyLatch   = new CountDownLatch(2)

    // Create a fresh instance, not a singleton object
    val testEventBus = new EventBus[Int] {}

    val sub1 = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        // Signal ready on first message
        if (msg.payload == 0) {
          readyLatch.countDown()
        } else {
          counter.addAndGet(msg.payload)
          messageLatch.countDown()
        }
      }
    }

    val sub2 = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        // Signal ready on first message
        if (msg.payload == 0) {
          readyLatch.countDown()
        } else {
          counter.addAndGet(msg.payload * 2)
          messageLatch.countDown()
        }
      }
    }

    testEventBus.subscribe(sub1)
    testEventBus.subscribe(sub2)

    // Send ready signal and wait for both subscribers to be ready
    testEventBus.publishNoTopic(0)
    readyLatch.await()

    // Now publish the real message
    testEventBus.publishNoTopic(1)
    messageLatch.await()
    assertEquals(counter.get(), 3)

    // Shutdown the event bus
    testEventBus.shutdown()

    // Messages published after shutdown should not be processed
    val oldCounter = counter.get()
    testEventBus.publishNoTopic(10)
    Thread.sleep(20) // Give time for any potential message processing

    assertEquals(
      counter.get(),
      oldCounter,
      "No messages should be processed after shutdown"
    )
  }

  test("subscriber with AutoCloseable") {
    @volatile
    var counter = 0
    val latch   = new CountDownLatch(1)

    // Create a fresh instance, not a singleton object
    val testEventBus = new EventBus[Int] {}

    val subscriber = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        counter += msg.payload
        latch.countDown()
      }
    }

    testEventBus.subscribe(subscriber)
    testEventBus.publishNoTopic(5)
    latch.await()
    assertEquals(counter, 5)

    // Use try-with-resources pattern
    subscriber.close()

    // Messages after close should not be processed
    val oldCounter = counter
    testEventBus.publishNoTopic(10)
    Thread.sleep(20) // Give time for any potential message processing

    assertEquals(
      counter,
      oldCounter,
      "No messages should be processed after close"
    )
  }

  test("onPublishError callback for filter exceptions") {
    @volatile
    var errorCount                                 = 0
    @volatile
    var lastError: Option[Throwable]               = None
    @volatile
    var lastSubscriptionId: Option[java.util.UUID] = None

    val testEventBus = new EventBus[Int] {
      override def onPublishError(
          error: Throwable,
          message: EventBusMessage[Int],
          subscriptionId: java.util.UUID
      ): Unit = {
        errorCount += 1
        lastError = Some(error)
        lastSubscriptionId = Some(subscriptionId)
      }
    }

    // Subscribe with a filter that throws an exception
    val subId = testEventBus.subscribe(
      Subscriber[Int](_ => ()),
      _ => throw new RuntimeException("Filter error!")
    )

    // Publish a message
    testEventBus.publishNoTopic(42)
    Thread.sleep(10) // Give time for error handling

    assertEquals(errorCount, 1)
    assert(lastError.isDefined)
    assertEquals(lastError.get.getMessage, "Filter error!")
    assert(lastSubscriptionId.isDefined)
    assertEquals(lastSubscriptionId.get, subId)
  }

  test("onPublishError does not block other subscribers") {
    val errorCount     = new AtomicInteger(0)
    val successCounter = new AtomicInteger(0)
    val latch          = new CountDownLatch(2)

    val testEventBus = new EventBus[Int] {
      override def onPublishError(
          error: Throwable,
          message: EventBusMessage[Int],
          subscriptionId: java.util.UUID
      ): Unit = {
        errorCount.incrementAndGet()
      }
    }

    // First subscriber with failing filter
    testEventBus.subscribe(
      Subscriber[Int](_ => ()),
      _ => throw new RuntimeException("Boom!")
    )

    // Second subscriber that should still receive messages
    testEventBus.subscribe(Subscriber[Int] { msg =>
      successCounter.addAndGet(msg.payload)
      latch.countDown()
    })

    // Third subscriber that should also receive messages
    testEventBus.subscribe(Subscriber[Int] { msg =>
      successCounter.addAndGet(msg.payload * 2)
      latch.countDown()
    })

    testEventBus.publishNoTopic(1)
    latch.await()

    assertEquals(
      errorCount.get(),
      1,
      "Should have one error from failing filter"
    )
    assertEquals(
      successCounter.get(),
      3,
      "Other subscribers should still receive messages"
    )
  }

  test("subscriber onError callback for message processing exceptions") {
    @volatile
    var errorCount                                = 0
    @volatile
    var lastError: Option[Throwable]              = None
    @volatile
    var lastMessage: Option[EventBusMessage[Int]] = None
    @volatile
    var successCount                              = 0

    val latch = new CountDownLatch(2)

    val testEventBus = new EventBus[Int] {}

    val subscriber = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        if (msg.payload < 0) {
          throw new IllegalArgumentException("Negative value!")
        }
        successCount += 1
        latch.countDown()
      }

      override def onError(
          error: Throwable,
          message: EventBusMessage[Int]
      ): Unit = {
        errorCount += 1
        lastError = Some(error)
        lastMessage = Some(message)
        latch.countDown()
      }
    }

    testEventBus.subscribe(subscriber)

    // Send one good message
    testEventBus.publishNoTopic(5)
    // Send one bad message
    testEventBus.publishNoTopic(-1)

    latch.await()

    assertEquals(successCount, 1, "One message should process successfully")
    assertEquals(errorCount, 1, "One error should be caught")
    assert(lastError.isDefined)
    assertEquals(lastError.get.getMessage, "Negative value!")
    assert(lastMessage.isDefined)
    assertEquals(lastMessage.get.payload, -1)
  }

  test("subscriber continues processing after onMsg exception") {
    @volatile
    var processedCount = 0
    val latch          = new CountDownLatch(3)

    val testEventBus = new EventBus[Int] {}

    val subscriber = new Subscriber[Int] {
      override def onMsg(msg: EventBusMessage[Int]): Unit = {
        if (msg.payload == 2) {
          throw new RuntimeException("Error on 2!")
        }
        processedCount += 1
        latch.countDown()
      }

      override def onError(
          error: Throwable,
          message: EventBusMessage[Int]
      ): Unit = {
        // Still count down to show error was handled
        latch.countDown()
      }
    }

    testEventBus.subscribe(subscriber)

    // Send three messages, middle one will fail
    testEventBus.publishNoTopic(1)
    testEventBus.publishNoTopic(2) // This will throw
    testEventBus.publishNoTopic(3)

    latch.await()

    assertEquals(
      processedCount,
      2,
      "Should process 2 messages (1 and 3), skipping the failing one"
    )
  }
}
