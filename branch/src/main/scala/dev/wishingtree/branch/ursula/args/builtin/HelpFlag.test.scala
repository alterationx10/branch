package dev.wishingtree.branch.ursula.args.builtin

import munit.*

class HelpFlagSpec extends munit.FunSuite {

  val presentArgs: Seq[String] = Seq(
    "-h",
    "--help",
    "command -h",
    "command --help",
    "command arg1 arg2 --help"
  )

  val missingArgs: Seq[String] = Seq(
    "command h help arg1 arg2",
    "arg1 arg2"
  )

  test("HelpFlag.name") {
    assertEquals(HelpFlag.name, "help")
  }

  test("HelpFlag.shortKey") {
    assertEquals(HelpFlag.shortKey, "h")
  }

  test("HelpFlag.isBoolean") {
    assertEquals(HelpFlag.expectsArgument, false)
  }

  test("HelpFlag is not exclusive") {
    assert(HelpFlag.exclusive.isEmpty || HelpFlag.exclusive.exists(_.isEmpty))
  }

  test("HelpFlag has no dependencies") {
    assert(HelpFlag.dependsOn.isEmpty || HelpFlag.dependsOn.exists(_.isEmpty))
  }

  test("HelpFlag is present") {
    assert(HelpFlag.isPresent(presentArgs))
    assert(!HelpFlag.isPresent(missingArgs))
  }

}
