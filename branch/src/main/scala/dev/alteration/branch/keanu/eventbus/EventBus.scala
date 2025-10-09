package dev.alteration.branch.keanu.eventbus

import java.util
import java.util.UUID
import scala.collection.concurrent

/** An event bus for publishing and subscribing to events.
  *
  * Error handling is resilient by design - exceptions in filters or during
  * message delivery do not block the publish operation. Instead, errors are
  * reported via the [[onPublishError]] callback, which can be overridden to
  * integrate with logging frameworks, metrics systems, or custom error
  * handling.
  *
  * @example
  *   {{{
  * // Custom error handling
  * object MyEventBus extends EventBus[String] {
  *   override def onPublishError(
  *     error: Throwable,
  *     message: EventBusMessage[String],
  *     subscriptionId: UUID
  *   ): Unit = {
  *     logger.error(s"Failed to publish to subscription $subscriptionId", error)
  *     metrics.increment("eventbus.publish.errors")
  *   }
  * }
  *   }}}
  */
trait EventBus[T] extends AutoCloseable {

  /** An internal subscription model */
  private case class Subscription(
      id: UUID,
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  )

  /** A map of subscribers */
  private val subscribers: concurrent.Map[UUID, Subscription] =
    concurrent.TrieMap.empty

  /** Called when an error occurs during message publishing.
    *
    * This method is invoked when either a filter predicate throws an exception
    * or when putting a message into a subscriber's mailbox fails. The default
    * implementation is a no-op for maximum resilience, but users can override
    * it to add logging, metrics, alerting, or other error handling.
    *
    * Note: This method is called synchronously during publish, so it should be
    * fast and non-blocking. Avoid heavy operations here.
    *
    * @param error
    *   The exception that occurred
    * @param message
    *   The message that was being published
    * @param subscriptionId
    *   The UUID of the subscription that failed
    */
  def onPublishError(
      error: Throwable,
      message: EventBusMessage[T],
      subscriptionId: UUID
  ): Unit = {
    // Default: no-op for resilience
    // Override to add logging, metrics, etc.
  }

  /** Publishes a message to all subscribers.
    *
    * This method is resilient - errors in filter predicates or message delivery
    * do not stop publication to other subscribers. Errors are reported via
    * [[onPublishError]].
    */
  final def publish(msg: EventBusMessage[T]): Unit =
    subscribers.foreach { (id, sub) =>
      try {
        if sub.filter(msg) then sub.subscriber.mailbox.put(msg)
      } catch {
        case error: Exception =>
          onPublishError(error, msg, id)
      }
    }

  /** Publishes a message to all subscribers.
    */
  def publish(topic: String, payload: T): Unit = {
    val msg = EventBusMessage[T](topic = topic, payload = payload)
    publish(msg)
  }

  /** Publishes a message to all subscribers. Defaults to an empty String topic.
    */
  final def publishNoTopic(payload: T): Unit =
    publish("", payload)

  /** Subscribes a subscriber to the event bus.
    */
  final def subscribe(subscriber: Subscriber[T]): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, _ => true)
    subscribers += sub.id -> sub
    sub.id
  }

  /** Subscribes a subscriber to the event bus with a filter.
    */
  final def subscribe(
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  ): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, filter)
    subscribers += sub.id -> sub
    sub.id

  }

  /** Unsubscribes a subscriber from the event bus.
    */
  def unsubscribe(subscriber: Subscriber[T]): Unit =
    subscribers.find(_._2.subscriber == subscriber).foreach { case (id, sub) =>
      sub.subscriber.shutdown()
      subscribers -= id
    }

  /** Unsubscribes a subscriber from the event bus by its id.
    */
  def unsubscribe(ids: UUID*): Unit =
    ids.foreach { id =>
      subscribers.get(id).foreach(_.subscriber.shutdown())
      subscribers -= id
    }

  /** Shuts down all subscribers and clears the event bus.
    * This method is idempotent and can be called multiple times safely.
    */
  def shutdown(): Unit = {
    subscribers.values.foreach(_.subscriber.shutdown())
    subscribers.clear()
  }

  /** Closes the event bus (delegates to shutdown for AutoCloseable support).
    */
  override def close(): Unit = shutdown()

}
