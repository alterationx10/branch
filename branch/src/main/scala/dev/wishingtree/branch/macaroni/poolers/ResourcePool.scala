package dev.wishingtree.branch.macaroni.poolers

import java.util.concurrent.{ConcurrentLinkedQueue, Semaphore}
import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Success, Try}

/** A ResourcePool is a pool of resources that can be acquired and released. The
  * pool has a fixed size and will block if no resources are available.
  * Resources are lazily created and added to the pool.
  *
  * A shutdown hook is automatically added to ensure the pool is cleanly
  * drained/released before the JVM exits, if not previous shutdown.
  *
  * @tparam R
  *   the type of resource
  */
trait ResourcePool[R] {

  /** A flag controlling if the pool is shutting down.
    */
  private final val isShuttingDown: AtomicBoolean =
    new AtomicBoolean(false)

  /** Returns true if the pool is shutting down.
    */
  final def isShutdown: Boolean =
    isShuttingDown.get()

  /** The size of the pool. Defaults to 5
    */
  val poolSize: Int =
    5

  /** The semaphore controlling access to the pool.
    */
  private final val gate: Semaphore =
    new Semaphore(poolSize, true)

  /** The pool of resources.
    */
  private[poolers] final val pool: ConcurrentLinkedQueue[R] =
    new ConcurrentLinkedQueue[R]()

  /** Acquire a resource.
    */
  def acquire: R

  /** Release a resource.
    */
  def release(resource: R): Unit

  /** Test a resource to see if it is still valid on return to the pool.
    * Defaults to always true.
    */
  def test(resource: R): Boolean =
    true

  /** Acquire a resource from the pool to use in the provided thunk before
    * returning it to the pool.
    *
    * @throws IllegalStateException
    *   if the pool is shutting down
    */
  def use[A](fn: R => A): A = {
    val resource = borrowResource
    try {
      fn(resource)
    } finally {
      returnResource(resource)
    }
  }

  /** Borrow a resource from the pool.
    * @throws IllegalStateException
    *   if the pool is shutting down
    */
  private def borrowResource: R = {
    if isShuttingDown.get() then
      throw new IllegalStateException("ResourcePool is shutting down")
    gate.acquire()
    // Lazily fill the pool
    Option(pool.poll()).getOrElse(acquire)
  }

  /** Return a resource to the pool. If the resource fails the [[test]], it is
    * released, and not added back.
    */
  private def returnResource(resource: R): Unit = {
    Try(test(resource)) match {
      case Success(true) =>
        pool.add(resource)
      case _             =>
        Try(release(resource))
    }
    gate.release()
  }

  /** Fill the pool to its maximum size.
    */
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

  /** Drain the pool of all resources, calling [[release()]] before removing
    * them.
    */
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

  /** Shutdown the pool, draining the pool and preventing new resources from
    * being acquired. the ResourcePool cannot be used after this method is
    * called.
    */
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
