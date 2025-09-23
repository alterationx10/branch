package dev.alteration.branch.ursula.args

import munit.FunSuite

class FlagSpec extends FunSuite {

  // NOTE: This relies on the contents .env to be loaded
  trait TestFlag extends StringFlag {
    override val description: String = "A flag for testing"
    override val name: String        = "override"
    override val shortKey: String    = "t"
  }

  // No def, no env
  case object TestFlag extends TestFlag

  // No def, env present
  case object TestFlagEnv extends TestFlag {
    override val env: Option[String] = Option("TEST_FLAG")
  }

  // No def, env not present
  case object TestFlagEnv2 extends TestFlag {
    override val env: Option[String] = Option("NOT_PRESENT")
  }

  // def, no env
  case object TestFlagDef extends TestFlag {
    override val default: Option[String] = Option("123")
  }

  // Has both def, env
  case object TestFlagEnvDef extends TestFlag {
    override val default: Option[String] = Option("123")
    override val env: Option[String]     = Option("TEST_FLAG")

  }

  // Has both def, env - env not present
  case object TestFlagEnvDef2 extends TestFlag {
    override val default: Option[String] = Option("123")
    override val env: Option[String]     = Option("NOT_PRESENT")

  }

  case object TestFlagOptions extends TestFlag {
    override val options: Option[Set[String]] = Option(Set("abc", "123"))
  }

  case object TestFlagOptionsEnv extends TestFlag {
    override val options: Option[Set[String]] = Option(Set("abc", "123"))
    override val env: Option[String]          = Option("BAD_OPTION_TEST_FLAG")
  }

  case object TestFlagOptionsDef extends TestFlag {
    override val options: Option[Set[String]] = Option(Set("abc", "123"))
    override val default: Option[String]      = Some("xyz")
  }

  val argsWithFlag: Seq[String] = "-l this -l that -t xyz".split(" ").toSeq
  val argsNoFlag: Seq[String]   = "-l this -l that".split(" ").toSeq

  test("Flag.parseFirstArg - args with flag") {
    assertEquals(TestFlag.parseFirstArg(argsWithFlag), Some("xyz"))
    assertEquals(TestFlagEnv.parseFirstArg(argsWithFlag), Some("xyz"))
    assertEquals(TestFlagEnv2.parseFirstArg(argsWithFlag), Some("xyz"))
    assertEquals(TestFlagDef.parseFirstArg(argsWithFlag), Some("xyz"))
    assertEquals(TestFlagEnvDef.parseFirstArg(argsWithFlag), Some("xyz"))
    assertEquals(TestFlagEnvDef2.parseFirstArg(argsWithFlag), Some("xyz"))
  }

  test("Flag.parseFirstArg - args without flag") {
    assertEquals(TestFlag.parseFirstArg(argsNoFlag), Option.empty[String])
    assertEquals(TestFlagEnv.parseFirstArg(argsNoFlag), Some("abc"))
    assertEquals(TestFlagEnv2.parseFirstArg(argsNoFlag), Option.empty[String])
    assertEquals(TestFlagDef.parseFirstArg(argsNoFlag), Some("123"))
    assertEquals(TestFlagEnvDef.parseFirstArg(argsNoFlag), Some("abc"))
    assertEquals(TestFlagEnvDef2.parseFirstArg(argsNoFlag), Some("123"))
  }

  test("Flag.parseArgs - args with flag") {
    assertEquals(TestFlag.parseArgs(argsWithFlag), Seq("xyz"))
    assertEquals(TestFlagEnv.parseArgs(argsWithFlag), Seq("xyz"))
    assertEquals(TestFlagEnv2.parseArgs(argsWithFlag), Seq("xyz"))
    assertEquals(TestFlagDef.parseArgs(argsWithFlag), Seq("xyz"))
    assertEquals(TestFlagEnvDef.parseArgs(argsWithFlag), Seq("xyz"))
    assertEquals(TestFlagEnvDef2.parseArgs(argsWithFlag), Seq("xyz"))
  }

  test("Flag.parseArgs - args without flag") {
    assertEquals(TestFlag.parseArgs(argsNoFlag), Seq.empty[String])
    assertEquals(TestFlagEnv.parseArgs(argsNoFlag), Seq("abc"))
    assertEquals(TestFlagEnv2.parseArgs(argsNoFlag), Seq.empty[String])
    assertEquals(TestFlagDef.parseArgs(argsNoFlag), Seq("123"))
    assertEquals(TestFlagEnvDef.parseArgs(argsNoFlag), Seq("abc"))
    assertEquals(TestFlagEnvDef2.parseArgs(argsNoFlag), Seq("123"))
  }

  test("Flag.parseArgs - only accepts valid options") {
    assertEquals(
      TestFlagOptions.parseArgs("-t 123 -t abc".split(" ").toSeq),
      Seq("123", "abc")
    )
    assertEquals(TestFlagOptions.parseArgs(Seq.empty), Seq.empty[String])
    intercept[IllegalArgumentException] {
      TestFlagOptions.parseArgs("-t xyz".split(" ").toSeq)
    }

    assertEquals(
      TestFlagOptionsEnv.parseArgs("-t 123 -t abc".split(" ").toSeq),
      Seq("123", "abc")
    )
    intercept[IllegalArgumentException] {
      TestFlagOptionsEnv.parseArgs(Seq.empty)
    }

    assertEquals(
      TestFlagOptionsDef.parseArgs("-l this -l that -t 123".split(" ").toSeq),
      Seq("123")
    )

    intercept[IllegalArgumentException] {
      // This should throw because the default is not in the options
      TestFlagOptionsDef.parseArgs(Seq.empty)
    }

  }

}
