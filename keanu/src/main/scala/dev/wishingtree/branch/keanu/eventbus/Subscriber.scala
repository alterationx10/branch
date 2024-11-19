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
        Try(onMsg(mailbox.take()))
      }
    })

  def onMsg(msg: EventBusMessage[T]): Unit

}
