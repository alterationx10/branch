package dev.wishingtree.branch.veil

import munit.FunSuite

import java.nio.file.Path

case class AppConfig(host: String, port: Int) derives Config
case class AppConfig2(host: String, port: Int)

class ConfigSpec extends FunSuite {

  test("Config.fromResource") {
    for {
      config <- summon[Config[AppConfig]].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  test("Config.fromFile") {
    for {
      config <-
        summon[Config[AppConfig]]
          .fromFile(Path.of("", "app-config.json").toAbsolutePath.toString)
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  test("Config.fromResource with AppConfig2 auto-derive") {
    for {
      config <- summon[Config[AppConfig2]].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig2("localhost", 9000))
  }

}
