package dev.wishingtree.branch.lzy

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.util.Try

private[lzy] trait LazyRuntime {
  def runSync[A](lzy: Lazy[A])(d: Duration)(using
      executionContext: ExecutionContext
  ): Try[A]
  def runAsync[A](lzy: Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A]
}

object LazyRuntime extends LazyRuntime {

  /** Run a Lazy value synchronously.
    */
  override final def runSync[A](lzy: Lazy[A])(d: Duration = Duration.Inf)(using
      executionContext: ExecutionContext
  ): Try[A] =
    Try(
      Await.result(evalF(lzy), d)
    )

  /** Run a Lazy value asynchronously.
    */
  override final def runAsync[A](lzy: Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A] =
    evalF(lzy)

  @tailrec
  private final def evalF[A](
      lzy: Lazy[A]
  )(using executionContext: ExecutionContext): Future[A] = {
    lzy match {
      case Lazy.Fn(a)           => Future(a())
      case Lazy.Suspend(s)      => evalF(s())
      case Lazy.FlatMap(lzy, f) =>
        lzy match {
          case Lazy.Suspend(s)    => evalF(Lazy.FlatMap(s(), f))
          case Lazy.FlatMap(l, g) => evalF(l.flatMap(g(_).flatMap(f)))
          case Lazy.Fn(a)         => evalF(f(a()))
          case Lazy.Fail(e)       => Future.failed(e)
          case Lazy.Recover(l, r) => evalFFlatMapRecover(l, r, f)
          case Lazy.Sleep(d)      => evalFFlatMapSleep(d, f)
        }
      case Lazy.Fail(e)         => Future.failed(e)
      case Lazy.Recover(lzy, f) => evalFRecover(lzy, f)
      case Lazy.Sleep(d)        => Future(Thread.sleep(d.toMillis))
    }
  }

  private final def evalFFlatMapSleep[A](d: Duration, f: Unit => Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A] = {
    Future {
      Thread.sleep(d.toMillis)
    }.flatMap(_ => evalF(f(())))
  }

  private final def evalFFlatMapRecover[A, B](
      lzy: Lazy[A],
      r: Throwable => Lazy[A],
      f: A => Lazy[B]
  )(using executionContext: ExecutionContext): Future[B] = {
    evalFRecover(lzy, r).flatMap(z => evalF(f(z)))
  }

  private final def evalFRecover[A](
      lzy: Lazy[A],
      f: Throwable => Lazy[A]
  )(using executionContext: ExecutionContext): Future[A] = {
    lzy match {
      case Lazy.Fail(e) => evalF(f(e))
      case _            => evalF(lzy).recoverWith { case t: Throwable => evalF(f(t)) }
    }
  }
}
