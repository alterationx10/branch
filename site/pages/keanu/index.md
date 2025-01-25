# Keanu

This module currently contains a simple *typed* EventBus, and a (local) ActorSystem.

## EventBus

Extend `EventBus[T]` for your type, e.g.

```scala 3
object IntEventBus extends EventBus[Int]
```

Then, you can have some implementation extend `Subsciber[T]` and subscribe, or pass in an anonymous implementation (e.g.
`IntEventBus.subscribe((msg: Int) => println(s"Got message $msg"))`)

Under the hood, the subscriber gets a queue of messages, and a thread that processes them, so things will be processed
in order, but asynchronously.

## ActorSystem

The `AcotrSystem` trait is implemented, and you can have an object extend it for easy singleton access. The `apply`
method also returns a new instance as well.

let's say you have two actors:

```scala 3
 case class EchoActor() extends Actor {
  override def onMsg: PartialFunction[Any, Any] = {
    case any =>
      println(s"Echo: $any")
  }
}

case class SampleActor(actorSystem: ActorSystem) extends Actor {
  println("starting actor")
  var counter = 0

  override def onMsg: PartialFunction[Any, Any] = {
    case n: Int =>
      counter += n
      actorSystem.tell[EchoActor]("echo", s"Counter is now $counter")
    case "boom" => 1 / 0
    case "count" =>
      counter
    case "print" => println(s"Counter is $counter")
    case _ => println("Unhandled")
  }
}
```

You can register `props` to the actor system which capture arguments used to create actor instances. This is a bit
unsafe in
the sense that it takes varargs that should be supplied to create the actor, so no compiler checks at the moment.

```scala 3
val as = ActorSystem()
val saProps = ActorContext.props[SampleActor](as)
as.registerProp(saProps)
as.registerProp(ActorContext.props[EchoActor]())
```

at this point, you can send messages to the actors with the `tell` method on the `ActorSystem`:

```scala 3
// Helper lambda to send messages to the SampleActor named counter
val counterActor = as.tell[SampleActor]("counter", _)
counterActor(1)
counterActor(2)
counterActor(3)
counterActor(4)
counterActor("boom")
counterActor(5)
counterActor("print")
```

Actors are indexed based on name and type, so you can have multiple actors of the same type, but they must have unique
names (and you can have actors with the same name, as long as they are different types).

If you want to shut down the ActorSystem, you can use the `shutdownAwait` method, which will send a `PoisonPill` to all
actors, and attempt to wait for their queues to finish processing.