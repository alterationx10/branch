package dev.alteration.branch.macaroni.typeclasses

/** A type-class for functors with two type parameters.
  *
  * Laws:
  *   - Identity: bimap(fab)(identity, identity) == fab
  *   - Composition: bimap(fab)(f1.andThen(f2), g1.andThen(g2)) ==
  *     bimap(bimap(fab)(f1, g1))(f2, g2)
  */
trait Bifunctor[F[_, _]] {

  def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]

  def leftMap[A, B, C](fab: F[A, B])(f: A => C): F[C, B] =
    bimap(fab)(f, (b: B) => b)

  def rightMap[A, B, D](fab: F[A, B])(g: B => D): F[A, D] =
    bimap(fab)((a: A) => a, g)

}

object Bifunctor {

  def apply[F[_, _]](using b: Bifunctor[F]): Bifunctor[F] = b

  given Bifunctor[Either] with {
    def bimap[A, B, C, D](
        fab: Either[A, B]
    )(f: A => C, g: B => D): Either[C, D] =
      fab match {
        case Left(a)  => Left(f(a))
        case Right(b) => Right(g(b))
      }
  }

  given Bifunctor[Tuple2] with {
    def bimap[A, B, C, D](fab: (A, B))(f: A => C, g: B => D): (C, D) =
      (f(fab._1), g(fab._2))
  }

}
