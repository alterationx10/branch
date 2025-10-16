package dev.alteration.branch.macaroni.typeclasses

import munit.*

class ContravariantSpec extends FunSuite {

  test("Ordering contravariant contramap") {
    val cf             = summon[Contravariant[Ordering]]
    val intOrdering    = Ordering[Int]
    case class Person(age: Int)
    val personOrdering = cf.contramap(intOrdering)((p: Person) => p.age)

    val alice = Person(30)
    val bob   = Person(25)
    assert(personOrdering.compare(alice, bob) > 0)
    assert(personOrdering.compare(bob, alice) < 0)
  }

  test("Ordering contravariant identity law") {
    val cf           = summon[Contravariant[Ordering]]
    val ord          = Ordering[Int]
    val contramapped = cf.contramap(ord)((x: Int) => x)

    assertEquals(contramapped.compare(10, 20), ord.compare(10, 20))
  }

  test("Function contravariant contramap") {
    val cf        = summon[Contravariant[[A] =>> A => Int]]
    val lengthFn  = (s: String) => s.length
    case class Wrapper(value: String)
    val wrapperFn = cf.contramap(lengthFn)((w: Wrapper) => w.value)

    assertEquals(wrapperFn(Wrapper("hello")), 5)
  }

  test("Predicate contravariant contramap") {
    val cf           = summon[Contravariant[[A] =>> A => Boolean]]
    val isEven       = (n: Int) => n % 2 == 0
    case class Number(value: Int)
    val numberIsEven = cf.contramap(isEven)((n: Number) => n.value)

    assert(numberIsEven(Number(4)))
    assert(!numberIsEven(Number(3)))
  }

  test("Eq contravariant contramap") {
    val cf    = summon[Contravariant[Eq]]
    val intEq = summon[Eq[Int]]
    case class Age(years: Int)
    val ageEq = cf.contramap(intEq)((a: Age) => a.years)

    assert(ageEq.eqv(Age(25), Age(25)))
    assert(!ageEq.eqv(Age(25), Age(30)))
  }

  test("Order contravariant contramap") {
    val cf         = summon[Contravariant[Order]]
    val intOrder   = summon[Order[Int]]
    case class Score(points: Int)
    val scoreOrder = cf.contramap(intOrder)((s: Score) => s.points)

    assert(scoreOrder.lt(Score(10), Score(20)))
    assert(scoreOrder.gt(Score(30), Score(20)))
    assert(scoreOrder.eqv(Score(15), Score(15)))
  }

  test("Show contravariant contramap") {
    val cf        = summon[Contravariant[Show]]
    val intShow   = summon[Show[Int]]
    case class Count(value: Int)
    val countShow = cf.contramap(intShow)((c: Count) => c.value)

    assertEquals(countShow.show(Count(42)), "42")
  }

  test("Contravariant composition law") {
    val cf         = summon[Contravariant[[A] =>> A => String]]
    val toStringFn = (n: Int) => n.toString
    val f          = (s: String) => s.length                // String => Int
    val g          = (b: Boolean) => if (b) "yes" else "no" // Boolean => String

    val result1 = cf.contramap(cf.contramap(toStringFn)(f))(g)
    val result2 = cf.contramap(toStringFn)((b: Boolean) => f(g(b)))

    // Both should produce the same behavior
    assertEquals(result1(true), result2(true))
    assertEquals(result1(false), result2(false))
  }

  test("Contravariant apply summons instance") {
    val cf            = Contravariant[Ordering]
    val intOrdering   = Ordering[Int]
    case class Value(n: Int)
    val valueOrdering = cf.contramap(intOrdering)((v: Value) => v.n)

    assert(valueOrdering.compare(Value(10), Value(20)) < 0)
  }

}
