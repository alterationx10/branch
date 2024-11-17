package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleException extends Throwable

case object PoisonPillException extends LifecycleException
