package dev.alteration.branch.macaroni.typeclasses

import scala.annotation.targetName

/** A type-class for type-safe equality comparison.
  *
  * Laws:
  *   - Reflexivity: eqv(a, a) == true
  *   - Symmetry: eqv(a, b) == eqv(b, a)
  *   - Transitivity: if eqv(a, b) && eqv(b, c) then eqv(a, c)
  */
trait Eq[T] {

  def eqv(a: T, b: T): Boolean

  def neqv(a: T, b: T): Boolean = !eqv(a, b)

  extension (t: T) {
    @targetName("symbolicEqv")
    def ===(b: T): Boolean = eqv(t, b)

    @targetName("symbolicNeqv")
    def =!=(b: T): Boolean = neqv(t, b)
  }

}

object Eq {

  def apply[T](using e: Eq[T]): Eq[T] = e

  def fromEquals[T]: Eq[T] = new Eq[T] {
    def eqv(a: T, b: T): Boolean = a == b
  }

  given Eq[String] with {
    def eqv(a: String, b: String): Boolean = a == b
  }

  given Eq[Int] with {
    def eqv(a: Int, b: Int): Boolean = a == b
  }

  given Eq[Long] with {
    def eqv(a: Long, b: Long): Boolean = a == b
  }

  given Eq[Double] with {
    def eqv(a: Double, b: Double): Boolean = a == b
  }

  given Eq[Boolean] with {
    def eqv(a: Boolean, b: Boolean): Boolean = a == b
  }

  given [T](using e: Eq[T]): Eq[List[T]] with {
    def eqv(a: List[T], b: List[T]): Boolean = {
      a.length == b.length && a.zip(b).forall { case (x, y) => e.eqv(x, y) }
    }
  }

  given [T](using e: Eq[T]): Eq[Option[T]] with {
    def eqv(a: Option[T], b: Option[T]): Boolean = (a, b) match {
      case (Some(x), Some(y)) => e.eqv(x, y)
      case (None, None)       => true
      case _                  => false
    }
  }

  given [L, R](using el: Eq[L], er: Eq[R]): Eq[Either[L, R]] with {
    def eqv(a: Either[L, R], b: Either[L, R]): Boolean = (a, b) match {
      case (Left(x), Left(y))   => el.eqv(x, y)
      case (Right(x), Right(y)) => er.eqv(x, y)
      case _                    => false
    }
  }

}
