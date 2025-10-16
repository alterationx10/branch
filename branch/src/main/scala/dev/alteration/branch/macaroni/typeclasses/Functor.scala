package dev.alteration.branch.macaroni.typeclasses

/** A type-class for types that can be mapped over.
  *
  * Laws:
  *   - Identity: map(fa)(x => x) == fa
  *   - Composition: map(map(fa)(f))(g) == map(fa)(f.andThen(g))
  */
trait Functor[F[_]] {

  def map[A, B](fa: F[A])(f: A => B): F[B]

  extension [A](fa: F[A]) {
    def as[B](b: B): F[B] = Functor.this.map(fa)(_ => b)
    def void: F[Unit]     = as(())
  }

}

object Functor {

  def apply[F[_]](using f: Functor[F]): Functor[F] = f

  given Functor[List] with {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }

  given Functor[Vector] with {
    def map[A, B](fa: Vector[A])(f: A => B): Vector[B] = fa.map(f)
  }

  given Functor[Option] with {
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
  }

  given [E]: Functor[[T] =>> Either[E, T]] with {
    def map[A, B](fa: Either[E, A])(f: A => B): Either[E, B] = fa.map(f)
  }

  given [K]: Functor[[V] =>> Map[K, V]] with {
    def map[A, B](fa: Map[K, A])(f: A => B): Map[K, B] =
      fa.view.mapValues(f).toMap
  }

}
