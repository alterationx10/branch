package dev.alteration.branch.macaroni.typeclasses

import scala.annotation.targetName

/** A type-class for combining two values of the same type.
  *
  * Laws:
  *   - Associativity: combine(combine(a, b), c) == combine(a, combine(b, c))
  */
trait Semigroup[T] {

  def combine(a: T, b: T): T

  extension (t: T) {
    @targetName("symbolicCombine")
    def |+|(b: T): T = combine(t, b)
  }
}
