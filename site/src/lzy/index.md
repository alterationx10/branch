# Lzy

*Lzy* is somewhere between a tiny Effect System, and lazy Futures.

## A Prelude

A (Scala) `Future` is an eager calculation - once referenced, it's going to run! This also means once it's done, we
can't re-run it - only evaluate the finished state. Sometimes it's beneficial to describe some logic in a lazy fashion,
where it's more of a blueprint of what to do. Calling the blueprint runs the code, whose output can be assigned to a
value, but then the blueprint can be run again!

Let's compare the following Future code to Lazy code:

```scala
val f1: Future[Int] = Future(Random.nextInt(10))
// f1 is already running, kicked off by an implicit ExecutionContext
val f2: Future[Int] = Future(Random.nextInt(10))
// f2 is now running...
def fRandomSum: Future[Int] = for {
  a <- f1
  b <- f2
} yield (a + b)
// fRandomSum will be the same every time it's called
println(Await(fRandomSum))
println(Await(fRandomSum))
println(Await(fRandomSum))
```

```scala
val l1: Lazy[Int] = Lazy.fn(Random.nextInt(10))
// l1 is a description of what you want to do, nothing is running yet 
val l2: Lazy[Int] = Lazy.fn(Random.nextInt(10))

def lzyRandomSum: Lazy[Int] = for {
  a <- l1
  b <- l2
} yield (a + b)
// lzyRandomSum will be different each time, because the whole blueprint is evaluated on each call
println(lzyRandomSum.runSync)
```

This description/lazy evaluation approach lets you structure you're programs in descriptive ways, but also has the
bonus of all the combinator effects you can add on. For example, let's say we want to add some ergonomic error handling
to one of out lazy function and add a way to `.recover` a failure.

```scala
def myLazyOp(arg: Int): Lazy[Int] =
  Lazy.fn(42 / arg)

myLazyOp(0).runSync
// -> Failure(Arithmetic Exception)

myLazyOp(0).recover(_ => Lazy.value(0)).runSync
// => Success(0)
```

The recovery doesn't have to be part of our definition of `myLazyOp`, and can be applied where needed, and different
recovery strategies used in various places.

## Building an Effect System

...

## Other Libraries

If you like *Lzy*, you should check out [ZIO](https://zio.dev/)
