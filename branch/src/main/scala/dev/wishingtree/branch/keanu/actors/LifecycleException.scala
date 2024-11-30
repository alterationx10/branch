package dev.wishingtree.branch.keanu.actors

private[actors] sealed trait LifecycleException extends Throwable

private[actors] case object PoisonPillException extends LifecycleException
private[actors] case class OnMsgException(e: Throwable)
    extends LifecycleException
private[actors] case class InstantiationException(e: Throwable)
    extends LifecycleException
