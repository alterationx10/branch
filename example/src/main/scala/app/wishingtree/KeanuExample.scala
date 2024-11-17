package app.wishingtree

import dev.wishingtree.branch.keanu.actors.{Actor, ActorContext, ActorSystem}
import dev.wishingtree.branch.keanu.eventbus.{
  EventBus,
  EventMessage,
  Subscriber
}

object IntEventBus extends EventBus[Int]

object KeanuExample extends Subscriber[Int] { self =>

  override def onMessage(msg: Int): Unit =
    println(s"Got Int = $msg")

  def main(args: Array[String]): Unit = {

    case class SampleActor() extends Actor {

      var counter = 0

      override def onMsg: PartialFunction[Any, Any] = {
        case n: Int  => {
          counter += n
          println(counter)
        }
        case "count" => counter
        case "print" => println(s"Counter is $counter")
        case _       => println("Unhandled")
      }
    }

    val saProps = ActorContext.props[SampleActor]()
    val as      = new ActorSystem {}
    as.registerProp(saProps)

    val counterActor = as.actorForName[SampleActor]("counter")

    IntEventBus.subscribe((msg: Int) => counterActor.tell(msg))
    IntEventBus.publish(EventMessage("", 1))
    IntEventBus.publish(EventMessage("", 2))
    IntEventBus.publish(EventMessage("", 3))
    IntEventBus.publish(EventMessage("", 4))
    IntEventBus.publish(EventMessage("", 5))
    counterActor.tell("print")

    as.shutdownAwait


  }

}
