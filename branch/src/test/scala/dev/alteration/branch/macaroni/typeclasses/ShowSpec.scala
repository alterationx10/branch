package dev.alteration.branch.macaroni.typeclasses

import munit.*

class ShowSpec extends FunSuite {

  test("String show") {
    val s = summon[Show[String]]
    assertEquals(s.show("hello"), "hello")
  }

  test("Int show") {
    val s = summon[Show[Int]]
    assertEquals(s.show(42), "42")
    assertEquals(s.show(-10), "-10")
    assertEquals(s.show(0), "0")
  }

  test("Long show") {
    val s = summon[Show[Long]]
    assertEquals(s.show(1000L), "1000")
    assertEquals(s.show(-500L), "-500")
  }

  test("Double show") {
    val s = summon[Show[Double]]
    assertEquals(s.show(3.14), "3.14")
    assertEquals(s.show(-2.71), "-2.71")
  }

  test("Boolean show") {
    val s = summon[Show[Boolean]]
    assertEquals(s.show(true), "true")
    assertEquals(s.show(false), "false")
  }

  test("List show with empty list") {
    val s = summon[Show[List[Int]]]
    assertEquals(s.show(List.empty), "[]")
  }

  test("List show with elements") {
    val s = summon[Show[List[Int]]]
    assertEquals(s.show(List(1, 2, 3)), "[1, 2, 3]")
  }

  test("List show with nested lists") {
    val s = summon[Show[List[String]]]
    assertEquals(s.show(List("hello", "world")), "[hello, world]")
  }

  test("Option show with Some") {
    val s = summon[Show[Option[Int]]]
    assertEquals(s.show(Some(42)), "Some(42)")
  }

  test("Option show with None") {
    val s = summon[Show[Option[Int]]]
    assertEquals(s.show(None), "None")
  }

  test("Option show with String value") {
    val s = summon[Show[Option[String]]]
    assertEquals(s.show(Some("hello")), "Some(hello)")
  }

  test("Either show with Left") {
    val s = summon[Show[Either[String, Int]]]
    assertEquals(s.show(Left("error")), "Left(error)")
  }

  test("Either show with Right") {
    val s = summon[Show[Either[String, Int]]]
    assertEquals(s.show(Right(42)), "Right(42)")
  }

  test("Either show with nested types") {
    val s = summon[Show[Either[Int, String]]]
    assertEquals(s.show(Left(404)), "Left(404)")
    assertEquals(s.show(Right("success")), "Right(success)")
  }

  test("Show apply summons instance") {
    val s = Show[Int]
    assertEquals(s.show(100), "100")
  }

  test("Show fromToString constructor") {
    case class Person(name: String, age: Int)
    given Show[Person] = Show.fromToString[Person]

    val person = Person("Alice", 30)
    assertEquals(Show[Person].show(person), "Person(Alice,30)")
  }

  test("Show custom implementation") {
    case class Point(x: Int, y: Int)
    given Show[Point] with {
      def show(p: Point): String = s"(${p.x}, ${p.y})"
    }

    val point = Point(3, 4)
    assertEquals(Show[Point].show(point), "(3, 4)")
  }

  test("Show nested structures") {
    val s    = summon[Show[List[Option[Int]]]]
    val data = List(Some(1), None, Some(3))
    assertEquals(s.show(data), "[Some(1), None, Some(3)]")
  }

  test("Show complex nested Either") {
    val s = summon[Show[Option[Either[String, Int]]]]
    assertEquals(s.show(Some(Right(42))), "Some(Right(42))")
    assertEquals(s.show(Some(Left("error"))), "Some(Left(error))")
    assertEquals(s.show(None), "None")
  }

}
