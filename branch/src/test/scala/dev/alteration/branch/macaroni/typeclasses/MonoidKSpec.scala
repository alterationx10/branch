package dev.alteration.branch.macaroni.typeclasses

import MonoidK.given
import munit.*

class MonoidKSpec extends FunSuite {

  test("List monoidK has correct identity") {
    val m = summon[MonoidK[List]]
    assertEquals(m.emptyK[Int], List.empty[Int])
  }

  test("List monoidK left identity law") {
    val m = summon[MonoidK[List]]
    val a = List(1, 2, 3)
    assertEquals(m.combineK(m.emptyK, a), a)
  }

  test("List monoidK right identity law") {
    val m = summon[MonoidK[List]]
    val a = List(1, 2, 3)
    assertEquals(m.combineK(a, m.emptyK), a)
  }

  test("List monoidK associativity") {
    val m = summon[MonoidK[List]]
    val a = List(1, 2)
    val b = List(3, 4)
    val c = List(5, 6)
    assertEquals(
      m.combineK(m.combineK(a, b), c),
      m.combineK(a, m.combineK(b, c))
    )
  }

  test("List monoidK combine") {
    val m      = summon[MonoidK[List]]
    val result = m.combineK(List(1, 2), List(3, 4))
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("Vector monoidK has correct identity") {
    val m = summon[MonoidK[Vector]]
    assertEquals(m.emptyK[String], Vector.empty[String])
  }

  test("Vector monoidK left identity law") {
    val m = summon[MonoidK[Vector]]
    val a = Vector("a", "b")
    assertEquals(m.combineK(m.emptyK, a), a)
  }

  test("Vector monoidK right identity law") {
    val m = summon[MonoidK[Vector]]
    val a = Vector("a", "b")
    assertEquals(m.combineK(a, m.emptyK), a)
  }

  test("Vector monoidK combine") {
    val m      = summon[MonoidK[Vector]]
    val result = m.combineK(Vector(1, 2), Vector(3, 4))
    assertEquals(result, Vector(1, 2, 3, 4))
  }

  test("Option monoidK has correct identity") {
    val m = summon[MonoidK[Option]]
    assertEquals(m.emptyK[Int], None)
  }

  test("Option monoidK left identity law") {
    val m = summon[MonoidK[Option]]
    val a = Some(42)
    assertEquals(m.combineK(m.emptyK, a), a)
  }

  test("Option monoidK right identity law") {
    val m = summon[MonoidK[Option]]
    val a = Some(42)
    assertEquals(m.combineK(a, m.emptyK), a)
  }

  test("Option monoidK combine with Some values") {
    val m = summon[MonoidK[Option]]
    assertEquals(m.combineK(Some(1), Some(2)), Some(1))
  }

  test("Option monoidK combine with None") {
    val m = summon[MonoidK[Option]]
    assertEquals(m.combineK(None: Option[Int], Some(2)), Some(2))
    assertEquals(m.combineK(Some(1), None: Option[Int]), Some(1))
  }

  test("MonoidK <+> operator works") {
    val a      = List(1, 2)
    val b      = List(3, 4)
    val result = a <+> b
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("MonoidK apply summons instance") {
    val m = MonoidK[List]
    assertEquals(m.emptyK[Int], List.empty[Int])
    assertEquals(m.combineK(List(1, 2), List(3, 4)), List(1, 2, 3, 4))
  }

}
