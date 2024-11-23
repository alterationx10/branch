import dev.wishingtree.branch.keanu.actors.{Actor, ActorContext, ActorSystem}
import dev.wishingtree.branch.keanu.eventbus.{EventBus, EventBusMessage, Subscriber}

object IntEventBus extends EventBus[Int]

object KeanuExample { self =>

  def main(args: Array[String]): Unit = {

    case class EchoActor() extends Actor {
      override def onMsg: PartialFunction[Any, Any] = { case any =>
        println(s"Echo: $any")
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

    val as      = ActorSystem()
    val saProps = ActorContext.props[SampleActor](as)
    as.registerProp(saProps)
    as.registerProp(ActorContext.props[EchoActor]())

    val counterActor = as.tell[SampleActor]("counter", _)

    counterActor(1)
    counterActor(2)
    counterActor(3)
    counterActor(4)
    counterActor("boom")
    counterActor(5)
    counterActor("print")

    as.shutdownAwait

  }

}
