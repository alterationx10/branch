package dev.alteration.branch.keanu.actors

import java.time.Instant

/** Represents a message that could not be delivered or processed.
  *
  * @param message
  *   The original message
  * @param recipient
  *   The actor that should have received the message
  * @param timestamp
  *   When the message was dead-lettered
  * @param reason
  *   Why the message was dead-lettered
  */
case class DeadLetter(
    message: Any,
    recipient: ActorRefId,
    timestamp: Instant,
    reason: DeadLetterReason
)

/** Reasons why a message ended up in the dead letter queue.
  */
sealed trait DeadLetterReason

/** The actor's onMsg PartialFunction was not defined for this message.
  */
case object UnhandledMessage extends DeadLetterReason

/** The actor does not exist and cannot be created.
  */
case object ActorNotFound extends DeadLetterReason

/** The mailbox is full and cannot accept more messages.
  */
case object MailboxFull extends DeadLetterReason
