package dev.alteration.branch.macaroni.typeclasses

import scala.annotation.targetName

/** A type-class for combining two values of the same type constructor.
  *
  * This is like Semigroup but operates at the type constructor level (kind * ->
  * *).
  *
  * Laws:
  *   - Associativity: combineK(combineK(fa, fb), fc) == combineK(fa,
  *     combineK(fb, fc))
  */
trait SemigroupK[F[_]] {

  def combineK[A](fa: F[A], fb: F[A]): F[A]

  extension [A](fa: F[A]) {
    @targetName("symbolicCombineK")
    def <+>(fb: F[A]): F[A] = combineK(fa, fb)
  }

}

object SemigroupK {

  def apply[F[_]](using s: SemigroupK[F]): SemigroupK[F] = s

  given SemigroupK[List] with {
    def combineK[A](fa: List[A], fb: List[A]): List[A] = fa ++ fb
  }

  given SemigroupK[Vector] with {
    def combineK[A](fa: Vector[A], fb: Vector[A]): Vector[A] = fa ++ fb
  }

  given SemigroupK[Option] with {
    def combineK[A](fa: Option[A], fb: Option[A]): Option[A] = fa.orElse(fb)
  }

  given [E]: SemigroupK[[T] =>> Either[E, T]] with {
    def combineK[A](fa: Either[E, A], fb: Either[E, A]): Either[E, A] =
      fa match {
        case Right(a) => Right(a)
        case Left(_)  => fb
      }
  }

}
