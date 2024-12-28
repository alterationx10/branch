package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.HttpMethod
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.testkit.fixtures.FileFixtureSuite

class FileContextHandlerSpec extends FileFixtureSuite {

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

  fileWithSuffix(".html").test("FileContextHandler.defaultExists .html") {
    file =>
      val parent     = file.getParent
      val fileName   = file.relativeTo(parent).toString.stripSuffix(".html")
      val ctxHandler = FileContextHandler(parent)
      assert(ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".html").test("FileContextHandler.defaultFile .html") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".html")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultFile(parent / fileName).exists())
  }

  fileWithSuffix(".htm").test("FileContextHandler.defaultExists .htm") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".htm")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".htm").test("FileContextHandler.defaultFile .htm") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".htm")
    val ctxHandler = FileContextHandler(parent)
    assert(ctxHandler.defaultFile(parent / fileName).exists())
  }

  fileWithSuffix(".txt").test("FileContextHandler.defaultExists .txt") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".txt")
    val ctxHandler = FileContextHandler(parent)
    assert(!ctxHandler.defaultExists(parent / fileName))
  }

  fileWithSuffix(".txt").test("FileContextHandler.defaultExists .txt") { file =>
    val parent     = file.getParent
    val fileName   = file.relativeTo(parent).toString.stripSuffix(".txt")
    val ctxHandler = FileContextHandler(parent)
    intercept[IllegalArgumentException] {
      ctxHandler.defaultFile(parent / fileName)
    }
  }
}
