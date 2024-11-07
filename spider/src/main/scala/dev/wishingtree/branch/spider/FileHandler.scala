package dev.wishingtree.branch.spider

import Paths.*
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

private[spider] case class FileHandler(rootDir: Path)
    extends RequestHandler[Unit, File] {

  override def handle(request: Request[Unit]): Response[File] = {
    val filePath = request.uri.getPath.toLowerCase
    Response(
      body = new File(filePath)
    )
  }
}
