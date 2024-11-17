package dev.wishingtree.branch.keanu.actors

sealed trait LifecycleAction
case object Restart extends LifecycleAction
case object Terminate extends LifecycleAction
