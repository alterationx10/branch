package dev.alteration.branch.veil

import munit.FunSuite

class VeilSpec extends FunSuite {

  test("Veil.get") {
    for {
      t1 <- Veil.get("THING_1")
    } yield assertEquals(t1, "abc")

    for {
      t2 <- Veil.get("THING_2")
    } yield assertEquals(t2, "123")

    for {
      t3 <- Veil.get("THING_3")
    } yield assertEquals(t3, "1+1=2")

    assert(Veil.get("USER").nonEmpty)
  }

}
