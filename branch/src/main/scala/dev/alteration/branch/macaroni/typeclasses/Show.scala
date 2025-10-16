package dev.alteration.branch.macaroni.typeclasses

/** A type-class for converting values to human-readable strings.
  *
  * This provides a type-safe alternative to `.toString` for domain types.
  */
trait Show[T] {

  def show(t: T): String

}

object Show {

  def apply[T](using s: Show[T]): Show[T] = s

  def fromToString[T]: Show[T] = new Show[T] {
    def show(t: T): String = t.toString
  }

  given Show[String] with {
    def show(s: String): String = s
  }

  given Show[Int] with {
    def show(i: Int): String = i.toString
  }

  given Show[Long] with {
    def show(l: Long): String = l.toString
  }

  given Show[Double] with {
    def show(d: Double): String = d.toString
  }

  given Show[Boolean] with {
    def show(b: Boolean): String = b.toString
  }

  given [T](using s: Show[T]): Show[List[T]] with {
    def show(list: List[T]): String =
      list.map(s.show).mkString("[", ", ", "]")
  }

  given [T](using s: Show[T]): Show[Option[T]] with {
    def show(opt: Option[T]): String = opt match {
      case Some(t) => s"Some(${s.show(t)})"
      case None    => "None"
    }
  }

  given [L, R](using sl: Show[L], sr: Show[R]): Show[Either[L, R]] with {
    def show(either: Either[L, R]): String = either match {
      case Left(l)  => s"Left(${sl.show(l)})"
      case Right(r) => s"Right(${sr.show(r)})"
    }
  }

}
