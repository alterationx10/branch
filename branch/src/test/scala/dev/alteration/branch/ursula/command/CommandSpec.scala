package dev.alteration.branch.ursula.command

import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.ursula.args.*
import dev.alteration.branch.ursula.extensions.Extensions.splitSeq
import munit.*

import scala.concurrent.ExecutionContext

// A <-> B Conflict
// C requires an argument
// D is a required flag

object AFlag extends BooleanFlag {
  override val description: String = "A flag"
  override val name: String        = "aaa"
  override val shortKey: String    = "a"

  override val exclusive: Option[Seq[Flag[?]]] = Some(Seq(BFlag))

}

object BFlag extends BooleanFlag {
  override val description: String = "B flag"
  override val name: String        = "bbb"
  override val shortKey: String    = "b"

  override val exclusive: Option[Seq[Flag[?]]] = Some(Seq(AFlag))
}

object CFlag extends StringFlag {
  override val description: String = "C flag"
  override val name: String        = "ccc"
  override val shortKey: String    = "c"

}

object DFlag extends BooleanFlag {
  override val description: String = "D flag"
  override val name: String        = "ddd"
  override val shortKey: String    = "d"
  override val required: Boolean   = true

}

trait TestCommand extends Command {

  override def action(args: Seq[String]): Unit = ()

  val arguments: Seq[Argument[?]] = Seq.empty
  val description: String         = ""
  val examples: Seq[String]       = Seq.empty
  val flags: Seq[Flag[?]]         =
    Seq(AFlag, BFlag, CFlag, DFlag)
  val trigger: String             = "test"
  val usage: String               = "Used in a test"
}

object TestCommand extends TestCommand

object NonStrictTestCommand extends TestCommand {
  override val strict: Boolean = false
}

class CommandSpec extends FunSuite {
  given ExecutionContext = BranchExecutors.executionContext

  val goodCommand: Seq[String] = "-a -d -c 123".splitSeq()
  val missingFlag: Seq[String] = "-c 123".splitSeq()
  val unknownArg: Seq[String]  = "-f".splitSeq()
  val conflicting: Seq[String] = "-a -b -d".splitSeq()
  val help: Seq[String]        = "-a -b -d -f -h".splitSeq()

  test("should succeed when flags are given correctly") {
    assert(
      TestCommand.lazyAction(goodCommand).runSync().isSuccess
    )
  }

  test("should succeed when help flag is given") {
    assert(
      TestCommand.lazyAction(help).runSync().isSuccess
    )
  }

  test("should fail when missing a required flag") {
    assert(
      TestCommand.lazyAction(missingFlag).runSync().isFailure
    )
  }

  test("should fail when given an unknown flag") {
    assert(
      TestCommand.lazyAction(unknownArg).runSync().isFailure
    )
  }

  test("should fail when given conflicting flags") {
    assert(
      TestCommand.lazyAction(conflicting).runSync().isFailure
    )
  }

  test(
    "should not fail on unknown/conflicting/missing flags if non-strict"
  ) {
    assert(
      NonStrictTestCommand.lazyAction(unknownArg).runSync().isSuccess
    )
    assert(
      NonStrictTestCommand.lazyAction(conflicting).runSync().isSuccess
    )
    assert(
      NonStrictTestCommand.lazyAction(missingFlag).runSync().isSuccess
    )
  }

}
