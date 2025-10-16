package dev.alteration.branch.macaroni.typeclasses

/** A type-class for applicative functors - functors with application.
  *
  * Laws:
  *   - Identity: ap(pure(identity))(fa) == fa
  *   - Homomorphism: ap(pure(f))(pure(a)) == pure(f(a))
  *   - Interchange: ap(ff)(pure(a)) == ap(pure(f => f(a)))(ff)
  *   - Composition: ap(ap(ap(pure(compose))(ff))(fg))(fa) == ap(ff)(ap(fg)(fa))
  *   - Map: map(fa)(f) == ap(pure(f))(fa)
  */
trait Applicative[F[_]] extends Functor[F] {

  def pure[A](a: A): F[A]

  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  // Implement map in terms of ap and pure
  override def map[A, B](fa: F[A])(f: A => B): F[B] =
    ap(pure(f))(fa)

  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    ap(map(fa)(a => (b: B) => f(a, b)))(fb)

  def map3[A, B, C, D](fa: F[A], fb: F[B], fc: F[C])(f: (A, B, C) => D): F[D] =
    ap(ap(map(fa)(a => (b: B) => (c: C) => f(a, b, c)))(fb))(fc)

}

object Applicative {

  def apply[F[_]](using a: Applicative[F]): Applicative[F] = a

  given Applicative[Option] with {
    def pure[A](a: A): Option[A]                               = Some(a)
    def ap[A, B](ff: Option[A => B])(fa: Option[A]): Option[B] =
      (ff, fa) match {
        case (Some(f), Some(a)) => Some(f(a))
        case _                  => None
      }
  }

  given Applicative[List] with {
    def pure[A](a: A): List[A]                           = List(a)
    def ap[A, B](ff: List[A => B])(fa: List[A]): List[B] =
      for {
        f <- ff
        a <- fa
      } yield f(a)
  }

  given Applicative[Vector] with {
    def pure[A](a: A): Vector[A]                               = Vector(a)
    def ap[A, B](ff: Vector[A => B])(fa: Vector[A]): Vector[B] =
      for {
        f <- ff
        a <- fa
      } yield f(a)
  }

  given [E]: Applicative[[T] =>> Either[E, T]] with {
    def pure[A](a: A): Either[E, A]                                     = Right(a)
    def ap[A, B](ff: Either[E, A => B])(fa: Either[E, A]): Either[E, B] =
      (ff, fa) match {
        case (Right(f), Right(a)) => Right(f(a))
        case (Left(e), _)         => Left(e)
        case (_, Left(e))         => Left(e)
      }
  }

}
