package dev.alteration.branch.macaroni.typeclasses

/** A type-class for combining values with an identity element at the type
  * constructor level.
  *
  * This is like Monoid but operates at the type constructor level (kind * ->
  * *).
  *
  * Laws:
  *   - Left identity: combineK(emptyK, fa) == fa
  *   - Right identity: combineK(fa, emptyK) == fa
  *   - Associativity (from SemigroupK): combineK(combineK(fa, fb), fc) ==
  *     combineK(fa, combineK(fb, fc))
  */
trait MonoidK[F[_]] extends SemigroupK[F] {

  def emptyK[A]: F[A]

}

object MonoidK {

  def apply[F[_]](using m: MonoidK[F]): MonoidK[F] = m

  given MonoidK[List] with {
    def emptyK[A]: List[A]                             = List.empty
    def combineK[A](fa: List[A], fb: List[A]): List[A] = fa ++ fb
  }

  given MonoidK[Vector] with {
    def emptyK[A]: Vector[A]                                 = Vector.empty
    def combineK[A](fa: Vector[A], fb: Vector[A]): Vector[A] = fa ++ fb
  }

  given MonoidK[Option] with {
    def emptyK[A]: Option[A]                                 = None
    def combineK[A](fa: Option[A], fb: Option[A]): Option[A] = fa.orElse(fb)
  }

}
