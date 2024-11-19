package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleEvent

case object PoisonPill extends LifecycleEvent

private [actors] case class PoisonPillTermination(refId: ActorRefId) extends LifecycleEvent
private [actors] case class OnMsgTermination(refId: ActorRefId, e: Throwable) extends LifecycleEvent
private [actors] case class InterruptedTermination(refId: ActorRefId) extends LifecycleEvent
private [actors] case class InitializationTermination(refId: ActorRefId) extends LifecycleEvent