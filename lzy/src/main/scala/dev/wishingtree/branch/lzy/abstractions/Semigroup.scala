package dev.wishingtree.branch.lzy.abstractions

import scala.annotation.targetName


trait Semigroup[T] {

  def combine(a: T, b: T): T

  extension (t: T) {
    @targetName("symbolicCombine")
    def |+|(b: T): T = combine(t, b)
  }
}
