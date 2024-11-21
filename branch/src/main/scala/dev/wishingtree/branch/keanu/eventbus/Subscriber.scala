package dev.wishingtree.branch.keanu.eventbus

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.util.*

trait Subscriber[T] {
  private[eventbus] val mailbox: BlockingQueue[EventBusMessage[T]] =
    new LinkedBlockingQueue[EventBusMessage[T]]()

  private val thread: Thread = Thread
    .ofVirtual()
    .start(() => {
      while (true) {
        val msg = mailbox.take()
        Try(onMsg(msg))
      }
    })

  def onMsg(msg: EventBusMessage[T]): Unit

}
