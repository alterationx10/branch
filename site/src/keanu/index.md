# Keanu

This module currently contains a simple *typed* EventBus.

Have an object extend `EventBus[T]` for your type, e.g.

```scala 3
object IntEventBus extends EventBus[Int]
```

Then, you can have some implementation extend `Subsciber[T]` and subscribe, or pass in an anonymous implementation (e.g.
`IntEventBus.subscribe((msg: Int) => println(s"Got message $msg"))`)