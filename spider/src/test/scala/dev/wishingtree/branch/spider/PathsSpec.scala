package dev.wishingtree.branch.spider

import dev.wishingtree.branch.spider.OpaqueSegments.*
import munit.FunSuite

class PathsSpec extends FunSuite {

  test("appendStr") {
    assertEquals(
      >> / "a" / "b",
      Segments("a/b")
    )
  }

  test("appendPath") {
    assertEquals(
      Segments("a/b") / Segments("c"),
      Segments("a/b/c")
    )
  }

  test("string interpolation") {
    assertEquals(
      p"a/b",
      Segments("a/b")
    )
  }

  test("partial function matching") {

    val pf: PartialFunction[Segments, String] = {
      // `
      case >> / "a" / "b" / s"$arg" => arg
    }

    assert(pf(p"a/b/123") == "123")
    assert(!pf.isDefinedAt(p"a/b/123/xyz"))
    assert(!pf.isDefinedAt(p"a/b/"))

  }

}
