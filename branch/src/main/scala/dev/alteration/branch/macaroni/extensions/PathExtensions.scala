package dev.alteration.branch.macaroni.extensions

import java.nio.file.Path

object PathExtensions {

  /** Current working directory as an absolute path */
  def wd: Path = Path.of("").toAbsolutePath

  extension (path: Path) {

    /** Resolve a path segment to the current path */
    def /(segment: String | Path): Path = {
      if (segment == null)
        throw new IllegalArgumentException("Path segment cannot be null")
      segment match {
        case p: Path     => path.resolve(p)
        case str: String => path.resolve(str)
      }
    }

    /** Get the relative path of this path from the given root path */
    def relativeTo(rootPath: Path): Path = {
      if (rootPath == null)
        throw new IllegalArgumentException("Root path cannot be null")
      rootPath.relativize(path)
    }

  }

}
