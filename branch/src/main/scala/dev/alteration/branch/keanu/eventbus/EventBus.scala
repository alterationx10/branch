package dev.alteration.branch.keanu.eventbus

import java.util
import java.util.UUID
import scala.collection.concurrent

/** An event bus for publishing and subscribing to events.
  *
  * Exceptions in filters or message delivery don't block publishing. Override
  * [[onPublishError]] to add logging or metrics.
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

  /** Called when a filter or mailbox.put() throws an exception.
    *
    * Override to add logging or metrics. Default is a no-op. Keep this method
    * fast and non-blocking.
    */
  def onPublishError(
      error: Throwable,
      message: EventBusMessage[T],
      subscriptionId: UUID
  ): Unit = ()

  /** Publishes a message to all subscribers.
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

  /** Publishes a message with a topic.
    */
  final def publish(topic: String, payload: T): Unit = {
    val msg = EventBusMessage[T](topic = topic, payload = payload)
    publish(msg)
  }

  /** Publishes a message with an empty topic.
    */
  final def publishNoTopic(payload: T): Unit =
    publish("", payload)

  /** Subscribes a subscriber. New subscribers may miss in-flight messages.
    */
  final def subscribe(subscriber: Subscriber[T]): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, _ => true)
    subscribers += sub.id -> sub
    sub.id
  }

  /** Subscribes a subscriber with a filter. New subscribers may miss in-flight
    * messages.
    */
  final def subscribe(
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  ): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, filter)
    subscribers += sub.id -> sub
    sub.id

  }

  /** Unsubscribes a subscriber. Shuts down its thread.
    */
  def unsubscribe(subscriber: Subscriber[T]): Unit =
    subscribers.find(_._2.subscriber == subscriber).foreach { case (id, sub) =>
      sub.subscriber.shutdown()
      subscribers -= id
    }

  /** Unsubscribes by subscription ID. Shuts down the subscriber's thread.
    */
  def unsubscribe(ids: UUID*): Unit =
    ids.foreach { id =>
      subscribers.get(id).foreach(_.subscriber.shutdown())
      subscribers -= id
    }

  /** Shuts down all subscribers and clears the event bus. Idempotent.
    */
  def shutdown(): Unit = {
    subscribers.values.foreach(_.subscriber.shutdown())
    subscribers.clear()
  }

  /** Delegates to [[shutdown()]].
    */
  override def close(): Unit = shutdown()

}
