package dev.wishingtree.branch.spider

import dev.wishingtree.branch.spider.Paths.*
import munit.FunSuite

class PathsSpec extends FunSuite {

  test("appendStr") {
    assertEquals(
      >> / "a" / "b",
      Path("a/b")
    )
  }

  test("appendPath") {
    assertEquals(
      Path("a/b") / Path("c"),
      Path("a/b/c")
    )
  }

  test("string interpolation") {
    assertEquals(
      p"a/b",
      Path("a/b")
    )
  }

  test("partial function matching") {

    val pf: PartialFunction[Path, String] = {
      // `
      case >> / "a" / "b" / s"$arg" => arg
    }

    assert(pf(p"a/b/123") == "123")
    assert(!pf.isDefinedAt(p"a/b/123/xyz"))
    assert(!pf.isDefinedAt(p"a/b/"))

  }

}
