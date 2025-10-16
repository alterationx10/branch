package dev.alteration.branch.macaroni.typeclasses

/** A type-class for data structures that can be folded.
  *
  * Laws:
  *   - Left fold consistency: foldLeft(fa, b)(f) should traverse left-to-right
  *   - Right fold consistency: foldRight(fa, b)(f) should traverse
  *     right-to-left
  *   - foldMap consistency: foldMap(fa)(f) == foldLeft(fa, empty)((b, a) => b
  *     \|+| f(a))
  */
trait Foldable[F[_]] {

  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  def foldRight[A, B](fa: F[A], b: B)(f: (A, B) => B): B

  def foldMap[A, B](fa: F[A])(f: A => B)(using m: Monoid[B]): B =
    foldLeft(fa, m.empty)((b, a) => m.combine(b, f(a)))

  def fold[A](fa: F[A])(using m: Monoid[A]): A =
    foldMap(fa)(identity)

  def toList[A](fa: F[A]): List[A] =
    foldLeft(fa, List.empty[A])((acc, a) => a :: acc).reverse

  def isEmpty[A](fa: F[A]): Boolean =
    foldLeft(fa, true)((_, _) => false)

  def nonEmpty[A](fa: F[A]): Boolean =
    !isEmpty(fa)

  def size[A](fa: F[A]): Int =
    foldLeft(fa, 0)((count, _) => count + 1)

  def exists[A](fa: F[A])(p: A => Boolean): Boolean =
    foldLeft(fa, false)((acc, a) => acc || p(a))

  def forall[A](fa: F[A])(p: A => Boolean): Boolean =
    foldLeft(fa, true)((acc, a) => acc && p(a))

  def find[A](fa: F[A])(p: A => Boolean): Option[A] =
    foldLeft(fa, Option.empty[A]) { (acc, a) =>
      acc.orElse(Some(a).filter(p))
    }

}

object Foldable {

  def apply[F[_]](using f: Foldable[F]): Foldable[F] = f

  given Foldable[List] with {
    def foldLeft[A, B](fa: List[A], b: B)(f: (B, A) => B): B =
      fa.foldLeft(b)(f)

    def foldRight[A, B](fa: List[A], b: B)(f: (A, B) => B): B =
      fa.foldRight(b)(f)
  }

  given Foldable[Vector] with {
    def foldLeft[A, B](fa: Vector[A], b: B)(f: (B, A) => B): B =
      fa.foldLeft(b)(f)

    def foldRight[A, B](fa: Vector[A], b: B)(f: (A, B) => B): B =
      fa.foldRight(b)(f)
  }

  given Foldable[Option] with {
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

  given [E]: Foldable[[T] =>> Either[E, T]] with {
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
