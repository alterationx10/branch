package dev.wishingtree.branch.veil

import dev.wishingtree.branch.friday.JsonDecoder

import scala.util.*
import java.nio.file.{Files, Path}

trait Config[T] {
  def load(file: String): Try[T]
}

object Config {

  inline def derived[T](using JsonDecoder[T]): Config[T] =
    (file: String) => {
      Try(Files.readString(Path.of(file)))
        .flatMap(json => summon[JsonDecoder[T]].decode(json))
    }
}
