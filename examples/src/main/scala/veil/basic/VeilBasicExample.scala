package veil.basic

import dev.alteration.branch.veil.Veil

/** A basic example showing how to use Veil to read environment variables.
  *
  * Veil loads environment variables from .env files based on the runtime
  * environment:
  *   - DEV (default): .env
  *   - TEST: .env.test
  *   - STAGING: .env.staging
  *   - PROD: .env.prod
  *
  * The runtime environment is controlled by the SCALA_ENV environment variable.
  *
  * To run this example:
  *   1. Create a .env file in your project root with some variables 2. Run: sbt
  *      "examples/runMain veil.basic.VeilBasicExample"
  *
  * To test different environments:
  *   - export SCALA_ENV=TEST (and create .env.test)
  *   - export SCALA_ENV=STAGING (and create .env.staging)
  *   - export SCALA_ENV=PROD (and create .env.prod)
  */
object VeilBasicExample {

  def main(args: Array[String]): Unit = {

    println("=== Veil Basic Example ===\n")

    // Get the current runtime environment
    println(s"Runtime Environment: ${Veil.runtimeEnv}")
    println()

    // Get a single environment variable
    val apiKey = Veil.get("API_KEY")
    println(s"API_KEY: ${apiKey.getOrElse("not found")}")

    // Get database configuration
    val dbHost = Veil.get("DB_HOST")
    val dbPort = Veil.get("DB_PORT")
    val dbName = Veil.get("DB_NAME")

    println(s"DB_HOST: ${dbHost.getOrElse("not found")}")
    println(s"DB_PORT: ${dbPort.getOrElse("not found")}")
    println(s"DB_NAME: ${dbName.getOrElse("not found")}")
    println()

    // Use getFirst to try multiple keys (useful for legacy compatibility)
    val port = Veil.getFirst("PORT", "APP_PORT", "SERVER_PORT")
    println(
      s"Port (checking PORT, APP_PORT, SERVER_PORT): ${port.getOrElse("8080")}"
    )
    println()

    // Veil checks .env files first, then falls back to system environment variables
    val systemPath = Veil.get("PATH")
    println(s"System PATH found: ${systemPath.isDefined}")

    println("\n=== Example Complete ===")
  }

}
