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

  test("toSeq splits path into segments") {
    assertEquals(p"a/b/c".toSeq, Seq("a", "b", "c"))
    assertEquals(p"".toSeq, Seq.empty)
    assertEquals(p"/a/b".toSeq, Seq("a", "b"))
    assertEquals(p"a//b".toSeq, Seq("a", "b"))
  }

  test("relativeTo with string root path") {
    assertEquals(p"a/b/c".relativeTo("a"), p"b/c")
    assertEquals(p"a/b/c".relativeTo("a/b"), p"c")
    assertEquals(p"/a/b/c".relativeTo("/a"), p"b/c")
  }

  test("relativeTo with Path root path") {
    assertEquals(p"a/b/c".relativeTo(p"a"), p"b/c")
    assertEquals(p"a/b/c".relativeTo(p"a/b"), p"c")
    assertEquals(p"/a/b/c".relativeTo(p"/a"), p"b/c")
  }

  test("null inputs throw IllegalArgumentException") {
    intercept[IllegalArgumentException] {
      p"test" / (null: String)
    }
    intercept[IllegalArgumentException] {
      p"test" / (null: Path)
    }
    intercept[IllegalArgumentException] {
      p"test".relativeTo(null: String)
    }
    intercept[IllegalArgumentException] {
      p"test".relativeTo(null: Path)
    }
  }

  test("wd returns absolute path") {
    assert(wd.isAbsolute)
  }

  test("path extractor edge cases") {
    val pf: PartialFunction[Path, String] = {
      case >> / "a" / ""   => "empty-segment" // This test shows the issue
      case >> / "" / "b"   => "leading-empty" // This test shows the issue
      case >> / "a" / "."  => "dot"
      case >> / "a" / ".." => "parent"
    }

    intercept[MatchError] {
      // These should match but don't because of the empty string handling
      // This is because the unapply calls toSeq which excludes empty strings
      pf(p"a/")
      pf(p"/b")
    }
    assertEquals(pf(p"a/."), "dot")
    assertEquals(pf(p"a/.."), "parent")
  }

}
