package dev.wishingtree.branch.keanu.actors

/** An actor trait which defines the onMsg method for handling messages.
  */
trait Actor extends Product with Serializable {

  /** The method to handle messages sent to the actor.
    */
  def onMsg: PartialFunction[Any, Any]
}
