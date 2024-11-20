package dev.wishingtree.branch.keanu.actors

trait Actor {
  def onMsg: PartialFunction[Any, Any]
}
