package veil.config

import dev.alteration.branch.veil.Config
import dev.alteration.branch.friday.JsonDecoder

/** An example showing how to use the Config typeclass to load configuration from
  * JSON files.
  *
  * The Config typeclass provides a type-safe way to load configuration:
  * - fromFile: Load from a file path
  * - fromResource: Load from resources
  *
  * Config automatically derives instances for any type that has a JsonDecoder,
  * which in turn can be automatically derived for case classes.
  *
  * To run this example:
  * 1. Create a config.json file in your project root (see example below)
  * 2. Run: sbt "examples/runMain veil.config.ConfigExample"
  *
  * Example config.json:
  * {{{
  * {
  *   "appName": "My Application",
  *   "port": 8080,
  *   "database": {
  *     "host": "localhost",
  *     "port": 5432,
  *     "name": "mydb"
  *   },
  *   "features": {
  *     "enableCache": true,
  *     "maxConnections": 100
  *   }
  * }
  * }}}
  */
object ConfigExample extends App {

  // Define configuration case classes
  case class DatabaseConfig(
      host: String,
      port: Int,
      name: String
  )

  case class Features(
      enableCache: Boolean,
      maxConnections: Int
  )

  case class AppConfig(
      appName: String,
      port: Int,
      database: DatabaseConfig,
      features: Features
  )

  println("=== Config Example ===\n")

  // JsonDecoder is automatically derived for case classes
  given JsonDecoder[DatabaseConfig] = JsonDecoder.derived
  given JsonDecoder[Features] = JsonDecoder.derived
  given JsonDecoder[AppConfig] = JsonDecoder.derived

  // Config is automatically derived when JsonDecoder is available
  val config = Config.of[AppConfig]

  // Try loading from a file
  println("Attempting to load config from file...")
  config.fromFile("config.json") match {
    case scala.util.Success(appConfig) =>
      println(s"Successfully loaded configuration:")
      println(s"  App Name: ${appConfig.appName}")
      println(s"  Port: ${appConfig.port}")
      println(s"  Database: ${appConfig.database.host}:${appConfig.database.port}/${appConfig.database.name}")
      println(s"  Cache Enabled: ${appConfig.features.enableCache}")
      println(s"  Max Connections: ${appConfig.features.maxConnections}")

    case scala.util.Failure(exception) =>
      println(s"Failed to load config: ${exception.getMessage}")
      println("\nTip: Create a config.json file in your project root with the structure shown in the example.")
  }

  println()

  // You can also load from resources (files in src/main/resources)
  println("Attempting to load config from resources...")
  config.fromResource("example-config.json") match {
    case scala.util.Success(appConfig) =>
      println(s"Successfully loaded from resources: ${appConfig.appName}")

    case scala.util.Failure(exception) =>
      println(s"No config found in resources (this is expected)")
  }

  println("\n=== Example Complete ===")
}
