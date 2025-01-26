package dev.wishingtree.branch.veil

import dev.wishingtree.branch.friday.JsonDecoder

import scala.util.*
import java.nio.file.{Files, Path}
import scala.compiletime.summonInline
import scala.io.Source

/** A type-class for reading configuration from a file or resource.
  *
  * @tparam T
  *   the type of the configuration object
  */
trait Config[T] {

  /** Reads configuration from a file.
    *
    * @param path
    *   the path to the configuration file
    * @return
    *   a `Try` containing the configuration object or an exception if an error
    *   occurs
    */
  def fromFile(path: Path): Try[T]

  /** Reads configuration from a file.
    *
    * @param file
    *   the path to the configuration file as a string
    * @return
    *   a `Try` containing the configuration object or an exception if an error
    *   occurs
    */
  def fromFile(file: String): Try[T] = fromFile(Path.of(file))

  /** Reads configuration from a resource.
    *
    * @param path
    *   the path to the resource
    * @return
    *   a `Try` containing the configuration object or an exception if an error
    *   occurs
    */
  def fromResource(path: String): Try[T]
}

object Config {

  /** Summons an implicit `Config` instance for the specified type.
    *
    * @tparam A
    *   the type of the configuration object
    * @return
    *   the summoned `Config` instance
    */
  inline def of[A]: Config[A] =
    summonInline[Config[A]]

  /** Derives a `Config` instance for the specified type using a `JsonDecoder`.
    *
    * @tparam T
    *   the type of the configuration object
    * @param jsonDecoder
    *   the implicit `JsonDecoder` instance
    * @return
    *   the derived `Config` instance
    */
  inline given derived[T](using JsonDecoder[T]): Config[T] =
    new Config[T] {

      /** Reads configuration from a file.
        *
        * @param path
        *   the path to the configuration file
        * @return
        *   a `Try` containing the configuration object or an exception if an
        *   error occurs
        */
      override def fromFile(path: Path): Try[T] =
        Try(Files.readString(path))
          .flatMap(json => summon[JsonDecoder[T]].decode(json))

      /** Reads configuration from a resource.
        *
        * @param path
        *   the path to the resource
        * @return
        *   a `Try` containing the configuration object or an exception if an
        *   error occurs
        */
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
