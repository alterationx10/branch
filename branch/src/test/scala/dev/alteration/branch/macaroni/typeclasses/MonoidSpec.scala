package dev.alteration.branch.macaroni.typeclasses

import Monoid.given
import munit.*

class MonoidSpec extends FunSuite {

  test("String monoid has correct identity") {
    val m = summon[Monoid[String]]
    assertEquals(m.empty, "")
  }

  test("String monoid left identity law") {
    val m = summon[Monoid[String]]
    val a = "Hello"
    assertEquals(m.combine(m.empty, a), a)
  }

  test("String monoid right identity law") {
    val m = summon[Monoid[String]]
    val a = "Hello"
    assertEquals(m.combine(a, m.empty), a)
  }

  test("String monoid associativity") {
    val m = summon[Monoid[String]]
    val a = "Hello"
    val b = " "
    val c = "World"
    assertEquals(
      m.combine(m.combine(a, b), c),
      m.combine(a, m.combine(b, c))
    )
  }

  test("Int monoid has correct identity") {
    val m = summon[Monoid[Int]]
    assertEquals(m.empty, 0)
  }

  test("Int monoid left identity law") {
    val m = summon[Monoid[Int]]
    val a = 42
    assertEquals(m.combine(m.empty, a), a)
  }

  test("Int monoid right identity law") {
    val m = summon[Monoid[Int]]
    val a = 42
    assertEquals(m.combine(a, m.empty), a)
  }

  test("Long monoid has correct identity") {
    val m = summon[Monoid[Long]]
    assertEquals(m.empty, 0L)
  }

  test("List monoid has correct identity") {
    val m = summon[Monoid[List[Int]]]
    assertEquals(m.empty, List.empty[Int])
  }

  test("List monoid left identity law") {
    val m = summon[Monoid[List[Int]]]
    val a = List(1, 2, 3)
    assertEquals(m.combine(m.empty, a), a)
  }

  test("List monoid right identity law") {
    val m = summon[Monoid[List[Int]]]
    val a = List(1, 2, 3)
    assertEquals(m.combine(a, m.empty), a)
  }

  test("Vector monoid has correct identity") {
    val m = summon[Monoid[Vector[String]]]
    assertEquals(m.empty, Vector.empty[String])
  }

  test("Set monoid has correct identity") {
    val m = summon[Monoid[Set[Int]]]
    assertEquals(m.empty, Set.empty[Int])
  }

  test("Set monoid combines correctly") {
    val m = summon[Monoid[Set[Int]]]
    val a = Set(1, 2, 3)
    val b = Set(3, 4, 5)
    assertEquals(m.combine(a, b), Set(1, 2, 3, 4, 5))
  }

  test("Map monoid has correct identity") {
    val m = summon[Monoid[Map[String, Int]]]
    assertEquals(m.empty, Map.empty[String, Int])
  }

  test("Map monoid combines correctly") {
    val m = summon[Monoid[Map[String, Int]]]
    val a = Map("a" -> 1, "b" -> 2)
    val b = Map("b" -> 3, "c" -> 4)
    // Note: second map overwrites first
    assertEquals(m.combine(a, b), Map("a" -> 1, "b" -> 3, "c" -> 4))
  }

  test("Option monoid has correct identity") {
    val m = summon[Monoid[Option[Int]]]
    assertEquals(m.empty, None)
  }

  test("Option monoid combines with orElse semantics") {
    val m = summon[Monoid[Option[Int]]]
    assertEquals(m.combine(Some(1), Some(2)), Some(1))
    assertEquals(m.combine(None, Some(2)), Some(2))
    assertEquals(m.combine(Some(1), None), Some(1))
    assertEquals(m.combine(None, None), None)
  }

  test("Monoid |+| operator works") {
    val a      = List(1, 2)
    val b      = List(3, 4)
    val result = a |+| b
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("Monoid apply summons instance") {
    val m = Monoid[String]
    assertEquals(m.empty, "")
    assertEquals(m.combine("Hello", " World"), "Hello World")
  }

}
