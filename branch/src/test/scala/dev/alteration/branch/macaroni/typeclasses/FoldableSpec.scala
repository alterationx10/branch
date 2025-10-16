package dev.alteration.branch.macaroni.typeclasses

import munit.*

class FoldableSpec extends FunSuite {

  test("List foldable foldLeft") {
    val f      = summon[Foldable[List]]
    val result = f.foldLeft(List(1, 2, 3, 4), 0)(_ + _)
    assertEquals(result, 10)
  }

  test("List foldable foldRight") {
    val f      = summon[Foldable[List]]
    val result = f.foldRight(List(1, 2, 3, 4), 0)(_ + _)
    assertEquals(result, 10)
  }

  test("List foldable foldMap with Monoid") {
    val f      = summon[Foldable[List]]
    val result = f.foldMap(List(1, 2, 3, 4))(x => x * 2)
    assertEquals(result, 20)
  }

  test("List foldable fold with Monoid") {
    val f      = summon[Foldable[List]]
    val result = f.fold(List("Hello", " ", "World"))
    assertEquals(result, "Hello World")
  }

  test("List foldable toList") {
    val f      = summon[Foldable[List]]
    val result = f.toList(List(1, 2, 3))
    assertEquals(result, List(1, 2, 3))
  }

  test("List foldable isEmpty") {
    val f = summon[Foldable[List]]
    assert(f.isEmpty(List.empty[Int]))
    assert(!f.isEmpty(List(1, 2, 3)))
  }

  test("List foldable nonEmpty") {
    val f = summon[Foldable[List]]
    assert(f.nonEmpty(List(1, 2, 3)))
    assert(!f.nonEmpty(List.empty[Int]))
  }

  test("List foldable size") {
    val f = summon[Foldable[List]]
    assertEquals(f.size(List(1, 2, 3, 4, 5)), 5)
    assertEquals(f.size(List.empty[Int]), 0)
  }

  test("List foldable exists") {
    val f = summon[Foldable[List]]
    assert(f.exists(List(1, 2, 3, 4))(_ > 3))
    assert(!f.exists(List(1, 2, 3))(_ > 5))
  }

  test("List foldable forall") {
    val f = summon[Foldable[List]]
    assert(f.forall(List(2, 4, 6, 8))(_ % 2 == 0))
    assert(!f.forall(List(1, 2, 3, 4))(_ % 2 == 0))
  }

  test("List foldable find") {
    val f = summon[Foldable[List]]
    assertEquals(f.find(List(1, 2, 3, 4))(_ > 2), Some(3))
    assertEquals(f.find(List(1, 2, 3))(_ > 5), None)
  }

  test("Vector foldable foldLeft") {
    val f      = summon[Foldable[Vector]]
    val result = f.foldLeft(Vector(10, 20, 30), 0)(_ + _)
    assertEquals(result, 60)
  }

  test("Vector foldable size") {
    val f = summon[Foldable[Vector]]
    assertEquals(f.size(Vector(1, 2, 3)), 3)
  }

  test("Option foldable foldLeft with Some") {
    val f      = summon[Foldable[Option]]
    val result = f.foldLeft(Some(42), 0)(_ + _)
    assertEquals(result, 42)
  }

  test("Option foldable foldLeft with None") {
    val f      = summon[Foldable[Option]]
    val result = f.foldLeft(None: Option[Int], 10)(_ + _)
    assertEquals(result, 10)
  }

  test("Option foldable size") {
    val f = summon[Foldable[Option]]
    assertEquals(f.size(Some(42)), 1)
    assertEquals(f.size(None: Option[Int]), 0)
  }

  test("Option foldable isEmpty") {
    val f = summon[Foldable[Option]]
    assert(f.isEmpty(None: Option[Int]))
    assert(!f.isEmpty(Some(42)))
  }

  test("Either foldable foldLeft with Right") {
    val f      = summon[Foldable[[T] =>> Either[String, T]]]
    val result = f.foldLeft(Right(42): Either[String, Int], 0)(_ + _)
    assertEquals(result, 42)
  }

  test("Either foldable foldLeft with Left") {
    val f      = summon[Foldable[[T] =>> Either[String, T]]]
    val result = f.foldLeft(Left("error"): Either[String, Int], 10)(_ + _)
    assertEquals(result, 10)
  }

  test("Either foldable size") {
    val f = summon[Foldable[[T] =>> Either[String, T]]]
    assertEquals(f.size(Right(42): Either[String, Int]), 1)
    assertEquals(f.size(Left("error"): Either[String, Int]), 0)
  }

  test("Foldable apply summons instance") {
    val f = Foldable[List]
    assertEquals(f.size(List(1, 2, 3)), 3)
  }

}
