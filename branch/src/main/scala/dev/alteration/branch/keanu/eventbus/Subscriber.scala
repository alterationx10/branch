package dev.alteration.branch.keanu.eventbus

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

/** A subscriber to an event bus.
  *
  * Each subscriber runs on its own virtual thread and processes messages
  * asynchronously. Exceptions thrown in [[onMsg]] are caught and passed to
  * [[onError]] (which defaults to a no-op). Call [[shutdown()]] or [[close()]]
  * to terminate the subscriber thread.
  */
trait Subscriber[T] extends AutoCloseable {

  /** The mailbox for the subscriber.
    */
  private[eventbus] val mailbox: BlockingQueue[EventBusMessage[T]] =
    new LinkedBlockingQueue[EventBusMessage[T]]()

  /** Flag to control the message processing loop.
    */
  @volatile private var running: Boolean = true

  /** The thread for the subscriber.
    */
  private val thread: Thread = Thread
    .ofVirtual()
    .start(() => {
      while (running) {
        try {
          val msg = mailbox.take()
          if (running) { // Check again after blocking wait
            try {
              onMsg(msg)
            } catch {
              case error: Exception =>
                onError(error, msg)
            }
          }
        } catch {
          case _: InterruptedException =>
            // Thread interrupted during shutdown, exit gracefully
            ()
        }
      }
    })

  /** The method called when a message is received.
    *
    * Exceptions thrown by this method will be caught and passed to
    * [[onError]]. The subscriber will continue processing subsequent messages.
    */
  def onMsg(msg: EventBusMessage[T]): Unit

  /** Called when [[onMsg]] throws an exception.
    *
    * Override to add logging, metrics, or other error handling. Default is a
    * no-op. Keep this method fast and non-blocking.
    */
  def onError(error: Throwable, message: EventBusMessage[T]): Unit = ()

  /** Stops the message processing thread. Idempotent.
    */
  def shutdown(): Unit = {
    if (running) {
      running = false
      thread.interrupt() // Wake up if blocked on mailbox.take()
    }
  }

  /** Delegates to [[shutdown()]].
    */
  override def close(): Unit = shutdown()

}

object Subscriber {

  /** Creates a subscriber from a message handler function.
    */
  def apply[T](handler: EventBusMessage[T] => Unit): Subscriber[T] =
    new Subscriber[T] {
      override def onMsg(msg: EventBusMessage[T]): Unit = handler(msg)
    }

}
