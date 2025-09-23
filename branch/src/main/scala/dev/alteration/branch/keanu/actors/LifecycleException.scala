package dev.alteration.branch.keanu.actors

/** A sealed trait for exceptions that can occur during the lifecycle of an
  * actor. Internally used for flow control.
  */
private[actors] sealed trait LifecycleException extends Throwable

private[actors] case object PoisonPillException extends LifecycleException
private[actors] case class OnMsgException(e: Throwable)
    extends LifecycleException
private[actors] case class InstantiationException(e: Throwable)
    extends LifecycleException
