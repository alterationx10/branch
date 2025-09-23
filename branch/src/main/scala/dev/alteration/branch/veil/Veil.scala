package dev.alteration.branch.veil

import RuntimeEnv.{DEV, PROD, TEST}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** An object for reading environment variables from a .env file.
  */
object Veil {

  /** The runtime environment, as based on the environment variable `SCALA_ENV`.
    * If missing, or configured incorrectly, defaults to `DEV`.
    */
  val runtimeEnv: RuntimeEnv =
    System
      .getenv()
      .asScala
      .get("SCALA_ENV")
      .flatMap(str => Try(RuntimeEnv.valueOf(str)).toOption)
      .getOrElse(RuntimeEnv.DEV)

  /** Loads the env file into a `Map`, based on the [[runtimeEnv]].
    *
    * The file loaded depends on the runtime environment:
    *   - `DEV` => ".env"
    *   - `TEST` => ".env.test"
    *   - `PROD` => ".env.prod"
    */
  private val dotEnv: Map[String, String] = {
    val envFile = this.runtimeEnv match {
      case DEV  => ".env"
      case TEST => ".env.test"
      case PROD => ".env.prod"
    }
    scala.util
      .Using(Files.lines(Path.of("", envFile))) { lineStream =>
        lineStream
          .iterator()
          .asScala
          .map { str =>
            val (a: String) :: b = str.split("=").toList: @unchecked
            a -> b.mkString("=") // need to "unquote" values
          }
          .toMap
      }
      .getOrElse(Map.empty[String, String])
  }

  /** Utility method to strip prefixed quotes from a string, as might be common
    * in env files.
    *
    * @param str
    *   the string to strip quotes from
    * @return
    *   the string without prefixed and suffixed quotes
    */
  private def stripQuotes(str: String): String =
    str.stripPrefix("\"").stripSuffix("\"")

  /** Get an environment variable by key. It first searches through variables
    * loaded from an env file, then through system variables.
    *
    * @param key
    *   the key of the environment variable
    * @return
    *   an `Option` containing the value of the environment variable, if found
    */
  final def get(key: String): Option[String] =
    dotEnv.get(key).map(stripQuotes).orElse(System.getenv().asScala.get(key))

}
