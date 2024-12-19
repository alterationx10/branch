package dev.wishingtree.branch.spider.server

import com.sun.net.httpserver.{Authenticator, Filter}
import dev.wishingtree.branch.spider.*
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import java.io.File
import java.nio.file.Path

object FileContextHandler {

  /** A list of default files to look for when a directory is requested. E.g.
    * /some/path -> /some/path/index.html
    */
  private[spider] val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )
}

/** A built-in context handler for serving files from the file system.
  */
case class FileContextHandler(
    rootFilePath: Path,
    contextPath: String = "/",
    override val filters: Seq[Filter] = Seq.empty,
    override val authenticator: Option[Authenticator] = Option.empty
) extends ContextHandler(contextPath) {

  private def fileExists(path: Path): Boolean = {
    val filePath = (rootFilePath / path).toPathString
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  private def defaultExists(path: Path): Boolean = {
    !path.toSeq.lastOption.exists(_.contains("\\.")) &&
    FileContextHandler.defaultFiles.foldLeft(false) { (b, d) =>
      val file = new File((rootFilePath / path / d).toPathString)
      b || (file.exists() && file.isFile)
    }
  }

  private def defaultFile(path: Path): File =
    FileContextHandler.defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toPathString))
      .find(_.exists())
      .getOrElse(throw new Exception("Not found"))

  private val fileHandler: FileHandler =
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpMethod, Path), RequestHandler[?, ?]] = {
    case HttpMethod.GET -> anyPath if fileExists(anyPath)    => fileHandler
    case HttpMethod.GET -> anyPath if defaultExists(anyPath) =>
      DefaultFileHandler(defaultFile(anyPath))
  }
}
