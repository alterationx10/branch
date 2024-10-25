package dev.wishingtree.branch.spider

import java.net.URLEncoder
import scala.annotation.targetName

object Paths {

  opaque type Path = Seq[String]

  object Path {

    def apply(path: String): Path =
      path
        .split("/")
        .filter(_.nonEmpty)
        .map(URLEncoder.encode(_, "UTF-8"))
        .toSeq

  }

  extension (path: Path) {

    @targetName("appendStr")
    def /(segment: String): Path = path ++ Path(segment)

    @targetName("appendPath")
    def /(subPath: Path): Path = path ++ subPath

  }

  @targetName("appendExtractor")
  object / {
    def unapply(path: Path): Option[(Path, String)] =
      path match {
        case head :+ tail => Some(head -> tail)
        case _            => None
      }
  }

  extension (sc: StringContext) {
    def p(args: Any*): Path = Path(sc.s(args*))
  }

  @targetName("root")
  val >> : Path = Path("/")
}
