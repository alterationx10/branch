package dev.alteration.branch.macaroni.fs

import java.nio.file.*

/** Provides extension methods and utilities for working with java.nio.file.Path
  */
object PathOps {

  /** Get the current working directory as a Path
    * @return
    *   Path representing the current working directory
    */
  def wd: Path = Path.of("").toAbsolutePath

  extension (path: Path) {

    /** Append a String segment to the path
      * @param segment
      *   The string segment to append
      * @return
      *   A new Path with the segment appended
      * @throws InvalidPathException
      *   if the segment is invalid
      */
    def /(segment: String): Path = {
      if (segment == null)
        throw new IllegalArgumentException("Path segment cannot be null")
      path.resolve(segment)
    }

    /** Append a Path to the current path
      * @param subPath
      *   The path to append
      * @return
      *   A new Path with the subPath appended
      * @throws InvalidPathException
      *   if the resulting path would be invalid
      */
    def /(subPath: Path): Path = {
      if (subPath == null)
        throw new IllegalArgumentException("SubPath cannot be null")
      path.resolve(subPath)
    }

    /** Convert the path to a sequence of path segments
      * @return
      *   Sequence of path segment strings, excluding empty segments
      */
    def toSeq: Seq[String] = path.toString.split("/").filter(_.nonEmpty).toSeq

    /** Relativize the path against a root path string
      * @param rootPath
      *   The root path to relativize against
      * @return
      *   A new Path representing the relative path
      * @throws IllegalArgumentException
      *   if rootPath is null
      */
    def relativeTo(rootPath: String): Path = {
      if (rootPath == null)
        throw new IllegalArgumentException("Root path cannot be null")
      Path.of(rootPath).relativize(path)
    }

    /** Relativize the path against a root Path
      * @param rootPath
      *   The root path to relativize against
      * @return
      *   A new Path representing the relative path
      * @throws IllegalArgumentException
      *   if rootPath is null
      */
    def relativeTo(rootPath: Path): Path = {
      if (rootPath == null)
        throw new IllegalArgumentException("Root path cannot be null")
      rootPath.relativize(path)
    }
  }

  /** Path extractor for pattern matching on path segments */
  object / {

    /** Splits a path into its parent path and last segment
      * @param path
      *   The path to split
      * @return
      *   Some((parent, lastSegment)) if path has segments, None for empty path
      */
    def unapply(path: Path): Option[(Path, String)] = {
      val segments = path.toSeq
      segments match {
        case head :+ tail => Some(Path.of(head.mkString("/")) -> tail)
        case _            => None
      }
    }
  }

  extension (sc: StringContext) {

    /** String interpolation for creating Path objects
      * @param args
      *   The interpolation arguments
      * @return
      *   A new Path constructed from the interpolated string
      */
    def p(args: Any*): Path = Path.of(sc.s(args*))

    /** Case-insensitive regex string interpolation
      * @return
      *   A Regex that matches the interpolated string case-insensitively
      */
    def ci = ("(?i)" + sc.parts.mkString).r
  }

  /** Empty relative path, useful for pattern matching. Also behaves as working
    * directory.
    */
  val >> : Path = Path.of("")

}
