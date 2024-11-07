package dev.wishingtree.branch.spider
import dev.wishingtree.branch.spider.OpaqueSegments.*

import java.io.File

case class FileContext(rootFilePath: Segments, rootPath: String = "/")
    extends ContextHandler(rootPath) {

  private def fileExists(path: Segments) = {
    val filePath = (rootFilePath / path).toPathString
    val file = new File(filePath)
    file.exists() && file.isFile
  }

  private val fileHandler: FileHandler = 
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpVerb, Segments), RequestHandler[_, _]] = {
    case HttpVerb.GET -> anyPath if fileExists(anyPath) => fileHandler
  }
}
