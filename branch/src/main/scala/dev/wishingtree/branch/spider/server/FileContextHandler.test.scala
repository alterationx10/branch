package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.macaroni.fs.PathOps.*

class FileContextHandlerSpec extends munit.FunSuite {

  // This test requires the book to be built
  // cd site
  // mdbook build
  // TODO: Set up a Fixture to stage files
  test("FileContextHandler") {
    val staticFilesPath = wd / "site" / "book"
    val files           = FileContextHandler(staticFilesPath)

    assert(files.contextRouter.isDefinedAt((HttpMethod.GET, >> / "index.html")))
    assert(files.contextRouter.isDefinedAt((HttpMethod.GET, >>)))
    assert(
      files.contextRouter.isDefinedAt(
        (HttpMethod.GET, >> / "css" / "general.css")
      )
    )
  }
}
