package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.spider.*
import dev.wishingtree.branch.spider.server.OpaqueSegments.Segments

import java.io.File

object FileContextHandler {

  private[spider] val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )
}

case class FileContextHandler(rootFilePath: Segments, contextPath: String = "/")
    extends ContextHandler(contextPath) {

  private def fileExists(path: Segments): Boolean = {
    val filePath = (rootFilePath / path).toPathString
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  private def defaultExists(path: Segments): Boolean = {
    !path.toSeq.lastOption.exists(_.contains("\\.")) &&
    FileContextHandler.defaultFiles.foldLeft(false) { (b, d) =>
      val file = new File((rootFilePath / path / d).toPathString)
      b || (file.exists() && file.isFile)
    }
  }

  private def defaultFile(path: Segments): File =
    FileContextHandler.defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toPathString))
      .find(_.exists())
      .getOrElse(throw new Exception("Not found"))

  private val fileHandler: FileHandler =
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpVerb, Segments), RequestHandler[?, ?]] = {
    case HttpVerb.GET -> anyPath if fileExists(anyPath)    => fileHandler
    case HttpVerb.GET -> anyPath if defaultExists(anyPath) =>
      DefaultFilerHandler(defaultFile(anyPath))
  }
}
