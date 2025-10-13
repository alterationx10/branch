package examples.mustachio.json

import dev.alteration.branch.mustachio.{Mustachio, Stache}
import dev.alteration.branch.friday.Json

/** An example showing how to use Mustachio with JSON data from Friday.
  *
  * This example demonstrates:
  *   - Converting Friday JSON to Stache using Stache.fromJson
  *   - Using JSON data sources for template rendering
  *   - Working with JSON files or API responses
  *   - A practical workflow for data-driven templates
  *
  * To run this example: sbt "examples/runMain
  * examples.mustachio.json.JsonIntegrationExample"
  */
object JsonIntegrationExample {

  def main(args: Array[String]): Unit = {

    println("=== Mustachio + Friday JSON Integration Example ===\n")

    // Parse JSON and render template
    println("=== Converting JSON to Stache ===")

    val jsonString = """{
      "title": "Blog Post",
      "author": {
        "name": "Alice Johnson",
        "email": "alice@example.com",
        "bio": "Software engineer & technical writer"
      },
      "date": "2025-01-25",
      "content": "This is an amazing blog post about Scala templating!",
      "tags": ["scala", "templates", "mustachio"],
      "stats": {
        "views": 1234,
        "likes": 89,
        "comments": 15
      }
    }"""

    val blogTemplate = """
      |# {{title}}
      |
      |**By {{author.name}}** ({{author.email}})
      |*{{date}}*
      |
      |{{content}}
      |
      |---
      |
      |## About the Author
      |{{author.bio}}
      |
      |## Tags
      |{{#tags}}
      |#{{.}}{{/tags}}
      |
      |## Stats
      |Views: {{stats.views}} | Likes: {{stats.likes}} | Comments: {{stats.comments}}
    """.stripMargin.trim

    val parsedJson = Json.parse(jsonString)

    parsedJson match {
      case Right(json) =>
        val context = Stache.fromJson(json)
        val result  = Mustachio.render(blogTemplate, context)
        println(result)
        println()

      case Left(error) =>
        println(s"Failed to parse JSON: $error")
    }

    // API response example
    println("=== Simulated API Response ===\n")

    val apiResponseJson = """{
      "status": "success",
      "data": {
        "users": [
          {
            "id": 1,
            "username": "alice",
            "active": true,
            "lastLogin": "2025-01-25"
          },
          {
            "id": 2,
            "username": "bob",
            "active": true,
            "lastLogin": "2025-01-24"
          },
          {
            "id": 3,
            "username": "charlie",
            "active": false,
            "lastLogin": "2025-01-15"
          }
        ]
      }
    }"""

    val reportTemplate = """
      |User Activity Report
      |====================
      |
      |Status: {{status}}
      |
      |Active Users:
      |{{#data.users}}
      |{{#active}}
      |  - {{username}} (ID: {{id}}, Last Login: {{lastLogin}})
      |{{/active}}
      |{{/data.users}}
      |
      |Inactive Users:
      |{{#data.users}}
      |{{^active}}
      |  - {{username}} (ID: {{id}}, Last Login: {{lastLogin}})
      |{{/active}}
      |{{/data.users}}
    """.stripMargin.trim

    Json.parse(apiResponseJson) match {
      case Right(json) =>
        val context = Stache.fromJson(json)
        val result  = Mustachio.render(reportTemplate, context)
        println(result)
        println()

      case Left(error) =>
        println(s"Failed to parse JSON: $error")
    }

    // Configuration file example
    println("=== Configuration Template ===\n")

    val configJson = """{
      "environment": "production",
      "database": {
        "host": "db.example.com",
        "port": 5432,
        "name": "myapp",
        "ssl": true
      },
      "cache": {
        "enabled": true,
        "ttl": 3600
      },
      "features": {
        "darkMode": true,
        "notifications": false,
        "analytics": true
      }
    }"""

    val configTemplate = """
      |# Application Configuration
      |
      |Environment: {{environment}}
      |
      |## Database Settings
      |- Host: {{database.host}}
      |- Port: {{database.port}}
      |- Database: {{database.name}}
      |{{#database.ssl}}
      |- SSL: Enabled ✓
      |{{/database.ssl}}
      |
      |## Cache Settings
      |{{#cache.enabled}}
      |- Cache: Enabled
      |- TTL: {{cache.ttl}} seconds
      |{{/cache.enabled}}
      |{{^cache.enabled}}
      |- Cache: Disabled
      |{{/cache.enabled}}
      |
      |## Feature Flags
      |{{#features.darkMode}}
      |✓ Dark Mode
      |{{/features.darkMode}}
      |{{^features.darkMode}}
      |✗ Dark Mode
      |{{/features.darkMode}}
      |{{#features.notifications}}
      |✓ Notifications
      |{{/features.notifications}}
      |{{^features.notifications}}
      |✗ Notifications
      |{{/features.notifications}}
      |{{#features.analytics}}
      |✓ Analytics
      |{{/features.analytics}}
      |{{^features.analytics}}
      |✗ Analytics
      |{{/features.analytics}}
    """.stripMargin.trim

    Json.parse(configJson) match {
      case Right(json) =>
        val context = Stache.fromJson(json)
        val result  = Mustachio.render(configTemplate, context)
        println(result)

      case Left(error) =>
        println(s"Failed to parse JSON: $error")
    }

    println("\n=== Example Complete ===")
  }

}
