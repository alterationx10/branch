package app.wishingtree

import dev.wishingtree.branch.friday.JsonDecoder
import dev.wishingtree.branch.veil.{Config, Veil}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object VeilExample {

  case class AppConfig(host: String, port: Int) derives JsonDecoder, Config

  def main(args: Array[String]): Unit = {

    println {
      Veil.get("THING_1")
    }

    println {
      Veil.get("THING_2")
    }

    println {
      Veil.get("THING_3")
    }

    println {
      Veil.get("USER")
    }

    println {
      summon[Config[AppConfig]].fromResource("app-config.json")
    }

    val filePath = Path.of("", "app-config.json").toAbsolutePath.toString

    println {
      summon[Config[AppConfig]].fromFile(filePath)
    }
  }

}
