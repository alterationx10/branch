package dev.wishingtree.branch.spider.server

import java.net.URLEncoder
import java.nio.file.Path
import scala.annotation.targetName

object OpaqueSegments {

  opaque type Segments = Seq[String]

  object Segments {

    def wd: Segments = {
      >> / Path.of("").toAbsolutePath.toString
    }

    def apply(path: String): Segments =
      path
        .split("/")
        .filter(_.nonEmpty)
        .map(URLEncoder.encode(_, "UTF-8"))
        .toSeq

  }

  extension (path: Segments) {

    @targetName("appendStr")
    def /(segment: String): Segments = path ++ Segments(segment)

    @targetName("appendPath")
    def /(subPath: Segments): Segments = path ++ subPath

    def toPathString       = "/" + path.mkString("/")
    def toSeq: Seq[String] = path
  }

  object / {
    def unapply(path: Segments): Option[(Segments, String)] =
      path match {
        case head :+ tail => Some(head -> tail)
        case _            => None
      }
  }

  extension (sc: StringContext) {
    def p(args: Any*): Segments = Segments(sc.s(args*))
    def ci                      = ("(?i)" + sc.parts.mkString).r
  }

  @targetName("root")
  val >> : Segments = Segments("/")
}
