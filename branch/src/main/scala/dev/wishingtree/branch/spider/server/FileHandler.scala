package dev.wishingtree.branch.spider.server

import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.spider.server.FileHandler.given
import dev.wishingtree.branch.spider.server.RequestHandler.given

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
private[spider] case class FileHandler(rootFilePath: Path)
    extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] = {
    val filePath = (rootFilePath / request.uri.getPath.stripPrefix("/")).toString
    Response(
      body = new File(filePath)
    ).autoContent(filePath)
  }
}

/** A built-in handler for serving default files (e.g. an index.html file).
  */
private[spider] case class DefaultFileHandler(file: File)
    extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] =
    Response(
      body = file
    ).autoContent(file.getName)
}
