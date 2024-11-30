package dev.wishingtree.branch.macaroni.runtimes

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext

/** BranchExecutors provides a global executor service and execution context for
  * use in Branch.
  */
object BranchExecutors {

  /** The global executor service. Uses a virtual thread per task.
    */
  lazy val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  /** The global execution context, using the [[executorService]]
    */
  lazy val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(executorService)
}
