package dev.wishingtree.branch.macaroni.poolers

import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors
import munit.FunSuite

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ResourcePoolSpec extends FunSuite {

  case class OnePool() extends ResourcePool[Int] {
    override def acquire: Int                 = 1
    override def release(resource: Int): Unit = ()
  }

  test("ResourcePool - async use") {
    val pool = OnePool()

    val asyncSum = (1 to 100).map { _ =>
      Future {
        pool.use(identity)
      }(BranchExecutors.executionContext)
    }

    val sum = Await
      .result(
        Future.sequence(asyncSum)(implicitly, BranchExecutors.executionContext),
        Duration.Inf
      )
      .sum

    assertEquals(sum, 100)
  }

}
