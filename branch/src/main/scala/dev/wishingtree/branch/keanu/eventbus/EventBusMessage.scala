package dev.wishingtree.branch.keanu.eventbus

/** A message model for the event bus.
  */
case class EventBusMessage[T](topic: String, payload: T)
