package dev.alteration.branch.macaroni.typeclasses

import munit.*

class BifunctorSpec extends FunSuite {

  test("Either bifunctor bimap with Right") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.bimap(Right(42): Either[String, Int])(
      (s: String) => s.length,
      (i: Int) => i * 2
    )
    assertEquals(result, Right(84))
  }

  test("Either bifunctor bimap with Left") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.bimap(Left("error"): Either[String, Int])(
      (s: String) => s.length,
      (i: Int) => i * 2
    )
    assertEquals(result, Left(5))
  }

  test("Either bifunctor leftMap") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.leftMap(Left("error"): Either[String, Int])(_.toUpperCase)
    assertEquals(result, Left("ERROR"))
  }

  test("Either bifunctor leftMap with Right") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.leftMap(Right(42): Either[String, Int])(_.toUpperCase)
    assertEquals(result, Right(42))
  }

  test("Either bifunctor rightMap") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.rightMap(Right(21): Either[String, Int])(_ * 2)
    assertEquals(result, Right(42))
  }

  test("Either bifunctor rightMap with Left") {
    val bf     = summon[Bifunctor[Either]]
    val result = bf.rightMap(Left("error"): Either[String, Int])(_ * 2)
    assertEquals(result, Left("error"))
  }

  test("Either bifunctor identity law") {
    val bf     = summon[Bifunctor[Either]]
    val either = Right(42): Either[String, Int]
    assertEquals(
      bf.bimap(either)((s: String) => s, (i: Int) => i),
      either
    )
  }

  test("Tuple2 bifunctor bimap") {
    val bf     = summon[Bifunctor[Tuple2]]
    val result = bf.bimap(("hello", 42))(_.length, _ * 2)
    assertEquals(result, (5, 84))
  }

  test("Tuple2 bifunctor leftMap") {
    val bf     = summon[Bifunctor[Tuple2]]
    val result = bf.leftMap(("hello", 42))(_.toUpperCase)
    assertEquals(result, ("HELLO", 42))
  }

  test("Tuple2 bifunctor rightMap") {
    val bf     = summon[Bifunctor[Tuple2]]
    val result = bf.rightMap(("hello", 21))(_ * 2)
    assertEquals(result, ("hello", 42))
  }

  test("Tuple2 bifunctor identity law") {
    val bf    = summon[Bifunctor[Tuple2]]
    val tuple = ("hello", 42)
    assertEquals(
      bf.bimap(tuple)((s: String) => s, (i: Int) => i),
      tuple
    )
  }

  test("Bifunctor composition law with Either") {
    val bf     = summon[Bifunctor[Either]]
    val either = Right(10): Either[String, Int]
    val f1     = (s: String) => s.length
    val f2     = (n: Int) => n.toString
    val g1     = (i: Int) => i * 2
    val g2     = (i: Int) => i + 1

    assertEquals(
      bf.bimap(bf.bimap(either)(f1, g1))(f2, g2),
      bf.bimap(either)(f1.andThen(f2), g1.andThen(g2))
    )
  }

  test("Bifunctor apply summons instance") {
    val bf     = Bifunctor[Either]
    val result = bf.bimap(Right(42): Either[String, Int])(
      (s: String) => s.length,
      (i: Int) => i * 2
    )
    assertEquals(result, Right(84))
  }

}
