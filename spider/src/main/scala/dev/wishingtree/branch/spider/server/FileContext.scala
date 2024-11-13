package dev.wishingtree.branch.spider.server

import OpaqueSegments.*
import dev.wishingtree.branch.spider.*

import java.io.File

object FileContext {

  private[spider] val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )
}

case class FileContext(rootFilePath: Segments, rootPath: String = "/")
    extends ContextHandler(rootPath) {

  private def fileExists(path: Segments): Boolean = {
    val filePath = (rootFilePath / path).toPathString
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  private def defaultExists(path: Segments): Boolean = {
    !path.toSeq.lastOption.exists(_.contains("\\.")) &&
    FileContext.defaultFiles.foldLeft(false) { (b, d) =>
      val file = new File((rootFilePath / path / d).toPathString)
      b || (file.exists() && file.isFile)
    }
  }

  private def defaultFile(path: Segments): File =
    FileContext.defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toPathString))
      .find(_.exists())
      .getOrElse(throw new Exception("Not found"))

  private val fileHandler: FileHandler =
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpVerb, Segments), RequestHandler[_, _]] = {
    case HttpVerb.GET -> anyPath if fileExists(anyPath)    => fileHandler
    case HttpVerb.GET -> anyPath if defaultExists(anyPath) =>
      DefaultFilerHandler(defaultFile(anyPath))
  }
}
