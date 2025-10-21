package dev.alteration.branch.spider.server

import FileHandler.given
import RequestHandler.given

import java.io.{File, FileInputStream, FileNotFoundException, RandomAccessFile}
import java.nio.file.Path
import java.security.MessageDigest
import scala.util.Using

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

  /** Generate an ETag for a file based on its path, size, and last modified
    * time. This uses a weak ETag format (W/"...") as the content isn't hashed.
    */
  def generateETag(file: File): String = {
    val lastModified = file.lastModified()
    val size         = file.length()
    val path         = file.getAbsolutePath
    val input        = s"$path-$size-$lastModified"
    val digest       = MessageDigest.getInstance("MD5")
    val hash         = digest.digest(input.getBytes("UTF-8"))
    val hexString    = hash.map("%02x".format(_)).mkString
    s"""W/"$hexString""""
  }

  /** Read a range of bytes from a file.
    * @param file
    *   The file to read from
    * @param start
    *   The starting byte position (inclusive)
    * @param end
    *   The ending byte position (inclusive, optional)
    * @return
    *   Array of bytes within the specified range
    */
  def readFileRange(file: File, start: Long, end: Option[Long]): Array[Byte] = {
    val fileLength = file.length()
    val endByte    = end.getOrElse(fileLength - 1).min(fileLength - 1)
    val length     = (endByte - start + 1).toInt

    Using(new RandomAccessFile(file, "r")) { raf =>
      raf.seek(start)
      val buffer = new Array[Byte](length)
      raf.readFully(buffer)
      buffer
    }.getOrElse(
      throw new FileNotFoundException(
        s"File not found: ${file.getAbsolutePath}"
      )
    )
  }

  /** Parse a Range header value (e.g., "bytes=0-1023")
    * @return
    *   Option of (start, end) where end is optional
    */
  def parseRangeHeader(rangeHeader: String): Option[(Long, Option[Long])] = {
    rangeHeader.trim match {
      case s"bytes=$start-$end" if end.nonEmpty =>
        for {
          s <- start.toLongOption
          e <- end.toLongOption
        } yield (s, Some(e))
      case s"bytes=$start-"                     =>
        start.toLongOption.map((_, None))
      case _                                    => None
    }
  }
}

/** A built-in handler for serving files from the file system.
  *
  * Supports:
  *   - ETag generation and If-None-Match handling
  *   - Range requests for partial content (video streaming, etc.)
  *   - Cache-Control headers
  *
  * @param rootFilePath
  *   The root directory to serve files from
  * @param maxAge
  *   Cache-Control max-age in seconds (default: 3600 = 1 hour)
  */
case class FileHandler(
    rootFilePath: Path,
    maxAge: Int = 3600
) extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] = {
    val filePath = {
      rootFilePath.resolve(request.uri.getPath.stripPrefix("/")).toString
    }
    val file     = new File(filePath)

    // Check if file exists
    if (!file.exists() || !file.isFile) {
      return Response(404, body = new File("")).textContent
    }

    // Generate ETag
    val etag = FileHandler.generateETag(file)

    // Check If-None-Match header (ETag validation)
    val ifNoneMatch = request.headers.get("If-None-Match").flatMap(_.headOption)
    if (ifNoneMatch.contains(etag)) {
      return Response(304, body = new File(""))
        .withHeader("ETag", etag)
    }

    // Check for Range header
    val rangeHeader = request.headers.get("Range").flatMap(_.headOption)
    val response    = rangeHeader.flatMap(FileHandler.parseRangeHeader) match {
      case Some((startByte, endByteOpt)) =>
        // Partial content response
        val fileLength = file.length()
        val endByte    = endByteOpt.getOrElse(fileLength - 1)
        val rangeBytes =
          FileHandler.readFileRange(file, startByte, Some(endByte))

        Response(206, body = file)
          .withHeader("Content-Range", s"bytes $startByte-$endByte/$fileLength")
          .withHeader("Content-Length", rangeBytes.length.toString)
          .withHeader("Accept-Ranges", "bytes")

      case None =>
        // Full content response
        Response(200, body = file)
          .withHeader("Accept-Ranges", "bytes")
    }

    // Add caching headers
    response
      .autoContent(filePath)
      .withHeader("ETag", etag)
      .withHeader("Cache-Control", s"public, max-age=$maxAge")
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
  * These helpers make it easy to serve static files with SpiderApp or other
  * server implementations.
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

  /** Create a file-serving router for SpiderApp.
    *
    * This router will serve files from rootFilePath, including support for
    * default files (index.html) and default endings (.html).
    *
    * Example:
    * {{{
    * object MyApp extends SpiderApp {
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
