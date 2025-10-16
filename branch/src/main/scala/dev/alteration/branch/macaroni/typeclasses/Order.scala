package dev.alteration.branch.macaroni.typeclasses

import scala.annotation.targetName

/** A type-class for type-safe ordering comparison.
  *
  * Extends Eq with ordering capabilities.
  *
  * Laws:
  *   - Totality: compare(a, b) is defined for all a and b
  *   - Antisymmetry: if compare(a, b) <= 0 && compare(b, a) <= 0 then eqv(a, b)
  *   - Transitivity: if compare(a, b) <= 0 && compare(b, c) <= 0 then
  *     compare(a, c) <= 0
  */
trait Order[T] extends Eq[T] {

  /** Returns a negative Int if a < b, 0 if a == b, positive if a > b */
  def compare(a: T, b: T): Int

  override def eqv(a: T, b: T): Boolean = compare(a, b) == 0

  def lt(a: T, b: T): Boolean    = compare(a, b) < 0
  def lteqv(a: T, b: T): Boolean = compare(a, b) <= 0
  def gt(a: T, b: T): Boolean    = compare(a, b) > 0
  def gteqv(a: T, b: T): Boolean = compare(a, b) >= 0

  def min(a: T, b: T): T = if (compare(a, b) <= 0) a else b
  def max(a: T, b: T): T = if (compare(a, b) >= 0) a else b

  extension (t: T) {
    @targetName("symbolicLt")
    def <(b: T): Boolean = lt(t, b)

    @targetName("symbolicLteqv")
    def <=(b: T): Boolean = lteqv(t, b)

    @targetName("symbolicGt")
    def >(b: T): Boolean = gt(t, b)

    @targetName("symbolicGteqv")
    def >=(b: T): Boolean = gteqv(t, b)
  }

}

object Order {

  def apply[T](using o: Order[T]): Order[T] = o

  def fromOrdering[T](using ord: Ordering[T]): Order[T] = new Order[T] {
    def compare(a: T, b: T): Int = ord.compare(a, b)
  }

  given Order[String] with {
    def compare(a: String, b: String): Int = a.compareTo(b)
  }

  given Order[Int] with {
    def compare(a: Int, b: Int): Int = Integer.compare(a, b)
  }

  given Order[Long] with {
    def compare(a: Long, b: Long): Int = java.lang.Long.compare(a, b)
  }

  given Order[Double] with {
    def compare(a: Double, b: Double): Int = java.lang.Double.compare(a, b)
  }

  given Order[Boolean] with {
    def compare(a: Boolean, b: Boolean): Int = java.lang.Boolean.compare(a, b)
  }

  given [T](using o: Order[T]): Order[List[T]] with {
    def compare(a: List[T], b: List[T]): Int = {
      (a, b) match {
        case (Nil, Nil)         => 0
        case (Nil, _)           => -1
        case (_, Nil)           => 1
        case (x :: xs, y :: ys) =>
          val cmp = o.compare(x, y)
          if (cmp != 0) cmp else compare(xs, ys)
      }
    }
  }

  given [T](using o: Order[T]): Order[Option[T]] with {
    def compare(a: Option[T], b: Option[T]): Int = (a, b) match {
      case (None, None)       => 0
      case (None, Some(_))    => -1
      case (Some(_), None)    => 1
      case (Some(x), Some(y)) => o.compare(x, y)
    }
  }

}
