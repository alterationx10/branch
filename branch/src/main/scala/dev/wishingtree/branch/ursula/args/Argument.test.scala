package dev.wishingtree.branch.ursula.args

import munit.*

class ArgumentSpec extends FunSuite {

  trait BaseArg extends Argument[Int] {
    val name                                = "base"
    val description                         = "base description"
    def parse: PartialFunction[String, Int] = { case s => s.toInt }
  }

  object TestArgOptions extends BaseArg {
    override val options: Option[Set[Int]] = Some(Set(1, 2))
  }

  object TestArgDefault extends BaseArg {
    override val default: Option[Int] = Some(1)
  }

  test("Argument options") {
    assertEquals(TestArgOptions.valueOrDefault(Some("1")), 1)

    intercept[IllegalArgumentException](
      TestArgOptions.valueOrDefault(Some("3"))
    )

    intercept[IllegalArgumentException](
      TestArgOptions.valueOrDefault(None)
    )

  }

  test("Argument default") {
    assertEquals(TestArgDefault.valueOrDefault(Some("1")), 1)
    assertEquals(TestArgDefault.valueOrDefault(None), 1)
  }

}
