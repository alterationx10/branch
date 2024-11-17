package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleEvent

case object PoisonPill extends LifecycleEvent
case class PoisonPillTermination(refId: ActorRefId) extends LifecycleEvent
case class OnMsgTermination(refId: ActorRefId, e: Throwable) extends LifecycleEvent
case class InterruptedTermination(refId: ActorRefId) extends LifecycleEvent
