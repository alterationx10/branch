package dev.alteration.branch.macaroni.typeclasses

import munit.*

class TraverseSpec extends FunSuite {

  test("List traverse with Option") {
    val t      = summon[Traverse[List]]
    val result = t.traverse(List(1, 2, 3))(x => Some(x * 2): Option[Int])
    assertEquals(result, Some(List(2, 4, 6)))
  }

  test("List traverse with Option returning None") {
    val t      = summon[Traverse[List]]
    val result = t.traverse(List(1, 2, 3)) { x =>
      if (x > 2) None else Some(x * 2): Option[Int]
    }
    assertEquals(result, None)
  }

  test("List traverse with empty list") {
    val t      = summon[Traverse[List]]
    val result = t.traverse(List.empty[Int])(x => Some(x * 2): Option[Int])
    assertEquals(result, Some(List.empty[Int]))
  }

  test("List sequence with Option") {
    val t                        = summon[Traverse[List]]
    val input: List[Option[Int]] = List(Some(1), Some(2), Some(3))
    val result                   = t.sequence(input)
    assertEquals(result, Some(List(1, 2, 3)))
  }

  test("List sequence with Option containing None") {
    val t                        = summon[Traverse[List]]
    val input: List[Option[Int]] = List(Some(1), None, Some(3))
    val result                   = t.sequence(input)
    assertEquals(result, None)
  }

  test("Vector traverse with Option") {
    val t      = summon[Traverse[Vector]]
    val result = t.traverse(Vector(10, 20, 30))(x => Some(x / 10): Option[Int])
    assertEquals(result, Some(Vector(1, 2, 3)))
  }

  test("Vector sequence with Option") {
    val t                          = summon[Traverse[Vector]]
    val input: Vector[Option[Int]] = Vector(Some(1), Some(2))
    val result                     = t.sequence(input)
    assertEquals(result, Some(Vector(1, 2)))
  }

  test("Option traverse with Some") {
    val t      = summon[Traverse[Option]]
    val result = t.traverse(Some(42))(x => List(x, x * 2))
    assertEquals(result, List(Some(42), Some(84)))
  }

  test("Option traverse with None") {
    val t      = summon[Traverse[Option]]
    val result = t.traverse(None: Option[Int])(x => List(x * 2))
    assertEquals(result, List(None))
  }

  test("Option sequence") {
    val t = summon[Traverse[Option]]
    assertEquals(
      t.sequence(Some(List(1, 2, 3))),
      List(Some(1), Some(2), Some(3))
    )
    assertEquals(t.sequence(None: Option[List[Int]]), List(None))
  }

  test("Either traverse with Right") {
    val t      = summon[Traverse[[T] =>> Either[String, T]]]
    val result =
      t.traverse(Right(42): Either[String, Int])(x => Some(x * 2): Option[Int])
    assertEquals(result, Some(Right(84)))
  }

  test("Either traverse with Left") {
    val t      = summon[Traverse[[T] =>> Either[String, T]]]
    val result = t.traverse(Left("error"): Either[String, Int])(x =>
      Some(x * 2): Option[Int]
    )
    assertEquals(result, Some(Left("error")))
  }

  test("Either sequence") {
    val t     = summon[Traverse[[T] =>> Either[String, T]]]
    val right = Right(Some(42)): Either[String, Option[Int]]
    val left  = Left("error"): Either[String, Option[Int]]
    assertEquals(t.sequence(right), Some(Right(42)))
    assertEquals(t.sequence(left), Some(Left("error")))
  }

  test("Traverse map inherited from Functor") {
    val t      = summon[Traverse[List]]
    val result = t.map(List(1, 2, 3))(_ * 2)
    assertEquals(result, List(2, 4, 6))
  }

  test("Traverse foldLeft inherited from Foldable") {
    val t      = summon[Traverse[List]]
    val result = t.foldLeft(List(1, 2, 3, 4), 0)(_ + _)
    assertEquals(result, 10)
  }

  test("Traverse apply summons instance") {
    val t      = Traverse[List]
    val result = t.traverse(List(1, 2, 3))(x => Some(x * 2): Option[Int])
    assertEquals(result, Some(List(2, 4, 6)))
  }

}
