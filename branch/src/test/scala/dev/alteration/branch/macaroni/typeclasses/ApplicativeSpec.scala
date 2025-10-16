package dev.alteration.branch.macaroni.typeclasses

import munit.*

class ApplicativeSpec extends FunSuite {

  test("Option applicative pure") {
    val ap = summon[Applicative[Option]]
    assertEquals(ap.pure(42), Some(42))
  }

  test("Option applicative ap with Some") {
    val ap = summon[Applicative[Option]]
    val ff = Some((x: Int) => x * 2)
    val fa = Some(21)
    assertEquals(ap.ap(ff)(fa), Some(42))
  }

  test("Option applicative ap with None function") {
    val ap = summon[Applicative[Option]]
    val ff = None: Option[Int => Int]
    val fa = Some(21)
    assertEquals(ap.ap(ff)(fa), None)
  }

  test("Option applicative ap with None value") {
    val ap = summon[Applicative[Option]]
    val ff = Some((x: Int) => x * 2)
    val fa = None: Option[Int]
    assertEquals(ap.ap(ff)(fa), None)
  }

  test("Option applicative identity law") {
    val ap = summon[Applicative[Option]]
    val fa = Some(42)
    assertEquals(ap.ap(ap.pure((x: Int) => x))(fa), fa)
  }

  test("Option applicative homomorphism law") {
    val ap = summon[Applicative[Option]]
    val f  = (x: Int) => x * 2
    val a  = 21
    assertEquals(ap.ap(ap.pure(f))(ap.pure(a)), ap.pure(f(a)))
  }

  test("List applicative pure") {
    val ap = summon[Applicative[List]]
    assertEquals(ap.pure(42), List(42))
  }

  test("List applicative ap") {
    val ap = summon[Applicative[List]]
    val ff = List((x: Int) => x * 2, (x: Int) => x + 10)
    val fa = List(1, 2, 3)
    assertEquals(ap.ap(ff)(fa), List(2, 4, 6, 11, 12, 13))
  }

  test("Vector applicative pure") {
    val ap = summon[Applicative[Vector]]
    assertEquals(ap.pure(42), Vector(42))
  }

  test("Vector applicative ap") {
    val ap = summon[Applicative[Vector]]
    val ff = Vector((x: Int) => x * 2)
    val fa = Vector(10, 20, 30)
    assertEquals(ap.ap(ff)(fa), Vector(20, 40, 60))
  }

  test("Either applicative pure") {
    val ap = summon[Applicative[[T] =>> Either[String, T]]]
    assertEquals(ap.pure(42), Right(42))
  }

  test("Either applicative ap with Right") {
    val ap = summon[Applicative[[T] =>> Either[String, T]]]
    val ff = Right((x: Int) => x * 2): Either[String, Int => Int]
    val fa = Right(21): Either[String, Int]
    assertEquals(ap.ap(ff)(fa), Right(42))
  }

  test("Either applicative ap with Left function") {
    val ap = summon[Applicative[[T] =>> Either[String, T]]]
    val ff = Left("error"): Either[String, Int => Int]
    val fa = Right(21): Either[String, Int]
    assertEquals(ap.ap(ff)(fa), Left("error"))
  }

  test("Either applicative ap with Left value") {
    val ap = summon[Applicative[[T] =>> Either[String, T]]]
    val ff = Right((x: Int) => x * 2): Either[String, Int => Int]
    val fa = Left("error"): Either[String, Int]
    assertEquals(ap.ap(ff)(fa), Left("error"))
  }

  test("Applicative map2") {
    val ap     = summon[Applicative[Option]]
    val result = ap.map2(Some(2), Some(3))(_ + _)
    assertEquals(result, Some(5))
  }

  test("Applicative map2 with None") {
    val ap     = summon[Applicative[Option]]
    val result = ap.map2(None: Option[Int], Some(3))(_ + _)
    assertEquals(result, None)
  }

  test("Applicative map3") {
    val ap     = summon[Applicative[Option]]
    val result = ap.map3(Some(2), Some(3), Some(4))(_ + _ + _)
    assertEquals(result, Some(9))
  }

  test("Applicative map3 with None") {
    val ap     = summon[Applicative[Option]]
    val result = ap.map3(Some(2), None: Option[Int], Some(4))(_ + _ + _)
    assertEquals(result, None)
  }

  test("Applicative map inherited from Functor") {
    val ap     = summon[Applicative[Option]]
    val result = ap.map(Some(21))(_ * 2)
    assertEquals(result, Some(42))
  }

  test("Applicative apply summons instance") {
    val ap = Applicative[Option]
    assertEquals(ap.pure(42), Some(42))
  }

}
