package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpVerb
import dev.wishingtree.branch.spider.server.OpaqueSegments.{>>, Segments}

class FileContextHandlerSpec extends munit.FunSuite {

  // This test requires the book to be built
  // cd site
  // mdbook build
  // TODO: Set up a Fixture to stage files
  test("FileContextHandler") {
    val staticFilesPath = Segments.wd / "site" / "book"
    val files           = FileContextHandler(staticFilesPath)

    assert(files.contextRouter.isDefinedAt((HttpVerb.GET, >> / "index.html")))
    assert(files.contextRouter.isDefinedAt((HttpVerb.GET, >>)))
    assert(
      files.contextRouter.isDefinedAt(
        (HttpVerb.GET, >> / "css" / "general.css")
      )
    )
  }
}
