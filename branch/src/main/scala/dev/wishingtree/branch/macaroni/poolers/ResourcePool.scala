package dev.wishingtree.branch.macaroni.poolers

import java.util.concurrent.{ConcurrentLinkedQueue, Semaphore}
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Success, Try}

trait ResourcePool[R] {

  private val isShuttingDown: AtomicBoolean =
    new AtomicBoolean(false)

  def isShutdown: Boolean =
    isShuttingDown.get()

  val poolSize: Int =
    5

  private val gate: Semaphore =
    new Semaphore(poolSize, true)

  private val pool: ConcurrentLinkedQueue[R] =
    new ConcurrentLinkedQueue[R]()

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
      throw new IllegalStateException("ResourcePool is shutting down")
    gate.acquire()
    // Lazily fill the pool
    Option(pool.poll()).getOrElse(acquire)
  }

  private def returnResource(resource: R): Unit = {
    Try(test(resource)) match {
      case Success(true) =>
        pool.add(resource)
      case _             =>
        Try(release(resource))
    }
    gate.release()
  }

  def fillPool(): Unit = {
    gate.acquire(poolSize)
    try {
      while (pool.size < poolSize) {
        pool.add(acquire)
      }
    } finally {
      gate.release(poolSize)
    }

  }

  def drainPool(): Unit = {
    // Wait until all resources are returned
    gate.acquire(poolSize)
    // Release all resources
    pool.iterator().forEachRemaining { r =>
      release(r)
      pool.remove(r)
    }
    // Release the gate
    gate.release(poolSize)
  }

  def shutdown(): Unit = {
    // Prevent new resources from being acquired
    isShuttingDown.set(true)
    drainPool()
  }

  Runtime.getRuntime.addShutdownHook {
    new Thread(() => {
      if !isShuttingDown.get() then shutdown()
    })
  }

}
