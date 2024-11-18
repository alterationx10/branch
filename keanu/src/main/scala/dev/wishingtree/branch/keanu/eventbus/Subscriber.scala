package dev.wishingtree.branch.keanu.eventbus

trait Subscriber[T] {
  def onMsg(msg: EventBusMessage[T]): Unit
}
