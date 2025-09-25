package dev.alteration.branch.macaroni.extensions

import java.nio.file.Path

object PathExtensions {

  extension (path: Path) {

    def /(segment: String | Path): Path = {
      if (segment == null)
        throw new IllegalArgumentException("Path segment cannot be null")
      segment match {
        case p: Path     => path.resolve(p)
        case str: String => path.resolve(str)
      }
    }

    def relativeTo(rootPath: Path): Path = {
      if (rootPath == null)
        throw new IllegalArgumentException("Root path cannot be null")
      rootPath.relativize(path)
    }

  }

}
