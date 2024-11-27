package dev.wishingtree.branch.keanu.actors

import munit.FunSuite

import java.util.concurrent.CountDownLatch

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
    latch.await()
    assertEquals(counter, 4)

  }

}
