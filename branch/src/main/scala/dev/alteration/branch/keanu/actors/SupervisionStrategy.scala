package dev.alteration.branch.keanu.actors

import scala.concurrent.duration.Duration

/** A strategy for handling actor failures.
  *
  * Supervision strategies determine what happens when an actor throws an
  * exception during message processing.
  */
sealed trait SupervisionStrategy

/** Restart the actor when it fails. This is the default strategy.
  *
  * The actor will be recreated and continue processing messages. The mailbox is
  * preserved.
  */
case object RestartStrategy extends SupervisionStrategy

/** Stop the actor when it fails.
  *
  * The actor will be removed from the system. The mailbox and any pending
  * messages will be discarded.
  */
case object StopStrategy extends SupervisionStrategy

/** Restart the actor with exponential backoff.
  *
  * After each failure, the system waits longer before restarting the actor.
  * This is useful for actors that may be failing due to external resource
  * unavailability.
  *
  * @param minBackoff
  *   Minimum wait time before first restart
  * @param maxBackoff
  *   Maximum wait time between restarts
  * @param maxRetries
  *   Maximum number of retries before stopping the actor. None means unlimited
  *   retries.
  * @param resetAfter
  *   Duration after which the failure count is reset. None means never reset.
  */
case class RestartWithBackoff(
    minBackoff: Duration,
    maxBackoff: Duration,
    maxRetries: Option[Int] = None,
    resetAfter: Option[Duration] = None
) extends SupervisionStrategy {
  require(
    minBackoff.toMillis > 0,
    "minBackoff must be positive"
  )
  require(
    maxBackoff.toMillis >= minBackoff.toMillis,
    "maxBackoff must be >= minBackoff"
  )
  require(
    maxRetries.forall(_ > 0),
    "maxRetries must be positive if specified"
  )
  require(
    resetAfter.forall(_.toMillis > 0),
    "resetAfter must be positive if specified"
  )
}
