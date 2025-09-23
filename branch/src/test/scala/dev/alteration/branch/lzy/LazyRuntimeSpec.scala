package dev.alteration.branch.lzy

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import munit.FunSuite

import scala.util.*
import scala.concurrent.{ExecutionContext, TimeoutException}
import scala.concurrent.duration.*
import scala.language.postfixOps

class LazyRuntimeSpec extends FunSuite {
  given ExecutionContext = BranchExecutors.executionContext

  test("LazyRuntime.runSync() captures the Future failure in the Try") {
    val lazyFail = Lazy.fail(new ArithmeticException("bad math"))
    assert(
      LazyRuntime.runSync(lazyFail) match {
        case Failure(e: ArithmeticException) => true
        case _                               => false
      }
    )
    val lazyBoom = Lazy.fn(throw new ArithmeticException("bad math"))
    assert(
      LazyRuntime.runSync(lazyBoom) match {
        case Failure(e: ArithmeticException) => true
        case _                               => false
      }
    )
  }

  test(
    "LazyRuntime.runSync() times out when appropriate"
  ) {
    assert(
      LazyRuntime.runSync(
        Lazy.sleep(100 milliseconds),
        1 milliseconds
      ) match {
        case Failure(e: TimeoutException) => true
        case _                            => false
      }
    )
  }

  test(
    "LazyRuntime.runSync() doesn't time out when appropriate"
  ) {
    assert(
      LazyRuntime
        .runSync(
          Lazy.sleep(5 milliseconds),
          10 milliseconds
        )
        .isSuccess
    )
  }
}
