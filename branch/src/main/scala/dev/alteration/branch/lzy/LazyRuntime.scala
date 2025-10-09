package dev.alteration.branch.lzy

import java.util.concurrent.{
  Executors,
  ScheduledExecutorService,
  TimeoutException,
  TimeUnit
}
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

private[lzy] trait LazyRuntime {
  def runSync[A](lzy: Lazy[A], d: Duration)(using
      executionContext: ExecutionContext
  ): Try[A]
  def runAsync[A](lzy: Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A]
}

object LazyRuntime extends LazyRuntime {

  // Shared scheduler for timeout and sleep operations
  private val scheduler: ScheduledExecutorService =
    Executors.newScheduledThreadPool(1)

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
      case Lazy.Fn(a)             => Future(a())
      case Lazy.Fail(e)           => Future.failed(e)
      case Lazy.Recover(lzy, f)   => evalRecover(lzy, f)
      case Lazy.Sleep(d)          => evalSleep(d)
      case Lazy.Timeout(lzy, d)   => evalTimeout(lzy, d)
      case Lazy.FromFuture(f)     => f
      case Lazy.Race(left, right) => evalRace(left, right)
      case Lazy.FlatMap(lzy, f)   =>
        lzy match {
          case Lazy.FlatMap(l, g)     => eval(l.flatMap(g(_).flatMap(f)))
          case Lazy.Fn(a)             => eval(f(a()))
          case Lazy.Fail(e)           => Future.failed(e)
          case Lazy.Recover(l, r)     => evalFlatMapRecover(l, r, f)
          case Lazy.Sleep(d)          => evalFlatMapSleep(d, f)
          case Lazy.Timeout(l, d)     => evalFlatMapTimeout(l, d, f)
          case Lazy.FromFuture(fut)   => evalFlatMapFromFuture(fut, f)
          case Lazy.Race(left, right) => evalFlatMapRace(left, right, f)
        }
    }
  }

  private final def evalFlatMapSleep[A](d: Duration, f: Unit => Lazy[A])(using
      executionContext: ExecutionContext
  ): Future[A] = {
    evalSleep(d).flatMap(_ => eval(f(())))
  }

  private final def evalFlatMapTimeout[A, B](
      lzy: Lazy[A],
      d: Duration,
      f: A => Lazy[B]
  )(using executionContext: ExecutionContext): Future[B] = {
    evalTimeout(lzy, d).flatMap(z => eval(f(z)))
  }

  private final def evalFlatMapFromFuture[A, B](
      fut: Future[A],
      f: A => Lazy[B]
  )(using executionContext: ExecutionContext): Future[B] = {
    fut.flatMap(a => eval(f(a)))
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

  private final def evalTimeout[A](
      lzy: Lazy[A],
      duration: Duration
  )(using executionContext: ExecutionContext): Future[A] = {
    val promise = Promise[A]()

    // Schedule timeout task
    val timeoutTask = scheduler.schedule(
      () => {
        promise.tryFailure(
          new TimeoutException(s"Operation timed out after $duration")
        )
      },
      duration.toMillis,
      TimeUnit.MILLISECONDS
    )

    // Run actual computation
    eval(lzy).onComplete { result =>
      timeoutTask.cancel(false) // Cancel timeout if computation finishes first
      promise.tryComplete(result)
    }

    promise.future
  }

  private final def evalSleep(
      duration: Duration
  ): Future[Unit] = {
    val promise = Promise[Unit]()
    scheduler.schedule(
      () => promise.success(()),
      duration.toMillis,
      TimeUnit.MILLISECONDS
    )
    promise.future
  }

  private final def evalRace[A, B](
      left: Lazy[A],
      right: Lazy[B]
  )(using executionContext: ExecutionContext): Future[A | B] = {
    val promise     = Promise[A | B]()
    val leftFuture  = eval(left)
    val rightFuture = eval(right)

    leftFuture.onComplete {
      case Success(a) => promise.trySuccess(a)
      case Failure(e) =>
        // If left fails, wait for right
        rightFuture.onComplete {
          case Success(b) => promise.trySuccess(b)
          case Failure(_) => promise.tryFailure(e) // Both failed
        }
    }

    rightFuture.onComplete {
      case Success(b) => promise.trySuccess(b)
      case Failure(e) =>
        // If right fails, wait for left
        leftFuture.onComplete {
          case Success(a) => promise.trySuccess(a)
          case Failure(_) => promise.tryFailure(e) // Both failed
        }
    }

    promise.future
  }

  private final def evalFlatMapRace[A, B, C](
      left: Lazy[A],
      right: Lazy[B],
      f: (A | B) => Lazy[C]
  )(using executionContext: ExecutionContext): Future[C] = {
    evalRace(left, right).flatMap(result => eval(f(result)))
  }
}
