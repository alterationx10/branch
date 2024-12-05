package dev.wishingtree.branch.veil

import dev.wishingtree.branch.friday.JsonDecoder

import scala.util.*
import java.nio.file.{Files, Path}
import scala.compiletime.summonInline
import scala.io.Source

/** A type-class for reading configuration from a file or resource.
  * @tparam T
  */
trait Config[T] {

  /** Read configuration from a file.
    * @param path
    * @return
    */
  def fromFile(path: Path): Try[T]

  /** Read configuration from a file.
    * @param path
    * @return
    */
  def fromFile(file: String): Try[T] = fromFile(Path.of(file))

  /** Read configuration from a resource.
    * @param path
    * @return
    */
  def fromResource(path: String): Try[T]
}

object Config {

  inline def of[A]: Config[A] =
    summonInline[Config[A]]

  inline given derived[T](using JsonDecoder[T]): Config[T] =
    new Config[T] {

      override def fromFile(path: Path): Try[T] =
        Try(Files.readString(path))
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
