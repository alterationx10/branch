package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleException extends Throwable

private[actors] case object PoisonPillException extends LifecycleException
private[actors] case class OnMsgException(e: Throwable)
    extends LifecycleException
private[actors] case class InstantiationException(e: Throwable)
    extends LifecycleException

case object ShutdownException extends LifecycleException {
  override def getMessage: String = "ActorSystem is shutting down"
}
