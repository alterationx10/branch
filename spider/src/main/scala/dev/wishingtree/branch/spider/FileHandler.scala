package dev.wishingtree.branch.spider

import OpaqueSegments.*
import FileHandler.given
import RequestHandler.given

import java.io.{File, FileInputStream}

object FileHandler {

  given Conversion[File, Array[Byte]] =
    file =>
      scala.util
        .Using(new FileInputStream(file)) { is =>
          is.readAllBytes()
        }
        .getOrElse(throw new Exception("Not found"))
}

private[spider] case class FileHandler(rootFilePath: Segments)
    extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] = {
    val filePath = (rootFilePath / request.uri.getPath.toLowerCase).toPathString
    Response(
      body = new File(filePath)
    ).autoContent(filePath)
  }
}

private[spider] case class DefaultFilerHandler(file: File)
    extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] =
    Response(
      body = file
    ).autoContent(file.getName)
}
