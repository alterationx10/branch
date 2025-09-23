package dev.alteration.branch.lzy.abstractions

import scala.annotation.targetName

/** A type-class for combining two values of the same type.
  */
trait Semigroup[T] {

  def combine(a: T, b: T): T

  extension (t: T) {
    @targetName("symbolicCombine")
    def |+|(b: T): T = combine(t, b)
  }
}
