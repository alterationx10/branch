package dev.wishingtree.branch.lzy

import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors

import scala.concurrent.ExecutionContext

/** A trait for creating a lazy application with a default main method.
  */
trait LazyApp {

  given ec: ExecutionContext = BranchExecutors.executionContext

  /** The main Lazy chain to run the application.
    */
  def run: Lazy[Any]

  /** The main method to run the application.
    */
  final def main(args: Array[String]): Unit =
    LazyRuntime.runSync(run)()
}
