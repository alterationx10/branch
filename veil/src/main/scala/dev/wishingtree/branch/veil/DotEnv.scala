package dev.wishingtree.branch.veil

import dev.wishingtree.branch.veil.RuntimeEnv.*

import java.io.File
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object DotEnv {
  def apply(env: RuntimeEnv = DEV): Map[String, String] = {
    val envFile = env match {
      case DEV  => ".env"
      case TEST => ".env.test"
      case PROD => ".env.prod"
    }
    val file    = new File(
      Path.of("", envFile).toAbsolutePath.toString
    )
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
}
