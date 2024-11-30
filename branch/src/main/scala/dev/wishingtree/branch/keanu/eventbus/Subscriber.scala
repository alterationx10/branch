package dev.wishingtree.branch.keanu.eventbus

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.util.*

/** A trait for creating a subscriber to an event bus.
  */
trait Subscriber[T] {

  /** The mailbox for the subscriber.
    */
  private[eventbus] val mailbox: BlockingQueue[EventBusMessage[T]] =
    new LinkedBlockingQueue[EventBusMessage[T]]()

  /** The thread for the subscriber.
    */
  private val thread: Thread = Thread
    .ofVirtual()
    .start(() => {
      while (true) {
        val msg = mailbox.take()
        Try(onMsg(msg))
      }
    })

  /** The method called when a message is received. Internally, it is wrapped in
    * a Try, so no exceptions should be thrown.
    */
  def onMsg(msg: EventBusMessage[T]): Unit

}
