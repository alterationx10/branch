package dev.alteration.branch.macaroni.typeclasses

/** A type-class for sequential composition of computations.
  *
  * Laws:
  *   - Left identity: flatMap(pure(a))(f) == f(a)
  *   - Right identity: flatMap(fa)(pure) == fa
  *   - Associativity: flatMap(flatMap(fa)(f))(g) == flatMap(fa)(a =>
  *     flatMap(f(a))(g))
  */
trait Monad[F[_]] extends Applicative[F] {

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  // Implement map in terms of flatMap
  override def map[A, B](fa: F[A])(f: A => B): F[B] =
    flatMap(fa)(a => pure(f(a)))

  def flatten[A](ffa: F[F[A]]): F[A] =
    flatMap(ffa)((fa: F[A]) => fa)

}

object Monad {

  def apply[F[_]](using m: Monad[F]): Monad[F] = m

  given Monad[Option] with {
    def pure[A](a: A): Option[A]                                   = Some(a)
    def ap[A, B](ff: Option[A => B])(fa: Option[A]): Option[B]     =
      (ff, fa) match {
        case (Some(f), Some(a)) => Some(f(a))
        case _                  => None
      }
    def flatMap[A, B](fa: Option[A])(f: A => Option[B]): Option[B] =
      fa.flatMap(f)
  }

  given Monad[List] with {
    def pure[A](a: A): List[A]                               = List(a)
    def ap[A, B](ff: List[A => B])(fa: List[A]): List[B]     =
      for {
        f <- ff
        a <- fa
      } yield f(a)
    def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] = fa.flatMap(f)
  }

  given Monad[Vector] with {
    def pure[A](a: A): Vector[A]                                   = Vector(a)
    def ap[A, B](ff: Vector[A => B])(fa: Vector[A]): Vector[B]     =
      for {
        f <- ff
        a <- fa
      } yield f(a)
    def flatMap[A, B](fa: Vector[A])(f: A => Vector[B]): Vector[B] =
      fa.flatMap(f)
  }

  given [E]: Monad[[T] =>> Either[E, T]] with {
    def pure[A](a: A): Either[E, A]                                         = Right(a)
    def ap[A, B](ff: Either[E, A => B])(fa: Either[E, A]): Either[E, B]     =
      (ff, fa) match {
        case (Right(f), Right(a)) => Right(f(a))
        case (Left(e), _)         => Left(e)
        case (_, Left(e))         => Left(e)
      }
    def flatMap[A, B](fa: Either[E, A])(f: A => Either[E, B]): Either[E, B] =
      fa.flatMap(f)
  }

}
