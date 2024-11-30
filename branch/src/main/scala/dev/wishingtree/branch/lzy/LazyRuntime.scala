package dev.wishingtree.branch.lzy

import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors

import java.util.concurrent.{CompletableFuture, ExecutorService}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.util.{Failure, Success, Try}

private[lzy] trait LazyRuntime {
  def runSync[A](lzy: Lazy[A]): Try[A]
  def runAsync[A](lzy: Lazy[A]): Future[A]
}

object LazyRuntime extends LazyRuntime {

  /** The default executor service for running Lazy values. Uses
    * [[BranchExecutors.executorService]]
    */
  final val executorService: ExecutorService =
    BranchExecutors.executorService

  /** The default execution context for running Lazy values. Uses
    * [[BranchExecutors.executionContext]]
    */
  final val executionContext: ExecutionContext =
    BranchExecutors.executionContext

  /** Run a Lazy value synchronously.
    */
  override final def runSync[A](lzy: Lazy[A]): Try[A] =
    eval(lzy).get()

  /** Run a Lazy value asynchronously.
    */
  override final def runAsync[A](lzy: Lazy[A]): Future[A] =
    eval(lzy).asScala.flatMap(t => Future.fromTry(t))(executionContext)

  private final def eval[A](lzy: Lazy[A]): CompletableFuture[Try[A]] = {
    CompletableFuture.supplyAsync(
      () => {
        lzy match {
          case Lazy.Fn(a)                               =>
            Try(a())
          case Lazy.FlatMap(lzy, f: (Any => Lazy[Any])) =>
            eval(lzy).get() match {
              case Success(a) => eval(f(a)).get()
              case Failure(e) => Failure(e)
            }
          case Lazy.Fail(e)                             =>
            Failure[A](e)
          case Lazy.Recover(lzy, f)                     =>
            eval(lzy).get match {
              case Failure(e) => eval(f(e)).get
              case success    => success
            }
          case Lazy.Sleep(duration)                     =>
            Thread.sleep(duration.toMillis)
            Success(())
        }
      },
      executorService
    )
  }

}
