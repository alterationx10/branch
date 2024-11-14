package dev.wishingtree.branch.keanu.eventbus

case class EventMessage[T](topic: String, payload: T)
