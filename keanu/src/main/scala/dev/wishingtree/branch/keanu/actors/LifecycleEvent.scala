package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleEvent

case object PoisonPill extends LifecycleEvent

private[actors] case object UnexpectedTermination         extends LifecycleEvent
private[actors] case object CancellationTermination       extends LifecycleEvent
private[actors] case object PoisonPillTermination         extends LifecycleEvent
private[actors] case class OnMsgTermination(e: Throwable) extends LifecycleEvent
private[actors] case object InterruptedTermination        extends LifecycleEvent
private[actors] case object InitializationTermination     extends LifecycleEvent
