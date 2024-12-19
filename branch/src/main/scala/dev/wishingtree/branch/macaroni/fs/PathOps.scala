package dev.wishingtree.branch.macaroni.fs

import java.nio.file.*
import scala.jdk.CollectionConverters.*

object PathOps {

  /** Get the current working directory as a Path
    * @return
    */
  def wd: Path = Path.of("").toAbsolutePath

  extension (path: Path) {

    /** Append a String to the path */
    def /(segment: String): Path = path.resolve(segment)

    /** Append a Path to the path */
    def /(subPath: Path): Path = path.resolve(subPath)

    /** Convert the path to a String. It will be prefixed with a slash
      */
    def toPathString = "/" + path.toString

    /** Convert the path to a Seq[String]
      */
    def toSeq: Seq[String] = path.toString.split("/").filter(_.nonEmpty).toSeq
  }

  object / {

    def unapply(path: Path): Option[(Path, String)] = {
      val segments = path.toSeq
      segments match {
        case head :+ tail => Some(Path.of(head.mkString("/")) -> tail)
        case _            => None
      }
    }
  }

  extension (sc: StringContext) {

    /** String interpolation for Segments */
    def p(args: Any*): Path = Path.of(sc.s(args*))

    /** Case-insensitive string interpolation for Segments */
    def ci = ("(?i)" + sc.parts.mkString).r
  }

  val >> : Path = Path.of("")

}
