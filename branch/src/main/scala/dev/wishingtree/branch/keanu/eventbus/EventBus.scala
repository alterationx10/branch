package dev.wishingtree.branch.keanu.eventbus

import java.util
import java.util.UUID
import scala.collection.concurrent

trait EventBus[T] {

  private case class Subscription(
      id: UUID,
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  )

  private val subscribers: concurrent.Map[UUID, Subscription] =
    concurrent.TrieMap.empty

  def publish(msg: EventBusMessage[T]): Unit =
    subscribers.foreach { (id, sub) =>
      if sub.filter(msg) then sub.subscriber.mailbox.put(msg)
    }

  def publish(topic: String, payload: T): Unit = {
    val msg = EventBusMessage[T](topic = topic, payload = payload)
    publish(msg)
  }

  def publishNoTopic(payload: T): Unit =
    publish("", payload)

  def subscribe(subscriber: Subscriber[T]): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, _ => true)
    subscribers += sub.id -> sub
    sub.id
  }

  def subscribe(
      subscriber: Subscriber[T],
      filter: EventBusMessage[T] => Boolean
  ): UUID = {
    val sub = Subscription(UUID.randomUUID(), subscriber, filter)
    subscribers += sub.id -> sub
    sub.id

  }

  def unsubscribe(subscriber: Subscriber[T]): Unit =
    subscribers.find(_._2.subscriber == subscriber).foreach { case (id, _) =>
      subscribers -= id
    }

  def unsubscribe(ids: UUID*): Unit =
    ids.foreach(subscribers -= _)

  def drainAwait: Unit =
    subscribers.foreach { (id, sub) =>
      while (sub.subscriber.mailbox.size() > 0) {
        // wait for all messages to be processed
      }
    }

}
