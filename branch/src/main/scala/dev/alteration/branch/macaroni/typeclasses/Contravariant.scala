package dev.alteration.branch.macaroni.typeclasses

/** A type-class for contravariant functors.
  *
  * Where Functor maps in the positive direction (A => B becomes F[A] => F[B]),
  * Contravariant maps in the negative direction (A => B becomes F[B] => F[A]).
  *
  * This is useful for types that consume rather than produce values.
  *
  * Laws:
  *   - Identity: contramap(fa)(identity) == fa
  *   - Composition: contramap(fa)(f.andThen(g)) ==
  *     contramap(contramap(fa)(g))(f)
  */
trait Contravariant[F[_]] {

  def contramap[A, B](fa: F[A])(f: B => A): F[B]

}

object Contravariant {

  def apply[F[_]](using c: Contravariant[F]): Contravariant[F] = c

  // Ordering is contravariant
  given Contravariant[Ordering] with {
    def contramap[A, B](fa: Ordering[A])(f: B => A): Ordering[B] =
      (x: B, y: B) => fa.compare(f(x), f(y))
  }

  // Functions from A are contravariant in A
  given [R]: Contravariant[[A] =>> A => R] with {
    def contramap[A, B](fa: A => R)(f: B => A): B => R =
      b => fa(f(b))
  }

  // Predicates (functions to Boolean) are contravariant
  given Contravariant[[A] =>> A => Boolean] with {
    def contramap[A, B](fa: A => Boolean)(f: B => A): B => Boolean =
      b => fa(f(b))
  }

  // Eq is contravariant
  given Contravariant[Eq] with {
    def contramap[A, B](fa: Eq[A])(f: B => A): Eq[B] = new Eq[B] {
      def eqv(x: B, y: B): Boolean = fa.eqv(f(x), f(y))
    }
  }

  // Order is contravariant
  given Contravariant[Order] with {
    def contramap[A, B](fa: Order[A])(f: B => A): Order[B] = new Order[B] {
      def compare(x: B, y: B): Int = fa.compare(f(x), f(y))
    }
  }

  // Show is contravariant
  given Contravariant[Show] with {
    def contramap[A, B](fa: Show[A])(f: B => A): Show[B] = new Show[B] {
      def show(b: B): String = fa.show(f(b))
    }
  }

}
