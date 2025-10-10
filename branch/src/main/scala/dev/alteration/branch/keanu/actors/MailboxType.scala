package dev.alteration.branch.keanu.actors

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import scala.math.Ordering

/** A factory for creating mailbox queues for actors.
  *
  * Different mailbox types provide different characteristics for message
  * delivery and processing.
  */
sealed trait MailboxType {

  /** Create a new mailbox queue for an actor.
    *
    * @return
    *   A BlockingQueue that will be used as the actor's mailbox
    */
  def createMailbox(): BlockingQueue[Any]
}

/** An unbounded mailbox with no capacity limit.
  *
  * This is the default mailbox type. Messages are processed in FIFO order.
  * Warning: Can lead to memory issues if messages arrive faster than they can
  * be processed.
  */
case object UnboundedMailbox extends MailboxType {
  override def createMailbox(): BlockingQueue[Any] =
    new LinkedBlockingQueue[Any]()
}

/** A bounded mailbox with a maximum capacity.
  *
  * When the mailbox is full, the overflow strategy determines what happens to
  * new messages.
  *
  * @param capacity
  *   Maximum number of messages the mailbox can hold
  * @param overflowStrategy
  *   Strategy for handling messages when mailbox is full
  */
case class BoundedMailbox(
    capacity: Int,
    overflowStrategy: OverflowStrategy = DropOldest
) extends MailboxType {
  require(capacity > 0, "Mailbox capacity must be positive")

  override def createMailbox(): BlockingQueue[Any] =
    new BoundedMailboxQueue(capacity, overflowStrategy)
}

/** A mailbox that processes messages according to a priority ordering.
  *
  * Messages are processed in priority order rather than FIFO order. This is
  * useful for actors that need to handle urgent messages before normal ones.
  *
  * @param ordering
  *   The ordering to use for prioritizing messages
  */
case class PriorityMailbox(ordering: Ordering[Any]) extends MailboxType {
  override def createMailbox(): BlockingQueue[Any] =
    new PriorityBlockingQueue[Any](11, ordering)
}

/** Strategy for handling messages when a bounded mailbox is full.
  */
sealed trait OverflowStrategy

/** Drop the oldest message in the mailbox to make room for the new one.
  */
case object DropOldest extends OverflowStrategy

/** Drop the newest message (the one being added).
  */
case object DropNewest extends OverflowStrategy

/** Fail with an exception when the mailbox is full.
  */
case object Fail extends OverflowStrategy

/** A custom BlockingQueue implementation for bounded mailboxes with overflow
  * strategies.
  *
  * @param capacity
  *   Maximum number of messages
  * @param overflowStrategy
  *   Strategy for handling overflow
  */
private class BoundedMailboxQueue(
    capacity: Int,
    overflowStrategy: OverflowStrategy
) extends LinkedBlockingQueue[Any](capacity) {

  override def put(e: Any): Unit = {
    if (!offer(e)) {
      overflowStrategy match {
        case DropOldest =>
          poll()       // Remove oldest
          super.put(e) // Add new (should always succeed now)

        case DropNewest =>
          () // Do nothing, just drop the new message

        case Fail =>
          throw new MailboxOverflowException(
            s"Mailbox full (capacity: $capacity)"
          )
      }
    }
  }

  override def offer(e: Any): Boolean = {
    val result = super.offer(e)
    if (!result) {
      overflowStrategy match {
        case DropOldest =>
          poll()         // Remove oldest
          super.offer(e) // Try again

        case DropNewest =>
          false // Drop the new message

        case Fail =>
          throw new MailboxOverflowException(
            s"Mailbox full (capacity: $capacity)"
          )
      }
    } else {
      result
    }
  }
}

/** Exception thrown when a bounded mailbox with Fail overflow strategy is full.
  */
class MailboxOverflowException(message: String)
    extends RuntimeException(message)
