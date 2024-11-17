package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleEvent

case object PoisonPill extends LifecycleEvent
case class OnMsgTermination(refId: (String, String), e: Throwable) extends LifecycleEvent
case class InterruptedTermination(refId: (String, String)) extends LifecycleEvent

case object PoisonPillException extends Throwable
