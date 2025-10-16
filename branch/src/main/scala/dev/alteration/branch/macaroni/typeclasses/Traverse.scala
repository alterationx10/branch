package dev.alteration.branch.macaroni.typeclasses

/** A type-class for traversable functors - functors that can be traversed with
  * effects.
  *
  * Laws:
  *   - Identity: traverse(fa)(a => pure(a)) == pure(fa)
  *   - Composition: traverse(fa)(a => map(g(a))(f)) == map(traverse(fa)(g))(t
  *     \=> traverse(t)(f))
  *   - Naturality: for any applicative transformation t: F ~> G,
  *     t(traverse(fa)(f))
  * \== traverse(fa)(a => t(f(a)))
  */
trait Traverse[F[_]] extends Functor[F] with Foldable[F] {

  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]

  def sequence[G[_]: Applicative, A](fga: F[G[A]]): G[F[A]] =
    traverse(fga)((ga: G[A]) => ga)

  // Implement map using traverse with Id
  override def map[A, B](fa: F[A])(f: A => B): F[B] = {
    // Use Option as a simple applicative for Id-like behavior
    type Id[X] = X
    given Applicative[Id] with {
      def pure[X](x: X): Id[X]                       = x
      def ap[X, Y](ff: Id[X => Y])(fx: Id[X]): Id[Y] = ff(fx)
    }
    traverse[Id, A, B](fa)(f)
  }

}

object Traverse {

  def apply[F[_]](using t: Traverse[F]): Traverse[F] = t

  given Traverse[List] with {
    def traverse[G[_]: Applicative, A, B](
        fa: List[A]
    )(f: A => G[B]): G[List[B]] = {
      val G = summon[Applicative[G]]
      fa.foldRight(G.pure(List.empty[B])) { (a, acc) =>
        G.map2(f(a), acc)(_ :: _)
      }
    }

    def foldLeft[A, B](fa: List[A], b: B)(f: (B, A) => B): B =
      fa.foldLeft(b)(f)

    def foldRight[A, B](fa: List[A], b: B)(f: (A, B) => B): B =
      fa.foldRight(b)(f)
  }

  given Traverse[Vector] with {
    def traverse[G[_]: Applicative, A, B](
        fa: Vector[A]
    )(f: A => G[B]): G[Vector[B]] = {
      val G = summon[Applicative[G]]
      fa.foldRight(G.pure(Vector.empty[B])) { (a, acc) =>
        G.map2(f(a), acc)(_ +: _)
      }
    }

    def foldLeft[A, B](fa: Vector[A], b: B)(f: (B, A) => B): B =
      fa.foldLeft(b)(f)

    def foldRight[A, B](fa: Vector[A], b: B)(f: (A, B) => B): B =
      fa.foldRight(b)(f)
  }

  given Traverse[Option] with {
    def traverse[G[_]: Applicative, A, B](
        fa: Option[A]
    )(f: A => G[B]): G[Option[B]] = {
      val G = summon[Applicative[G]]
      fa match {
        case Some(a) => G.map(f(a))(Some(_))
        case None    => G.pure(None)
      }
    }

    def foldLeft[A, B](fa: Option[A], b: B)(f: (B, A) => B): B =
      fa match {
        case Some(a) => f(b, a)
        case None    => b
      }

    def foldRight[A, B](fa: Option[A], b: B)(f: (A, B) => B): B =
      fa match {
        case Some(a) => f(a, b)
        case None    => b
      }
  }

  given [E]: Traverse[[T] =>> Either[E, T]] with {
    def traverse[G[_]: Applicative, A, B](
        fa: Either[E, A]
    )(f: A => G[B]): G[Either[E, B]] = {
      val G = summon[Applicative[G]]
      fa match {
        case Right(a) => G.map(f(a))(Right(_))
        case Left(e)  => G.pure(Left(e))
      }
    }

    def foldLeft[A, B](fa: Either[E, A], b: B)(f: (B, A) => B): B =
      fa match {
        case Right(a) => f(b, a)
        case Left(_)  => b
      }

    def foldRight[A, B](fa: Either[E, A], b: B)(f: (A, B) => B): B =
      fa match {
        case Right(a) => f(a, b)
        case Left(_)  => b
      }
  }

}
