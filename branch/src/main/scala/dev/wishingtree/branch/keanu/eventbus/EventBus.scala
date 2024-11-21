package dev.wishingtree.branch.keanu.eventbus

import java.util
import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait EventBus[T] {

  private case class Subscription(
      id: UUID,
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  )

  private val subscribers: mutable.ArrayBuffer[Subscription] =
    ArrayBuffer.empty

  def publish(msg: EventBusMessage[T]): Unit = synchronized {
    subscribers.foreach { sub =>
      if sub.filter(msg) then sub.subscriber.mailbox.put(msg)
    }
  }

  def publish(topic: String, payload: T): Unit = {
    val msg = EventBusMessage[T](topic = topic, payload = payload)
    publish(msg)
  }

  def publishNoTopic(payload: T): Unit =
    publish("", payload)

  def subscribe(subscriber: Subscriber[T]): UUID =
    synchronized {
      val sub = Subscription(UUID.randomUUID(), subscriber, _ => true)
      subscribers.addOne(sub)
      sub.id
    }

  def subscribe(
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  ): UUID = {
    synchronized {
      val sub = Subscription(UUID.randomUUID(), subscriber, filter)
      subscribers.addOne(sub)
      sub.id
    }
  }

  def unsubscribe(subscriber: Subscriber[T]): Unit =
    synchronized {
      subscribers.filterInPlace(sub => sub.subscriber != subscriber)
    }

  def unsubscribe(ids: UUID*): Unit =
    synchronized {
      subscribers.filterInPlace(sub => !ids.contains(sub.id))
    }
}
