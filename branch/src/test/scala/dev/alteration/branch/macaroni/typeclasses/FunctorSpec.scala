package dev.alteration.branch.macaroni.typeclasses

import Functor.given
import munit.*

class FunctorSpec extends FunSuite {

  test("List functor map") {
    val f      = summon[Functor[List]]
    val result = f.map(List(1, 2, 3))(_ * 2)
    assertEquals(result, List(2, 4, 6))
  }

  test("List functor identity law") {
    val f  = summon[Functor[List]]
    val fa = List(1, 2, 3)
    assertEquals(f.map(fa)(x => x), fa)
  }

  test("List functor composition law") {
    val f                = summon[Functor[List]]
    val fa               = List(1, 2, 3)
    val g: Int => Int    = _ * 2
    val h: Int => String = _.toString

    assertEquals(
      f.map(f.map(fa)(g))(h),
      f.map(fa)(g.andThen(h))
    )
  }

  test("Vector functor map") {
    val f      = summon[Functor[Vector]]
    val result = f.map(Vector(1, 2, 3))(_ + 10)
    assertEquals(result, Vector(11, 12, 13))
  }

  test("Vector functor identity law") {
    val f  = summon[Functor[Vector]]
    val fa = Vector("a", "b", "c")
    assertEquals(f.map(fa)(identity), fa)
  }

  test("Option functor map with Some") {
    val f      = summon[Functor[Option]]
    val result = f.map(Some(42))(_ * 2)
    assertEquals(result, Some(84))
  }

  test("Option functor map with None") {
    val f      = summon[Functor[Option]]
    val result = f.map(None: Option[Int])(_ * 2)
    assertEquals(result, None)
  }

  test("Option functor identity law") {
    val f    = summon[Functor[Option]]
    val some = Some(42)
    val none = None: Option[Int]
    assertEquals(f.map(some)(identity), some)
    assertEquals(f.map(none)(identity), none)
  }

  test("Either functor map with Right") {
    val f      = summon[Functor[[T] =>> Either[String, T]]]
    val result = f.map(Right(42))(_ * 2)
    assertEquals(result, Right(84))
  }

  test("Either functor map with Left") {
    val f      = summon[Functor[[T] =>> Either[String, T]]]
    val result = f.map(Left("error"): Either[String, Int])(_ * 2)
    assertEquals(result, Left("error"))
  }

  test("Either functor identity law") {
    val f     = summon[Functor[[T] =>> Either[String, T]]]
    val right = Right(42): Either[String, Int]
    val left  = Left("error"): Either[String, Int]
    assertEquals(f.map(right)(identity), right)
    assertEquals(f.map(left)(identity), left)
  }

  test("Map functor map") {
    val f      = summon[Functor[[V] =>> Map[String, V]]]
    val input  = Map("a" -> 1, "b" -> 2, "c" -> 3)
    val result = f.map(input)(_ * 10)
    assertEquals(result, Map("a" -> 10, "b" -> 20, "c" -> 30))
  }

  test("Map functor identity law") {
    val f  = summon[Functor[[V] =>> Map[String, V]]]
    val fa = Map("x" -> 100, "y" -> 200)
    assertEquals(f.map(fa)(identity), fa)
  }

  test("Functor as method") {
    val result = Some(42).as("constant")
    assertEquals(result, Some("constant"))
  }

  test("Functor as method with None") {
    val result = (None: Option[Int]).as("constant")
    assertEquals(result, None)
  }

  test("Functor void method") {
    val result = Some(42).void
    assertEquals(result, Some(()))
  }

  test("Functor void method with None") {
    val result = (None: Option[Int]).void
    assertEquals(result, None)
  }

  test("Functor void method with List") {
    val result = List(1, 2, 3).void
    assertEquals(result, List((), (), ()))
  }

  test("Functor apply summons instance") {
    val f      = Functor[List]
    val result = f.map(List(1, 2, 3))(_ * 2)
    assertEquals(result, List(2, 4, 6))
  }

  test("Functor composition preserves structure") {
    val f      = summon[Functor[List]]
    val fa     = List(1, 2, 3, 4, 5)
    val mapped = f.map(fa)(_ * 2)
    assertEquals(mapped.length, fa.length)
  }

  test("Functor with nested structures") {
    val f      = summon[Functor[List]]
    val nested = List(Some(1), None, Some(3))
    val result = f.map(nested)(opt => opt.map(_ * 2))
    assertEquals(result, List(Some(2), None, Some(6)))
  }

}
