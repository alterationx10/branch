package dev.wishingtree.branch.keanu.eventbus

trait Subscriber[T] {
  def onMessage(msg: T): Unit
}
