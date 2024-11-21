package dev.wishingtree.branch.lzy

import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*
import scala.util.{Failure, Success, Try}

trait LazyRuntime {
  def runSync[A](lzy: Lazy[A]): Try[A]
  def runAsync[A](lzy: Lazy[A]): Future[A]
}

object LazyRuntime extends LazyRuntime {

  val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(executorService)

  override def runSync[A](lzy: Lazy[A]): Try[A] =
    eval(lzy).get()

  override def runAsync[A](lzy: Lazy[A]): Future[A] =
    eval(lzy).asScala.flatMap(t => Future.fromTry(t))(executionContext)

  private def eval[A](lzy: Lazy[A]): CompletableFuture[Try[A]] = {
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
        }
      },
      executorService
    )
  }

}
