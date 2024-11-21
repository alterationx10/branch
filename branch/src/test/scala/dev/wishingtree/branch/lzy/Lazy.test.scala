package dev.wishingtree.branch.lzy

import munit.FunSuite

class LazySpec extends FunSuite {

  override def munitValueTransforms = super.munitValueTransforms ++ List(
    new ValueTransform(
      "Lazy",
      { case lzy: Lazy[?] =>
        LazyRuntime.runAsync(lzy)
      }
    )
  )

  test("Lazy.fn") {
    for {
      l <- Lazy.fn("abc")
    } yield assertEquals(l, "abc")
  }

  test("Lazy.flatMap") {
    for {
      l <- Lazy.fn("abc").flatMap(a => Lazy.fn(a + "def"))
    } yield assertEquals(l, "abcdef")
  }

  test("Lazy.map") {
    for {
      l <- Lazy.fn("abc").map(a => a + "def")
    } yield assertEquals(l, "abcdef")
  }

  test("Lazy.flatten") {
    for {
      l <- Lazy.fn(Lazy.fn("abc")).flatten
    } yield assertEquals(l, "abc")
  }

  test("Lazy.recover") {
    for {
      l <- Lazy.fail(new Exception("error")).recover(_ => Lazy.fn("abc"))
    } yield assertEquals(l, "abc")
  }

  test("Lazy.orElse") {
    for {
      l <- Lazy.fail(new Exception("error")).orElse(Lazy.fn("abc"))
    } yield assertEquals(l, "abc")
  }

  test("Lazy.orElseValue") {
    for {
      l <- Lazy.fail(new Exception("error")).orElseValue("abc")
    } yield assertEquals(l, "abc")
  }

}
