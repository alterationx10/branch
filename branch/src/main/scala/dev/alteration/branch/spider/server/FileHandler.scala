package dev.alteration.branch.spider.server

import FileHandler.given
import RequestHandler.given

import java.io.{File, FileInputStream, FileNotFoundException}
import java.nio.file.Path

object FileHandler {

  /** Conversion from File to Array[Byte]
    * @throws FileNotFoundException
    */
  given Conversion[File, Array[Byte]] =
    file =>
      scala.util
        .Using(new FileInputStream(file)) { is =>
          is.readAllBytes()
        }
        .getOrElse(
          throw new FileNotFoundException(
            s"File not found: ${file.getAbsolutePath}"
          )
        )
}

/** A built-in handler for serving files from the file system.
  */
case class FileHandler(rootFilePath: Path) extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] = {
    val filePath = {
      rootFilePath.resolve(request.uri.getPath.stripPrefix("/")).toString
    }
    Response(
      200,
      body = new File(filePath)
    ).autoContent(filePath)
  }
}

/** A built-in handler for serving default files (e.g. an index.html file).
  */
case class DefaultFileHandler(file: File) extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] =
    Response(
      200,
      body = file
    ).autoContent(file.getName)
}

/** Utilities for creating file-serving routers.
  *
  * These helpers make it easy to serve static files with SocketSpiderApp or
  * other server implementations.
  */
object FileServing {
  import dev.alteration.branch.macaroni.extensions.PathExtensions.*
  import java.nio.file.Files
  import dev.alteration.branch.spider.common.HttpMethod

  /** A list of default files to look for when a directory is requested. E.g.
    * /some/path -> /some/path/index.html
    */
  val defaultFiles: List[String] = List(
    "index.html",
    "index.htm"
  )

  /** A list of default file endings to look for when a file is requested. E.g.
    * /some/path -> /some/path.html
    */
  val defaultEndings: List[String] = List(
    ".html",
    ".htm"
  )

  private def pathFromStrList(segments: List[String]): Path = {
    segments match {
      case Nil          => Path.of("")
      case head :: Nil  => Path.of(head)
      case head :: tail => tail.foldLeft(Path.of(head))(_ / _)
    }
  }

  /** Check if a file exists at the given path.
    */
  def fileExists(rootFilePath: Path, path: Path): Boolean = {
    val filePath = (rootFilePath / path).toString
    val file     = new File(filePath)
    file.exists() && file.isFile
  }

  /** Check if a default file exists at the given path.
    *
    * If the path is a folder, looks for default files (index.html, etc).
    * Otherwise, looks for files with default endings (.html, etc).
    */
  def defaultExists(rootFilePath: Path, path: Path): Boolean = {
    if Files.isDirectory(rootFilePath / path) then {
      // If the path is a folder, see if a default file exists...
      defaultFiles.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path / d).toString)
        b || (file.exists() && file.isFile)
      }
    } else {
      // ... otherwise see if a file with a default ending exists
      defaultEndings.foldLeft(false) { (b, d) =>
        val file = new File((rootFilePath / path).toString + d)
        b || (file.exists() && file.isFile)
      }
    }
  }

  /** Get the default file at the given path.
    *
    * @throws IllegalArgumentException
    *   if no default file exists
    */
  def defaultFile(rootFilePath: Path, path: Path): File = {
    defaultFiles.iterator
      .map(fn => new File((rootFilePath / path / fn).toString))
      .find(_.exists())
      .orElse(
        defaultEndings.iterator
          .map(suffix => new File((rootFilePath / path).toString + suffix))
          .find(_.exists())
      )
      .getOrElse(throw new IllegalArgumentException("Not found"))
  }

  /** Create a file-serving router for SocketSpiderApp.
    *
    * This router will serve files from rootFilePath, including support for
    * default files (index.html) and default endings (.html).
    *
    * Example:
    * {{{
    * object MyApp extends SocketSpiderApp {
    *   override val router = FileServing.createRouter(Path.of("public"))
    * }
    * }}}
    */
  def createRouter(
      rootFilePath: Path
  ): PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
    val fileHandler = FileHandler(rootFilePath)

    {
      case HttpMethod.GET -> anyPath
          if fileExists(rootFilePath, pathFromStrList(anyPath)) =>
        fileHandler
      case HttpMethod.GET -> anyPath
          if defaultExists(rootFilePath, pathFromStrList(anyPath)) =>
        DefaultFileHandler(defaultFile(rootFilePath, pathFromStrList(anyPath)))
    }
  }
}
