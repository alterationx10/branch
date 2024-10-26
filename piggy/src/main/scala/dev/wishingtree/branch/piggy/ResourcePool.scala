package dev.wishingtree.branch.piggy

import java.util.concurrent.Semaphore
import scala.collection.mutable.*

trait ResourcePool[R] {

  val poolSize: Int =
    5

  private val gate: Semaphore =
    new Semaphore(poolSize, true)

  private val pool: Queue[R] =
    Queue.empty[R]

  def acquire: R
  def release(resource: R): Unit

  def test(resource: R): Boolean =
    true

  private def fillPool: Unit = {
    gate.acquire(poolSize)
    synchronized {
      while (pool.size < poolSize) {
        pool.enqueue(acquire)
      }
    }
    gate.release(poolSize)
  }

  def use[A](fn: R => A): A = {
    val resource = borrow
    try {
      fn(resource)
    } finally {
      returnResource(resource)
    }
  }

  private def borrow: R = {
    gate.acquire()
    synchronized(pool.dequeue())
  }

  private def returnResource(resource: R): Unit = {
    synchronized {
      if (test(resource)) {
        pool.enqueue(resource)
      } else {
        release(resource)
        pool.enqueue(acquire)
      }
    }
    gate.release()
  }

  // fill the pool on create
  fillPool
}
