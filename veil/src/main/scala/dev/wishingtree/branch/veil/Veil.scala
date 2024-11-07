package dev.wishingtree.branch.veil
import dev.wishingtree.branch.veil.RuntimeEnv.{DEV, PROD, TEST}

import scala.jdk.CollectionConverters.*
import scala.util.Try

trait Veil {
  val runtimeEnv: RuntimeEnv

  private final val dotEnv: Map[String, String] =
    DotEnv(runtimeEnv)

  final def getEnv(key: String): Option[String] =
    dotEnv.get(key).orElse(System.getenv().asScala.get(key))

}

object Veil {

  final val runtimeEnv: RuntimeEnv =
    System
      .getenv()
      .asScala
      .get("SCALA_ENV")
      .flatMap(str => Try(RuntimeEnv.valueOf(str)).toOption)
      .getOrElse(RuntimeEnv.DEV)

  final val env: Veil = new Veil {
    override val runtimeEnv: RuntimeEnv = runtimeEnv
  }

}
