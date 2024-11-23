package dev.wishingtree.branch.keanu.eventbus

case class EventBusMessage[T](topic: String, payload: T)
