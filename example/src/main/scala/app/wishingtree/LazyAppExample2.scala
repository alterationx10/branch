package app.wishingtree

import dev.wishingtree.branch.lzy.{Lazy, LazyApp, LazyRuntime}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

object LazyAppExample2 {

  def main(args: Array[String]): Unit = {
    given ExecutionContext = LazyRuntime.executionContext

    val f1: Future[Int] = Future(Random.nextInt(10))
    // f1 is already running, kicked off by an implicit ExecutionContext
    val f2: Future[Int] = Future(Random.nextInt(10))

    // f2 is now running...
    def fRandomSum: Future[Int] = for {
      a <- f1
      b <- f2
    } yield (a + b)

    // fRandomSum will be the same every time it's called
    println(Await.result(fRandomSum, Duration.Inf))
    println(Await.result(fRandomSum, Duration.Inf))
    println(Await.result(fRandomSum, Duration.Inf))

    val l1: Lazy[Int] = Lazy.fn(Random.nextInt(10))
    // l1 is a description of what you want to do, nothing is running yet
    val l2: Lazy[Int] = Lazy.fn(Random.nextInt(10))

    def lzyRandomSum: Lazy[Int] = for {
      a <- l1
      b <- l2
    } yield (a + b)
    // lzyRandomSum will be different each time, because the whole blueprint is evaluated on each call
    println(lzyRandomSum.runSync())
    println(lzyRandomSum.runSync())
    println(lzyRandomSum.runSync())

    def myLazyOp(arg: Int): Lazy[Int] =
      Lazy.fn(42 / arg)

    println(myLazyOp(0).runSync())
    // -> Failure(Arithmetic Exception)

    println(myLazyOp(0).recover(_ => Lazy.fn(0)).runSync())
  }

}
