package dev.wishingtree.branch.veil
import dev.wishingtree.branch.veil.RuntimeEnv.{DEV, PROD, TEST}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait Veil {
  val runtimeEnv: RuntimeEnv
  val dotEnv: Map[String, String]

  final def get(key: String): Option[String] =
    dotEnv.get(key).orElse(System.getenv().asScala.get(key))

}

object Veil {

  val runtimeEnv: RuntimeEnv =
    System
      .getenv()
      .asScala
      .get("SCALA_ENV")
      .flatMap(str => Try(RuntimeEnv.valueOf(str)).toOption)
      .getOrElse(RuntimeEnv.DEV)

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

  private def stripQuotes(str: String): String =
    str.stripPrefix("\"").stripSuffix("\"")

  final def get(key: String): Option[String] =
    dotEnv.get(key).orElse(System.getenv().asScala.get(key)).map(stripQuotes)

}
