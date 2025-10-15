package dev.alteration.branch.ursula.args

import munit.*

import java.nio.file.Path

class FlagsSpec extends FunSuite {

  test("Flags.string creates a StringFlag with correct properties") {
    val flag = Flags.string(
      name = "output",
      shortKey = "o",
      description = "Output file",
      default = Some("out.txt")
    )

    assertEquals(flag.name, "output")
    assertEquals(flag.shortKey, "o")
    assertEquals(flag.description, "Output file")
    assertEquals(flag.default, Some("out.txt"))
    assertEquals(flag.expectsArgument, true)
  }

  test("Flags.string parses arguments correctly") {
    val flag = Flags.string("test", "t", "Test flag")

    assertEquals(flag.parseFirstArg(Seq("-t", "value")), Some("value"))
    assertEquals(flag.parseFirstArg(Seq("--test", "other")), Some("other"))
  }

  test("Flags.string respects options when provided") {
    val flag = Flags.string(
      "env",
      "e",
      "Environment",
      options = Some(Set("dev", "prod"))
    )

    assertEquals(flag.parseFirstArg(Seq("-e", "dev")), Some("dev"))

    intercept[IllegalArgumentException] {
      flag.parseFirstArg(Seq("-e", "staging"))
    }
  }

  test("Flags.int creates an IntFlag with correct properties") {
    val flag = Flags.int(
      name = "port",
      shortKey = "p",
      description = "Port number",
      default = Some(8080)
    )

    assertEquals(flag.name, "port")
    assertEquals(flag.shortKey, "p")
    assertEquals(flag.description, "Port number")
    assertEquals(flag.default, Some(8080))
  }

  test("Flags.int parses integer arguments") {
    val flag = Flags.int("count", "c", "Count")

    assertEquals(flag.parseFirstArg(Seq("-c", "42")), Some(42))
    assertEquals(flag.parseFirstArg(Seq("--count", "100")), Some(100))
  }

  test("Flags.int uses default when not provided") {
    val flag = Flags.int("port", "p", "Port", default = Some(3000))

    assertEquals(flag.parseFirstArg(Seq.empty), Some(3000))
  }

  test("Flags.boolean creates a BooleanFlag") {
    val flag = Flags.boolean(
      name = "verbose",
      shortKey = "v",
      description = "Verbose output"
    )

    assertEquals(flag.name, "verbose")
    assertEquals(flag.shortKey, "v")
    assertEquals(flag.description, "Verbose output")
    assertEquals(flag.expectsArgument, false)
  }

  test("Flags.boolean detects presence correctly") {
    val flag = Flags.boolean("debug", "d", "Debug mode")

    assertEquals(flag.isPresent(Seq("-d")), true)
    assertEquals(flag.isPresent(Seq("--debug")), true)
    assertEquals(flag.isPresent(Seq.empty), false)
  }

  test("Flags.path creates a Path flag with correct properties") {
    val flag = Flags.path(
      name = "dir",
      shortKey = "d",
      description = "Directory path",
      default = Some(Path.of("./default"))
    )

    assertEquals(flag.name, "dir")
    assertEquals(flag.shortKey, "d")
    assertEquals(flag.description, "Directory path")
    assertEquals(flag.default, Some(Path.of("./default")))
  }

  test("Flags.path parses paths correctly") {
    val flag = Flags.path("input", "i", "Input path")

    assertEquals(
      flag.parseFirstArg(Seq("-i", "/tmp/test")),
      Some(Path.of("/tmp/test"))
    )
  }

  test("Flags.path supports custom parsers") {
    val flag = Flags.path(
      name = "workdir",
      shortKey = "w",
      description = "Working directory",
      parser = s => Path.of("/base").resolve(s)
    )

    assertEquals(
      flag.parseFirstArg(Seq("-w", "subdir")),
      Some(Path.of("/base/subdir"))
    )
  }

  test("Flags.custom creates a custom typed flag") {
    case class Email(value: String)

    val flag = Flags.custom[Email](
      name = "email",
      shortKey = "e",
      description = "Email address",
      parser = s => Email(s.toLowerCase)
    )

    assertEquals(flag.name, "email")
    assertEquals(
      flag.parseFirstArg(Seq("-e", "USER@EXAMPLE.COM")),
      Some(Email("user@example.com"))
    )
  }

  test("Flags.custom supports all flag properties") {
    val flag = Flags.custom[Int](
      name = "score",
      shortKey = "s",
      description = "Score",
      parser = _.toInt * 10,
      default = Some(100),
      required = true,
      options = Some(Set(100, 200, 300))
    )

    assertEquals(flag.required, true)
    assertEquals(flag.default, Some(100))
    assertEquals(flag.parseFirstArg(Seq("-s", "10")), Some(100))

    intercept[IllegalArgumentException] {
      flag.parseFirstArg(Seq("-s", "5")) // results in 50, not in options
    }
  }
}
