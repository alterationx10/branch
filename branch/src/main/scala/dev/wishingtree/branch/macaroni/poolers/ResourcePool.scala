package dev.wishingtree.branch.macaroni.poolers

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable

trait ResourcePool[R] {

  val isShuttingDown: AtomicBoolean =
    new AtomicBoolean(false)

  val poolSize: Int =
    5

  private val gate: Semaphore =
    new Semaphore(poolSize, true)

  private val pool: mutable.Queue[R] =
    mutable.Queue.empty[R]

  def acquire: R
  def release(resource: R): Unit

  def test(resource: R): Boolean =
    true

  def use[A](fn: R => A): A = {
    val resource = borrowResource
    try {
      fn(resource)
    } finally {
      returnResource(resource)
    }
  }

  private def borrowResource: R = {
    if isShuttingDown.get() then
      throw new IllegalStateException("Pool is shutting down")
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

  private def fillPool(): Unit = {
    gate.acquire(poolSize)
    synchronized {
      try {
        while (pool.size < poolSize) {
          pool.enqueue(acquire)
        }
      } finally {
        gate.release(poolSize)
      }
    }
  }

  Runtime.getRuntime.addShutdownHook {
    new Thread(() => {
      // Prevent new resources from being acquired
      isShuttingDown.set(true)
      // Wait until all resources are returned
      gate.acquire(poolSize)
      // Release all resources
      synchronized {
        pool.dequeueAll { r =>
          release(r)
          true
        }
      }
      // Release the gate
      gate.release(poolSize)
    })
  }

  // Fill the pool on startup
  fillPool()
}
