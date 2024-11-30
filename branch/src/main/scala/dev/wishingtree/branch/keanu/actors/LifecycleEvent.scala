package dev.wishingtree.branch.keanu.actors

/** A sealed trait typically used to indicate the termination result of an
  * actor.
  */
sealed trait LifecycleEvent

/** A message that causes an actor to terminate when sent. */
case object PoisonPill extends LifecycleEvent

private[actors] case object UnexpectedTermination         extends LifecycleEvent
private[actors] case object CancellationTermination       extends LifecycleEvent
private[actors] case object PoisonPillTermination         extends LifecycleEvent
private[actors] case class OnMsgTermination(e: Throwable) extends LifecycleEvent
private[actors] case object InterruptedTermination        extends LifecycleEvent
private[actors] case object InitializationTermination     extends LifecycleEvent
