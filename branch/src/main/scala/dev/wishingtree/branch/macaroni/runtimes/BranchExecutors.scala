package dev.wishingtree.branch.macaroni.runtimes

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext

object BranchExecutors {
  lazy val executorService: ExecutorService =
    Executors.newVirtualThreadPerTaskExecutor()

  lazy val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(executorService)
}
