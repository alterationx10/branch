package dev.alteration.branch.macaroni.typeclasses

/** A type-class for combining values with an identity element.
  *
  * Laws:
  *   - Left identity: combine(empty, a) == a
  *   - Right identity: combine(a, empty) == a
  *   - Associativity (from Semigroup): combine(combine(a, b), c) == combine(a,
  *     combine(b, c))
  */
trait Monoid[T] extends Semigroup[T] {

  def empty: T

}

object Monoid {

  def apply[T](using m: Monoid[T]): Monoid[T] = m

  given Monoid[String] with {
    def empty: String                         = ""
    def combine(a: String, b: String): String = a + b
  }

  given Monoid[Int] with {
    def empty: Int                   = 0
    def combine(a: Int, b: Int): Int = a + b
  }

  given Monoid[Long] with {
    def empty: Long                     = 0L
    def combine(a: Long, b: Long): Long = a + b
  }

  given [T]: Monoid[List[T]] with {
    def empty: List[T]                           = List.empty
    def combine(a: List[T], b: List[T]): List[T] = a ++ b
  }

  given [T]: Monoid[Vector[T]] with {
    def empty: Vector[T]                               = Vector.empty
    def combine(a: Vector[T], b: Vector[T]): Vector[T] = a ++ b
  }

  given [T]: Monoid[Set[T]] with {
    def empty: Set[T]                         = Set.empty
    def combine(a: Set[T], b: Set[T]): Set[T] = a ++ b
  }

  given [K, V]: Monoid[Map[K, V]] with {
    def empty: Map[K, V]                               = Map.empty
    def combine(a: Map[K, V], b: Map[K, V]): Map[K, V] = a ++ b
  }

  given [T]: Monoid[Option[T]] with {
    def empty: Option[T]                               = None
    def combine(a: Option[T], b: Option[T]): Option[T] = a.orElse(b)
  }

}
