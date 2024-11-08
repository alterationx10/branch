package dev.wishingtree.branch.veil

import dev.wishingtree.branch.friday.JsonDecoder

import scala.util.*
import java.nio.file.{Files, Path}
import scala.io.Source

trait Config[T] {
  def fromFile(file: String): Try[T]
  def fromResource(path: String): Try[T]
}

object Config {

  inline def derived[T](using JsonDecoder[T]): Config[T] =
    new Config[T] {

      override def fromFile(file: String): Try[T] =
        Try(Files.readString(Path.of(file)))
          .flatMap(json => summon[JsonDecoder[T]].decode(json))

      override def fromResource(path: String): Try[T] = {
        scala.util
          .Using(Source.fromResource(path)) { iter =>
            val json = iter.mkString
            summon[JsonDecoder[T]].decode(json)
          }
          .flatten
      }
    }
}
