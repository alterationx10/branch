package dev.wishingtree.branch.veil

import dev.wishingtree.branch.testkit.fixtures.FileFixtureSuite

import java.nio.file.Path

case class AppConfig(host: String, port: Int) derives Config
case class AppConfig2(host: String, port: Int)

class ConfigSpec extends FileFixtureSuite {

  test("Config.fromResource") {
    for {
      config <- summon[Config[AppConfig]].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  val json = """{"host":"localhost","port":9000}"""
  filesWithContent(json).test("Config.fromFile") { file =>
    for {
      config <-
        summon[Config[AppConfig]]
          .fromFile(file)
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  test("Config.fromResource with AppConfig2 auto-derive") {
    for {
      config <- summon[Config[AppConfig2]].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig2("localhost", 9000))
  }

}
