package dev.wishingtree.branch.keanu.actors

trait Actor extends Product with Serializable {
  def onMsg: PartialFunction[Any, Any]
}
