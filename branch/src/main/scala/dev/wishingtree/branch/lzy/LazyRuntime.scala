package dev.wishingtree.branch.lzy

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.util.Try

private[lzy] trait LazyRuntime {
  def runSync[A](lzy: Lazy[A], d: Duration)(using
      executionContext: ExecutionContext
  ): Try[A]
  def runAsync[A](lzy: Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A]
}

object LazyRuntime extends LazyRuntime {

  /** Run a Lazy value synchronously.
    */
  override final def runSync[A](lzy: Lazy[A], d: Duration = Duration.Inf)(using
      executionContext: ExecutionContext
  ): Try[A] =
    Try(
      Await.result(eval(lzy), d)
    )

  /** Run a Lazy value asynchronously.
    */
  override final def runAsync[A](lzy: Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A] =
    eval(lzy)

  @tailrec
  private final def eval[A](
      lzy: Lazy[A]
  )(using executionContext: ExecutionContext): Future[A] = {
    lzy match {
      case Lazy.Fn(a)           => Future(a())
      case Lazy.FlatMap(lzy, f) =>
        lzy match {
          case Lazy.FlatMap(l, g) => eval(l.flatMap(g(_).flatMap(f)))
          case Lazy.Fn(a)         => eval(f(a()))
          case Lazy.Fail(e)       => Future.failed(e)
          case Lazy.Recover(l, r) => evalFlatMapRecover(l, r, f)
          case Lazy.Sleep(d)      => evalFlatMapSleep(d, f)
        }
      case Lazy.Fail(e)         => Future.failed(e)
      case Lazy.Recover(lzy, f) => evalRecover(lzy, f)
      case Lazy.Sleep(d)        => Future(Thread.sleep(d.toMillis))
    }
  }

  private final def evalFlatMapSleep[A](d: Duration, f: Unit => Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A] = {
    Future {
      Thread.sleep(d.toMillis)
    }.flatMap(_ => eval(f(())))
  }

  private final def evalFlatMapRecover[A, B](
      lzy: Lazy[A],
      r: Throwable => Lazy[A],
      f: A => Lazy[B]
  )(using executionContext: ExecutionContext): Future[B] = {
    evalRecover(lzy, r).flatMap(z => eval(f(z)))
  }

  private final def evalRecover[A](
      lzy: Lazy[A],
      f: Throwable => Lazy[A]
  )(using executionContext: ExecutionContext): Future[A] = {
    lzy match {
      case Lazy.Fail(e) => eval(f(e))
      case _            => eval(lzy).recoverWith { case t: Throwable => eval(f(t)) }
    }
  }
}
