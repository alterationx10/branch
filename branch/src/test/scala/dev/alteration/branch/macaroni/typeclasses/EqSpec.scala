package dev.alteration.branch.macaroni.typeclasses

import dev.alteration.branch.macaroni.typeclasses.Order.given
import munit.*

class EqSpec extends FunSuite {

  test("String equality") {
    val eq = summon[Eq[String]]
    assert(eq.eqv("hello", "hello"))
    assert(!eq.eqv("hello", "world"))
  }

  test("String reflexivity law") {
    val eq = summon[Eq[String]]
    val a  = "hello"
    assert(eq.eqv(a, a))
  }

  test("String symmetry law") {
    val eq = summon[Eq[String]]
    val a  = "hello"
    val b  = "hello"
    assertEquals(eq.eqv(a, b), eq.eqv(b, a))
  }

  test("String transitivity law") {
    val eq = summon[Eq[String]]
    val a  = "hello"
    val b  = "hello"
    val c  = "hello"
    assert(eq.eqv(a, b) && eq.eqv(b, c) && eq.eqv(a, c))
  }

  test("Int equality") {
    val eq = summon[Eq[Int]]
    assert(eq.eqv(42, 42))
    assert(!eq.eqv(42, 43))
  }

  test("Int reflexivity law") {
    val eq = summon[Eq[Int]]
    val a  = 42
    assert(eq.eqv(a, a))
  }

  test("Long equality") {
    val eq = summon[Eq[Long]]
    assert(eq.eqv(100L, 100L))
    assert(!eq.eqv(100L, 200L))
  }

  test("Double equality") {
    val eq = summon[Eq[Double]]
    assert(eq.eqv(3.14, 3.14))
    assert(!eq.eqv(3.14, 2.71))
  }

  test("Boolean equality") {
    val eq = summon[Eq[Boolean]]
    assert(eq.eqv(true, true))
    assert(eq.eqv(false, false))
    assert(!eq.eqv(true, false))
  }

  test("List equality") {
    val eq = summon[Eq[List[Int]]]
    assert(eq.eqv(List(1, 2, 3), List(1, 2, 3)))
    assert(!eq.eqv(List(1, 2, 3), List(1, 2, 4)))
    assert(!eq.eqv(List(1, 2, 3), List(1, 2)))
  }

  test("Option equality") {
    val eq = summon[Eq[Option[Int]]]
    assert(eq.eqv(Some(42), Some(42)))
    assert(eq.eqv(None, None))
    assert(!eq.eqv(Some(42), Some(43)))
    assert(!eq.eqv(Some(42), None))
    assert(!eq.eqv(None, Some(42)))
  }

  test("Either equality") {
    val eq = summon[Eq[Either[String, Int]]]
    assert(eq.eqv(Right(42), Right(42)))
    assert(eq.eqv(Left("error"), Left("error")))
    assert(!eq.eqv(Right(42), Right(43)))
    assert(!eq.eqv(Left("error"), Left("different")))
    assert(!eq.eqv(Left("error"), Right(42)))
  }

  test("Eq neqv method") {
    val eq = summon[Eq[Int]]
    assert(eq.neqv(42, 43))
    assert(!eq.neqv(42, 42))
  }

  test("Eq === operator") {
    val a = 42
    val b = 42
    val c = 43
    assert(a === b)
    assert(!(a === c))
  }

  test("Eq =!= operator") {
    val a = 42
    val b = 43
    val c = 42
    assert(a =!= b)
    assert(!(a =!= c))
  }

  test("Eq apply summons instance") {
    val eq = Eq[String]
    assert(eq.eqv("hello", "hello"))
  }

  test("Eq fromEquals constructor") {
    case class Person(name: String, age: Int)
    given Eq[Person] = Eq.fromEquals[Person]

    val p1 = Person("Alice", 30)
    val p2 = Person("Alice", 30)
    val p3 = Person("Bob", 25)

    assert(p1 === p2)
    assert(!(p1 === p3))
  }

}
