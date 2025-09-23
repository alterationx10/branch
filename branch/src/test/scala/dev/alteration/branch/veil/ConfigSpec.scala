package dev.alteration.branch.veil

import dev.alteration.branch.testkit.fixtures.FileFixtureSuite

case class AppConfig(host: String, port: Int) derives Config
case class AppConfig2(host: String, port: Int)

class ConfigSpec extends FileFixtureSuite {

  test("Config.fromResource") {
    for {
      config <- Config.of[AppConfig].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  val json = """{"host":"localhost","port":9000}"""
  fileWithContent(json).test("Config.fromFile") { file =>
    for {
      config <-
        Config
          .of[AppConfig]
          .fromFile(file)
    } yield assertEquals(config, AppConfig("localhost", 9000))
  }

  test("Config.fromResource with AppConfig2 auto-derive") {
    for {
      config <- Config.of[AppConfig2].fromResource("app-config.json")
    } yield assertEquals(config, AppConfig2("localhost", 9000))
  }

  test("Config.fromFile fails with non-existent file") {
    val result = Config.of[AppConfig].fromFile("non-existent-file.json")
    assert(result.isFailure)
  }

  val malformedJson = """{"host":"localhost","port":}"""
  fileWithContent(malformedJson).test(
    "Config.fromFile fails with malformed JSON"
  ) { file =>
    val result = Config.of[AppConfig].fromFile(file)
    assert(result.isFailure)
  }

  test("Config.fromResource fails with non-existent resource") {
    val result = Config.of[AppConfig].fromResource("non-existent-config.json")
    assert(result.isFailure)
  }

  val jsonForStringPath = """{"host":"localhost","port":9000}"""
  fileWithContent(jsonForStringPath).test("Config.fromFile with string path") {
    file =>
      for {
        config <- Config.of[AppConfig].fromFile(file.toString)
      } yield assertEquals(config, AppConfig("localhost", 9000))
  }

}
