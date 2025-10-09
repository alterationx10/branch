package dev.alteration.branch.keanu.eventbus

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

/** A trait for creating a subscriber to an event bus.
  *
  * Each subscriber runs on its own virtual thread and processes messages
  * asynchronously from a mailbox queue. Call [[shutdown()]] or [[close()]]
  * to cleanly terminate the subscriber thread.
  *
  * == Error Handling ==
  *
  * Exceptions thrown in [[onMsg]] are caught and passed to [[onError]]. The
  * default [[onError]] is a no-op for resilience, but can be overridden:
  *
  * {{{
  * new Subscriber[Int] {
  *   override def onMsg(msg: EventBusMessage[Int]): Unit = {
  *     if (msg.payload < 0) throw new IllegalArgumentException("Negative!")
  *     println(msg.payload)
  *   }
  *
  *   override def onError(error: Throwable, msg: EventBusMessage[Int]): Unit = {
  *     logger.error(s"Failed to process message: \${msg.payload}", error)
  *   }
  * }
  * }}}
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

  /** Called when an error occurs during message processing.
    *
    * This method is invoked when [[onMsg]] throws an exception. The default
    * implementation is a no-op for maximum resilience, but users can override
    * it to add logging, metrics, alerting, or other error handling.
    *
    * Note: This method is called synchronously in the subscriber's thread, so
    * it should be fast and non-blocking. Avoid heavy operations here.
    *
    * @param error
    *   The exception that occurred
    * @param message
    *   The message that was being processed
    */
  def onError(error: Throwable, message: EventBusMessage[T]): Unit = {
    // Default: no-op for resilience
    // Override to add logging, metrics, etc.
  }

  /** Shuts down the subscriber, stopping the message processing thread.
    * This method is idempotent and can be called multiple times safely.
    */
  def shutdown(): Unit = {
    if (running) {
      running = false
      thread.interrupt() // Wake up if blocked on mailbox.take()
    }
  }

  /** Closes the subscriber (delegates to shutdown for AutoCloseable support).
    */
  override def close(): Unit = shutdown()

}

object Subscriber {

  /** Creates a subscriber from a message handler function.
    *
    * This is equivalent to using SAM conversion, but may be more explicit
    * and discoverable for users unfamiliar with SAM types.
    *
    * @param handler
    *   The function to handle incoming messages
    * @return
    *   A new subscriber instance
    * @example
    *   {{{
    * val subscriber = Subscriber[Int] { msg =>
    *   println(s"Received: \${msg.payload}")
    * }
    * eventBus.subscribe(subscriber)
    *   }}}
    */
  def apply[T](handler: EventBusMessage[T] => Unit): Subscriber[T] =
    new Subscriber[T] {
      override def onMsg(msg: EventBusMessage[T]): Unit = handler(msg)
    }

}
