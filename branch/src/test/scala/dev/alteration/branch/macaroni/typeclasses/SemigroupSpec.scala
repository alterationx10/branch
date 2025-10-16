package dev.alteration.branch.macaroni.typeclasses

import munit.*

class SemigroupSpec extends FunSuite {

  test("String semigroup combines correctly") {
    given Semigroup[String] with {
      def combine(a: String, b: String): String = a + b
    }

    val result = summon[Semigroup[String]].combine("Hello", " World")
    assertEquals(result, "Hello World")
  }

  test("Int semigroup with addition") {
    given Semigroup[Int] with {
      def combine(a: Int, b: Int): Int = a + b
    }

    val result = summon[Semigroup[Int]].combine(5, 3)
    assertEquals(result, 8)
  }

  test("List semigroup combines correctly") {
    given [T]: Semigroup[List[T]] with {
      def combine(a: List[T], b: List[T]): List[T] = a ++ b
    }

    val result = summon[Semigroup[List[Int]]].combine(List(1, 2), List(3, 4))
    assertEquals(result, List(1, 2, 3, 4))
  }

  test("Semigroup associativity law for strings") {
    given Semigroup[String] with {
      def combine(a: String, b: String): String = a + b
    }

    val sg = summon[Semigroup[String]]
    val a  = "Hello"
    val b  = " "
    val c  = "World"

    // (a + b) + c == a + (b + c)
    val left  = sg.combine(sg.combine(a, b), c)
    val right = sg.combine(a, sg.combine(b, c))
    assertEquals(left, right)
  }

  test("Semigroup associativity law for ints") {
    given Semigroup[Int] with {
      def combine(a: Int, b: Int): Int = a + b
    }

    val sg = summon[Semigroup[Int]]
    val a  = 10
    val b  = 20
    val c  = 30

    val left  = sg.combine(sg.combine(a, b), c)
    val right = sg.combine(a, sg.combine(b, c))
    assertEquals(left, right)
  }

  test("Semigroup |+| operator") {
    given Semigroup[Int] with {
      def combine(a: Int, b: Int): Int = a + b
    }

    val a      = 5
    val b      = 3
    val result = a |+| b
    assertEquals(result, 8)
  }

  test("Semigroup |+| operator chains associatively") {
    given Semigroup[String] with {
      def combine(a: String, b: String): String = a + b
    }

    val result = "Hello" |+| " " |+| "World"
    assertEquals(result, "Hello World")
  }

}
