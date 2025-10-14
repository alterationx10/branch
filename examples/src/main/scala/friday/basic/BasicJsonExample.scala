package examples.friday.basic

import dev.alteration.branch.friday.Json
import dev.alteration.branch.friday.Json.*

/** A basic example showing how to parse and navigate JSON using the Friday AST.
  *
  * This example demonstrates:
  *   - Parsing JSON strings into the Json AST
  *   - Using the `?` operator for safe navigation of nested structures
  *   - Safe access methods (strOpt, numOpt, etc.) that return Option
  *   - Unsafe access methods (strVal, numVal, etc.) that throw on mismatch
  *   - Building JSON objects programmatically
  *
  * To run this example: sbt "examples/runMain examples.friday.basic.BasicJsonExample"
  */
object BasicJsonExample {

  def main(args: Array[String]): Unit = {

    println("=== Friday JSON Parsing and Navigation Example ===\n")

    // Parse a JSON string
    val jsonString = """{
      "user": {
        "id": 42,
        "name": "Alice",
        "email": "alice@example.com",
        "active": true,
        "preferences": {
          "theme": "dark",
          "notifications": true
        }
      },
      "tags": ["scala", "json", "friday"]
    }"""

    val result = Json.parse(jsonString)

    result match {
      case Right(json) =>
        println("✓ JSON parsed successfully\n")

        // Using the ? operator for safe navigation
        println("=== Safe Navigation with ? ===")
        val userName = json ? "user" ? "name"
        println(s"User name: ${userName.flatMap(_.strOpt).getOrElse("not found")}")

        val userEmail = json ? "user" ? "email"
        println(s"User email: ${userEmail.flatMap(_.strOpt).getOrElse("not found")}")

        val theme = json ? "user" ? "preferences" ? "theme"
        println(s"Theme preference: ${theme.flatMap(_.strOpt).getOrElse("not found")}")

        // Missing fields return None
        val missingField = json ? "user" ? "address" ? "street"
        println(s"Missing field: ${missingField.flatMap(_.strOpt).getOrElse("not found")}\n")

        // Safe access with Option methods
        println("=== Safe Access Methods ===")
        (json ? "user" ? "id").flatMap(_.numOpt).foreach { id =>
          println(s"User ID (safe): $id")
        }

        (json ? "user" ? "active").flatMap(_.boolOpt).foreach { active =>
          println(s"Is active (safe): $active")
        }

        // Array access
        (json ? "tags").flatMap(_.arrOpt).foreach { tags =>
          println(s"Tags count: ${tags.length}")
          tags.foreach { tag =>
            println(s"  - ${tag.strOpt.getOrElse("unknown")}")
          }
        }

        println()

        // Unsafe access (use when you're confident about the structure)
        println("=== Unsafe Access Methods ===")
        try {
          val obj = json.objVal
          val user = obj("user").objVal
          val userId = user("id").numVal.toInt
          val userName = user("name").strVal
          println(s"User: $userName (ID: $userId)")
        } catch {
          case e: Exception =>
            println(s"Error with unsafe access: ${e.getMessage}")
        }

        println()

      case Left(error) =>
        println(s"Failed to parse JSON: $error")
    }

    // Building JSON programmatically
    println("=== Building JSON Programmatically ===")
    val newUser = Json.obj(
      "id"     -> JsonNumber(99),
      "name"   -> JsonString("Bob"),
      "email"  -> JsonString("bob@example.com"),
      "active" -> JsonBool(true),
      "roles"  -> JsonArray(IndexedSeq(JsonString("admin"), JsonString("user")))
    )

    println(s"Created user JSON:\n${newUser.toJsonString}\n")

    // Combining objects
    val metadata = Json.obj(
      "created" -> JsonString("2025-01-25"),
      "version" -> JsonNumber(1.0)
    )

    val fullDocument = Json.obj(
      "user"     -> newUser,
      "metadata" -> metadata
    )

    println(s"Full document:\n${fullDocument.toJsonString}")

    println("\n=== Example Complete ===")
  }

}
