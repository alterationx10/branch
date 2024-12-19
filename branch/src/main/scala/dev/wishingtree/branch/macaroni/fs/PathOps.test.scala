package dev.wishingtree.branch.macaroni.fs

import dev.wishingtree.branch.macaroni.fs.PathOps.*

import java.nio.file.*
import munit.*

class PathOpsSpec extends FunSuite {

  test("appendStr") {
    assertEquals(
      >> / "a" / "b",
      Path.of("a/b")
    )
  }

  test("appendPath") {
    assertEquals(
      Path.of("a/b") / Path.of("c"),
      Path.of("a/b/c")
    )
  }

  test("string interpolation") {
    assertEquals(
      p"a/b",
      Path.of("a/b")
    )
  }

  test("partial function matching") {

    val pf: PartialFunction[Path, String] = {
      case >> / "a" / "b" / s"$arg" => arg
      case >>                       => "root"
    }

    assertEquals(pf(p"a/b/123"), "123")
    assert(!pf.isDefinedAt(p"a/b/123/xyz"))
    assert(!pf.isDefinedAt(p"a/b/"))
    assert(pf(p"") == "root")

  }

  test("case insensitive matching") {

    val pf: PartialFunction[Path, String] = {
      case >> / ci"this" / ci"that" / s"$arg" => arg
    }

    assertEquals(pf(p"ThIs/tHaT/123"), "123")

  }

}
