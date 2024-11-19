package app.wishingtree

import dev.wishingtree.branch.keanu.actors.{Actor, ActorContext, ActorSystem}
import dev.wishingtree.branch.keanu.eventbus.{
  EventBus,
  EventBusMessage,
  Subscriber
}

object IntEventBus extends EventBus[Int]

object KeanuExample { self =>

  def main(args: Array[String]): Unit = {

    case class EchoActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = {
        case any => println(s"$any")
      }
    }

    case class SampleActor(actorSystem: ActorSystem) extends Actor {
      println("starting actor")
      var counter = 0

      override def onMsg: PartialFunction[Any, Any] = {
        case n: Int  =>
          counter += n
          actorSystem.tell[EchoActor]("echo", s"Counter is now $counter")
        case "boom"  => 1 / 0
        case "count" =>
          counter
        case "print" => println(s"Counter is $counter")
        case _       => println("Unhandled")
      }
    }

    val as      = new ActorSystem {}
    val saProps = ActorContext.props[SampleActor](as)
    as.registerProp(saProps)
    as.registerProp(ActorContext.props[EchoActor]())

    val counterActor = as.tell[SampleActor]("counter", _)

    IntEventBus.subscribe((msg) => counterActor(msg.payload))
    IntEventBus.publish(EventBusMessage("", 1))
    IntEventBus.publish(EventBusMessage("", 2))
    IntEventBus.publish(EventBusMessage("", 3))
    IntEventBus.publish(EventBusMessage("", 4))
    counterActor("boom")
    IntEventBus.publish(EventBusMessage("", 5))
    counterActor("print")

    as.shutdownAwait

  }

}
