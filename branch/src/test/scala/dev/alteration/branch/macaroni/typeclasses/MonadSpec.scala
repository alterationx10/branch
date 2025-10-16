package dev.alteration.branch.macaroni.typeclasses

import munit.*

class MonadSpec extends FunSuite {

  test("Option monad pure") {
    val m = summon[Monad[Option]]
    assertEquals(m.pure(42), Some(42))
  }

  test("Option monad flatMap with Some") {
    val m      = summon[Monad[Option]]
    val result = m.flatMap(Some(21))(x => Some(x * 2))
    assertEquals(result, Some(42))
  }

  test("Option monad flatMap with None") {
    val m      = summon[Monad[Option]]
    val result = m.flatMap(None: Option[Int])(x => Some(x * 2))
    assertEquals(result, None)
  }

  test("Option monad flatMap returning None") {
    val m      = summon[Monad[Option]]
    val result = m.flatMap(Some(21))(_ => None: Option[Int])
    assertEquals(result, None)
  }

  test("Option monad left identity law") {
    val m = summon[Monad[Option]]
    val f = (x: Int) => Some(x * 2)
    val a = 21
    assertEquals(m.flatMap(m.pure(a))(f), f(a))
  }

  test("Option monad right identity law") {
    val m  = summon[Monad[Option]]
    val fa = Some(42)
    assertEquals(m.flatMap(fa)(m.pure), fa)
  }

  test("Option monad associativity law") {
    val m  = summon[Monad[Option]]
    val fa = Some(5)
    val f  = (x: Int) => Some(x * 2)
    val g  = (x: Int) => Some(x + 10)
    assertEquals(
      m.flatMap(m.flatMap(fa)(f))(g),
      m.flatMap(fa)(x => m.flatMap(f(x))(g))
    )
  }

  test("List monad pure") {
    val m = summon[Monad[List]]
    assertEquals(m.pure(42), List(42))
  }

  test("List monad flatMap") {
    val m      = summon[Monad[List]]
    val result = m.flatMap(List(1, 2, 3))(x => List(x, x * 10))
    assertEquals(result, List(1, 10, 2, 20, 3, 30))
  }

  test("List monad flatMap with empty list") {
    val m      = summon[Monad[List]]
    val result = m.flatMap(List.empty[Int])(x => List(x * 2))
    assertEquals(result, List.empty[Int])
  }

  test("Vector monad pure") {
    val m = summon[Monad[Vector]]
    assertEquals(m.pure(42), Vector(42))
  }

  test("Vector monad flatMap") {
    val m      = summon[Monad[Vector]]
    val result = m.flatMap(Vector(1, 2, 3))(x => Vector(x, x * 10))
    assertEquals(result, Vector(1, 10, 2, 20, 3, 30))
  }

  test("Either monad pure") {
    val m = summon[Monad[[T] =>> Either[String, T]]]
    assertEquals(m.pure(42), Right(42))
  }

  test("Either monad flatMap with Right") {
    val m      = summon[Monad[[T] =>> Either[String, T]]]
    val result = m.flatMap(Right(21): Either[String, Int])(x => Right(x * 2))
    assertEquals(result, Right(42))
  }

  test("Either monad flatMap with Left") {
    val m      = summon[Monad[[T] =>> Either[String, T]]]
    val result =
      m.flatMap(Left("error"): Either[String, Int])(x => Right(x * 2))
    assertEquals(result, Left("error"))
  }

  test("Either monad flatMap returning Left") {
    val m      = summon[Monad[[T] =>> Either[String, T]]]
    val result = m.flatMap(Right(21): Either[String, Int])(_ => Left("error"))
    assertEquals(result, Left("error"))
  }

  test("Monad flatten") {
    val m      = summon[Monad[Option]]
    val nested = Some(Some(42))
    assertEquals(m.flatten(nested), Some(42))
  }

  test("Monad flatten with None outer") {
    val m      = summon[Monad[Option]]
    val nested = None: Option[Option[Int]]
    assertEquals(m.flatten(nested), None)
  }

  test("Monad flatten with None inner") {
    val m      = summon[Monad[Option]]
    val nested = Some(None: Option[Int])
    assertEquals(m.flatten(nested), None)
  }

  test("Monad map inherited from Applicative") {
    val m      = summon[Monad[Option]]
    val result = m.map(Some(21))(_ * 2)
    assertEquals(result, Some(42))
  }

  test("Monad apply summons instance") {
    val m = Monad[Option]
    assertEquals(m.pure(42), Some(42))
  }

  test("Monad chaining multiple flatMaps") {
    val m      = summon[Monad[Option]]
    val result = m.flatMap(Some(10)) { x =>
      m.flatMap(Some(20)) { y =>
        m.pure(x + y)
      }
    }
    assertEquals(result, Some(30))
  }

}
