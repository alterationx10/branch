package dev.alteration.branch.macaroni.typeclasses

import SemigroupK.given
import munit.*

class SemigroupKSpec extends FunSuite {

  test("List semigroupK combine") {
    val sg     = summon[SemigroupK[List]]
    val result = sg.combineK(List(1, 2), List(3, 4))
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("List semigroupK with empty lists") {
    val sg = summon[SemigroupK[List]]
    assertEquals(sg.combineK(List.empty[Int], List(1, 2)), List(1, 2))
    assertEquals(sg.combineK(List(1, 2), List.empty[Int]), List(1, 2))
  }

  test("List semigroupK associativity law") {
    val sg = summon[SemigroupK[List]]
    val a  = List(1, 2)
    val b  = List(3, 4)
    val c  = List(5, 6)
    assertEquals(
      sg.combineK(sg.combineK(a, b), c),
      sg.combineK(a, sg.combineK(b, c))
    )
  }

  test("List semigroupK <+> operator") {
    val result = List(1, 2) <+> List(3, 4)
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("Vector semigroupK combine") {
    val sg     = summon[SemigroupK[Vector]]
    val result = sg.combineK(Vector(1, 2), Vector(3, 4))
    assertEquals(result, Vector(1, 2, 3, 4))
  }

  test("Vector semigroupK associativity") {
    val sg = summon[SemigroupK[Vector]]
    val a  = Vector("a", "b")
    val b  = Vector("c", "d")
    val c  = Vector("e", "f")
    assertEquals(
      sg.combineK(sg.combineK(a, b), c),
      sg.combineK(a, sg.combineK(b, c))
    )
  }

  test("Option semigroupK combine with Some values") {
    val sg = summon[SemigroupK[Option]]
    assertEquals(sg.combineK(Some(1), Some(2)), Some(1))
  }

  test("Option semigroupK combine with None") {
    val sg = summon[SemigroupK[Option]]
    assertEquals(sg.combineK(None: Option[Int], Some(2)), Some(2))
    assertEquals(sg.combineK(Some(1), None: Option[Int]), Some(1))
    assertEquals(sg.combineK(None: Option[Int], None: Option[Int]), None)
  }

  test("Option semigroupK orElse semantics") {
    val sg = summon[SemigroupK[Option]]
    // First Some wins (orElse behavior)
    assertEquals(sg.combineK(Some(10), Some(20)), Some(10))
  }

  test("Either semigroupK combine with Right values") {
    val sg     = summon[SemigroupK[[T] =>> Either[String, T]]]
    val result =
      sg.combineK(Right(1): Either[String, Int], Right(2): Either[String, Int])
    assertEquals(result, Right(1))
  }

  test("Either semigroupK combine with Left") {
    val sg = summon[SemigroupK[[T] =>> Either[String, T]]]
    assertEquals(
      sg.combineK(
        Left("error"): Either[String, Int],
        Right(2): Either[String, Int]
      ),
      Right(2)
    )
    assertEquals(
      sg.combineK(
        Right(1): Either[String, Int],
        Left("error"): Either[String, Int]
      ),
      Right(1)
    )
  }

  test("SemigroupK apply summons instance") {
    val sg     = SemigroupK[List]
    val result = sg.combineK(List(1, 2), List(3, 4))
    assertEquals(result, List(1, 2, 3, 4))
  }

}
