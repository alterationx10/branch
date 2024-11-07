package dev.wishingtree.branch.spider
import dev.wishingtree.branch.spider.Paths.*

import java.io.File

case class FileContext(rootFilePath: Path, rootPath: String = "/")
    extends ContextHandler(rootPath) {

  private def fileExists(path: Path) = {
    val file = new File((rootFilePath / path).toString)
    file.exists() && file.isFile
  }

  private val fileHandler: FileHandler = 
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpVerb, Path), RequestHandler[_, _]] = {
    case HttpVerb.GET -> anyPath if fileExists(anyPath) => fileHandler
  }
}
