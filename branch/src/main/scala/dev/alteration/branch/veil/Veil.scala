package dev.alteration.branch.veil

import RuntimeEnv.{DEV, PROD, STAGING, TEST}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** An object for reading environment variables from a .env file.
  */
object Veil {

  /** The base directory for .env files. Can be overridden with the
    * `VEIL_ENV_DIR` environment variable. Defaults to the current working
    * directory.
    */
  private val envDir: String =
    System.getenv().asScala.getOrElse("VEIL_ENV_DIR", "")

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
    *   - `STAGING` => ".env.staging"
    *   - `PROD` => ".env.prod"
    */
  private val dotEnv: Map[String, String] = {
    val envFile = this.runtimeEnv match {
      case DEV     => ".env"
      case TEST    => ".env.test"
      case STAGING => ".env.staging"
      case PROD    => ".env.prod"
    }
    val envPath = Path.of(envDir, envFile)

    scala.util
      .Using(Files.lines(envPath)) { lineStream =>
        lineStream
          .iterator()
          .asScala
          .map(_.trim)
          .filterNot(line => line.isEmpty || line.startsWith("#"))
          .flatMap { str =>
            str.split("=", 2).toList match {
              case key :: value :: Nil =>
                Some(key.trim -> stripQuotes(value.trim))
              case key :: Nil          => Some(key.trim -> "")
              case _                   => None
            }
          }
          .toMap
      }
      .recover { case e =>
        System.err.println(
          s"[Veil] Warning: Failed to load '$envFile': ${e.getMessage}"
        )
        Map.empty[String, String]
      }
      .getOrElse(Map.empty[String, String])
  }

  /** Utility method to strip prefixed quotes from a string, as might be common
    * in env files. Handles both single and double quotes.
    *
    * @param str
    *   the string to strip quotes from
    * @return
    *   the string without prefixed and suffixed quotes
    */
  private def stripQuotes(str: String): String = {
    val trimmed = str.trim
    if (
      (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
      (trimmed.startsWith("'") && trimmed.endsWith("'"))
    ) {
      trimmed.substring(1, trimmed.length - 1)
    } else {
      trimmed
    }
  }

  /** Get an environment variable by key. It first searches through variables
    * loaded from an env file, then through system variables.
    *
    * @param key
    *   the key of the environment variable
    * @return
    *   an `Option` containing the value of the environment variable, if found
    */
  final def get(key: String): Option[String] =
    dotEnv.get(key).orElse(System.getenv().asScala.get(key))

  /** Get the first environment variable defined in the arguments. It first
    * searches through variables * loaded from an env file, then through system
    * variables.
    *
    * @param key
    *   The first key to search for
    * @param alt
    *   the alternative keys to look for
    * @return
    *   an `Option` containing the value of the environment variable, if found
    */
  final def getFirst(key: String, alt: String*): Option[String] = {
    get(key)
      .orElse {
        alt.find(a => get(a).isDefined).flatMap(get)
      }
  }

}
