package dev.wishingtree.branch.keanu.eventbus

import java.util
import java.util.UUID
import scala.collection.concurrent

/** An event bus for publishing and subscribing to events.
  */
trait EventBus[T] {

  /** An internal subscription model */
  private case class Subscription(
      id: UUID,
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  )

  /** A map of subscribers */
  private val subscribers: concurrent.Map[UUID, Subscription] =
    concurrent.TrieMap.empty

  /** Publishes a message to all subscribers.
    */
  final def publish(msg: EventBusMessage[T]): Unit =
    subscribers.foreach { (_, sub) =>
      try {
        if sub.filter(msg) then sub.subscriber.mailbox.put(msg)
      } catch {
        case e: Exception =>
          // Could add logging here
          ()
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
    subscribers.find(_._2.subscriber == subscriber).foreach { case (id, _) =>
      subscribers -= id
    }

  /** Unsubscribes a subscriber from the event bus by its id.
    */
  def unsubscribe(ids: UUID*): Unit =
    ids.foreach(subscribers -= _)

}
