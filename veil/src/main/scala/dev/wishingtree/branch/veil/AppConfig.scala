package dev.wishingtree.branch.veil

trait AppConfig[T] {
  val instance: T
}