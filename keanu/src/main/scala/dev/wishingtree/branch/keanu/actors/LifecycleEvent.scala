package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleEvent

case object PoisonPill extends LifecycleEvent
case object PoisonPillException extends Throwable
