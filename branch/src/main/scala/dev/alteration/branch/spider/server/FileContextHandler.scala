package dev.alteration.branch.spider.server

import com.sun.net.httpserver.{Authenticator, Filter}
import dev.alteration.branch.spider.*

import java.io.File
import java.nio.file.{Files, Path}

object FileContextHandler {

  private def pathFromStrList(segments: List[String]): Path = {
    segments match {
      case Nil          => Path.of("")
      case head :: Nil  => Path.of(head)
      case head :: tail => tail.foldLeft(Path.of(head))(_ / _)
    }
  }

  extension (path: Path) {

    def /(segment: String | Path): Path = {
      if (segment == null)
        throw new IllegalArgumentException("Path segment cannot be null")
      segment match {
        case p: Path     => path.resolve(p)
        case str: String => path.resolve(str)
      }
    }

    def relativeTo(rootPath: Path): Path = {
      if (rootPath == null)
        throw new IllegalArgumentException("Root path cannot be null")
      rootPath.relativize(path)
    }

  }

  /** A list of default files to look for when a directory is requested. E.g.
    * /some/path -> /some/path/index.html
    */
  private[spider] val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )

  /** A list of default file endings to look for when a file is requested. E.g.
    * /some/path -> /some/path.html
    */
  private[spider] val defaultEndings: List[String] = List(
    ".html",
    ".htm"
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
  import FileContextHandler.*

  private def fileExists(path: Path): Boolean = {
    val filePath = (rootFilePath / path).toString
    println(s"Checking for file at $filePath")
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  private[spider] def defaultExists(path: Path): Boolean = {
    println(s"Checking for default file at $path")
    if Files.isDirectory(rootFilePath / path) then {
      // If the path is a folder, see if a default file exists...
      FileContextHandler.defaultFiles.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path / d).toString)
        b || (file.exists() && file.isFile)
      }
    } else {
      // ... otherwise see if a file with a default ending exists
      FileContextHandler.defaultEndings.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path).toString + d)
        b || (file.exists() && file.isFile)
      }
    }

  }

  private[spider] def defaultFile(path: Path): File = {
    println(s"Getting default file at $path")
    FileContextHandler.defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toString))
      .find(_.exists())
      .orElse(
        FileContextHandler.defaultEndings.iterator
          .map(suffix => new File((rootFilePath / path).toString + suffix))
          .find(_.exists())
      )
      .getOrElse(throw new IllegalArgumentException("Not found"))
  }

  private val fileHandler: FileHandler =
    FileHandler(rootFilePath)

  override val contextRouter
      : PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    case HttpMethod.GET -> anyPath
        if fileExists(FileContextHandler.pathFromStrList(anyPath)) =>
      println("using filehanlder")
      fileHandler
    case HttpMethod.GET -> anyPath
        if defaultExists(FileContextHandler.pathFromStrList(anyPath)) =>
      println("using defaultfilehandler")
      DefaultFileHandler(
        defaultFile(FileContextHandler.pathFromStrList(anyPath))
      )
  }
}
