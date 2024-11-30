package dev.wishingtree.branch.spider.server

import java.net.URLEncoder
import java.nio.file.Path
import scala.annotation.targetName

object OpaqueSegments {

  /** An opaque type over Seq[String] to represent a path
    */
  opaque type Segments = Seq[String]

  object Segments {

    /** Get the current working directory as a Segments
      * @return
      */
    def wd: Segments = {
      >> / Path.of("").toAbsolutePath.toString
    }

    /** Convert a String to a Segments
      */
    def apply(path: String): Segments =
      path
        .split("/")
        .filter(_.nonEmpty)
        .map(URLEncoder.encode(_, "UTF-8"))
        .toSeq

  }

  extension (path: Segments) {

    /** Append a String to the path */
    @targetName("appendStr")
    def /(segment: String): Segments = path ++ Segments(segment)

    /** Append a Path to the path */
    @targetName("appendPath")
    def /(subPath: Segments): Segments = path ++ subPath

    /** Convert the path to a String. It will be prefixed with a slash
      */
    def toPathString = "/" + path.mkString("/")

    /** Convert the path to a Seq[String]
      */
    def toSeq: Seq[String] = path
  }

  /** Extractor for the last segment of a path
    */
  object / {

    def unapply(path: Segments): Option[(Segments, String)] =
      path match {
        case head :+ tail => Some(head -> tail)
        case _            => None
      }
  }

  extension (sc: StringContext) {

    /** String interpolation for Segments */
    def p(args: Any*): Segments = Segments(sc.s(args*))

    /** Case-insensitive string interpolation for Segments */
    def ci = ("(?i)" + sc.parts.mkString).r
  }

  /** The root path. This would correlate to {{{Seq.empty[String]}}}
    */
  @targetName("root")
  val >> : Segments = Segments("/")
}
