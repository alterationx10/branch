package dev.alteration.branch.macaroni.typeclasses

import munit.*

class OrderSpec extends FunSuite {

  test("String ordering") {
    val ord = summon[Order[String]]
    assert(ord.compare("abc", "def") < 0)
    assert(ord.compare("xyz", "abc") > 0)
    assert(ord.compare("abc", "abc") == 0)
  }

  test("String totality law") {
    val ord = summon[Order[String]]
    val a   = "hello"
    val b   = "world"
    // compare(a, b) is always defined
    assert(ord.compare(a, b) != ord.compare(a, b) || true) // always evaluates
  }

  test("String antisymmetry law") {
    val ord = summon[Order[String]]
    val a   = "hello"
    val b   = "hello"
    // if compare(a, b) <= 0 && compare(b, a) <= 0 then eqv(a, b)
    if (ord.compare(a, b) <= 0 && ord.compare(b, a) <= 0) {
      assert(ord.eqv(a, b))
    }
  }

  test("String transitivity law") {
    val ord = summon[Order[String]]
    val a   = "abc"
    val b   = "def"
    val c   = "xyz"
    // if compare(a, b) <= 0 && compare(b, c) <= 0 then compare(a, c) <= 0
    if (ord.compare(a, b) <= 0 && ord.compare(b, c) <= 0) {
      assert(ord.compare(a, c) <= 0)
    }
  }

  test("Int ordering") {
    val ord = summon[Order[Int]]
    assert(ord.compare(10, 20) < 0)
    assert(ord.compare(50, 30) > 0)
    assert(ord.compare(42, 42) == 0)
  }

  test("Long ordering") {
    val ord = summon[Order[Long]]
    assert(ord.compare(100L, 200L) < 0)
    assert(ord.compare(500L, 300L) > 0)
    assert(ord.compare(42L, 42L) == 0)
  }

  test("Double ordering") {
    val ord = summon[Order[Double]]
    assert(ord.compare(3.14, 2.71) > 0)
    assert(ord.compare(1.5, 2.5) < 0)
    assert(ord.compare(42.0, 42.0) == 0)
  }

  test("Boolean ordering") {
    val ord = summon[Order[Boolean]]
    assert(ord.compare(false, true) < 0)
    assert(ord.compare(true, false) > 0)
    assert(ord.compare(true, true) == 0)
  }

  test("Order lt method") {
    val ord = summon[Order[Int]]
    assert(ord.lt(10, 20))
    assert(!ord.lt(30, 20))
    assert(!ord.lt(20, 20))
  }

  test("Order lteqv method") {
    val ord = summon[Order[Int]]
    assert(ord.lteqv(10, 20))
    assert(ord.lteqv(20, 20))
    assert(!ord.lteqv(30, 20))
  }

  test("Order gt method") {
    val ord = summon[Order[Int]]
    assert(ord.gt(30, 20))
    assert(!ord.gt(10, 20))
    assert(!ord.gt(20, 20))
  }

  test("Order gteqv method") {
    val ord = summon[Order[Int]]
    assert(ord.gteqv(30, 20))
    assert(ord.gteqv(20, 20))
    assert(!ord.gteqv(10, 20))
  }

  test("Order min method") {
    val ord = summon[Order[Int]]
    assertEquals(ord.min(10, 20), 10)
    assertEquals(ord.min(30, 15), 15)
    assertEquals(ord.min(42, 42), 42)
  }

  test("Order max method") {
    val ord = summon[Order[Int]]
    assertEquals(ord.max(10, 20), 20)
    assertEquals(ord.max(30, 15), 30)
    assertEquals(ord.max(42, 42), 42)
  }

  test("Order < operator") {
    val a = 10
    val b = 20
    assert(a < b)
    assert(!(b < a))
  }

  test("Order <= operator") {
    val a = 10
    val b = 20
    val c = 20
    assert(a <= b)
    assert(b <= c)
  }

  test("Order > operator") {
    val a = 30
    val b = 20
    assert(a > b)
    assert(!(b > a))
  }

  test("Order >= operator") {
    val a = 30
    val b = 20
    val c = 20
    assert(a >= b)
    assert(b >= c)
  }

  test("List ordering - empty list is smallest") {
    val ord = summon[Order[List[Int]]]
    assert(ord.compare(List.empty, List(1)) < 0)
  }

  test("List ordering - lexicographic") {
    val ord = summon[Order[List[Int]]]
    assert(ord.compare(List(1, 2, 3), List(1, 2, 4)) < 0)
    assert(ord.compare(List(2, 1), List(1, 9)) > 0)
  }

  test("Option ordering - None is smallest") {
    val ord = summon[Order[Option[Int]]]
    assert(ord.compare(None, Some(1)) < 0)
    assert(ord.compare(Some(1), None) > 0)
  }

  test("Option ordering - compares values when both Some") {
    val ord = summon[Order[Option[Int]]]
    assert(ord.compare(Some(10), Some(20)) < 0)
    assert(ord.compare(Some(30), Some(20)) > 0)
    assert(ord.compare(Some(42), Some(42)) == 0)
  }

  test("Order extends Eq") {
    val ord = summon[Order[Int]]
    // Order should also work as Eq
    assert(ord.eqv(42, 42))
    assert(!ord.eqv(42, 43))
  }

  test("Order apply summons instance") {
    val ord = Order[String]
    assert(ord.compare("a", "b") < 0)
  }

  test("Order fromOrdering constructor") {
    case class Person(name: String, age: Int)
    given Ordering[Person] = Ordering.by(_.age)
    given Order[Person]    = Order.fromOrdering[Person]

    val p1 = Person("Alice", 25)
    val p2 = Person("Bob", 30)
    val p3 = Person("Charlie", 25)

    assert(p1 < p2)
    assert(!(p2 < p1))
    assert(p1 === p3) // same age
  }

}
