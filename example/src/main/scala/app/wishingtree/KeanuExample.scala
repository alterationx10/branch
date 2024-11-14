package app.wishingtree

import dev.wishingtree.branch.keanu.eventbus.{EventBus, EventMessage, Subscriber}

object IntEventBus extends EventBus[Int]

object KeanuExample extends Subscriber[Int] { self =>

  override def onMessage(msg: Int): Unit =
    println(s"Got Int = $msg")

  def log(msg: Any): Unit = println(s"Got message $msg")

  def main(args: Array[String]): Unit = {

    IntEventBus.subscribe(self, _.payload > 5)
    IntEventBus.subscribe(self, _.payload % 2 == 0)
    
    IntEventBus.subscribe((msg: Int) => log(msg))

    IntEventBus.publish(EventMessage[Int]("tinyInts", 4))
    IntEventBus.publish(EventMessage[Int]("tinyInts", 6))
    IntEventBus.publish(EventMessage[Int]("tinyInts", 10))
    IntEventBus.publish(EventMessage[Int]("tinyInts", 9))

  }

}
