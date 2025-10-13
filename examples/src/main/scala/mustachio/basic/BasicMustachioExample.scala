package examples.mustachio.basic

import dev.alteration.branch.mustachio.{Mustachio, Stache}

/** A basic example showing simple mustache template rendering with variables.
  *
  * This example demonstrates:
  *   - Basic variable substitution using {{variable}}
  *   - HTML escaping with {{variable}} vs unescaped {{{variable}}}
  *   - Creating context with Stache.obj and Stache.str
  *   - Nested object access with dot notation
  *
  * To run this example: sbt "examples/runMain
  * examples.mustachio.basic.BasicMustachioExample"
  */
object BasicMustachioExample {

  def main(args: Array[String]): Unit = {

    println("=== Basic Mustachio Template Example ===\n")

    // Simple variable substitution
    println("=== Simple Variables ===")
    val simpleTemplate = "Hello, {{name}}! Welcome to {{place}}."

    val simpleContext = Stache.obj(
      "name"  -> Stache.str("Alice"),
      "place" -> Stache.str("Scala")
    )

    val simpleResult = Mustachio.render(simpleTemplate, simpleContext)
    println(s"Template: $simpleTemplate")
    println(s"Result:   $simpleResult\n")

    // HTML escaping
    println("=== HTML Escaping ===")
    val escapedTemplate = """
      |Escaped: {{html}}
      |Unescaped: {{{html}}}
    """.stripMargin.trim

    val escapedContext = Stache.obj(
      "html" -> Stache.str("<script>alert('xss')</script>")
    )

    val escapedResult = Mustachio.render(escapedTemplate, escapedContext)
    println(s"Result:\n$escapedResult\n")

    // Nested object access
    println("=== Nested Objects ===")
    val nestedTemplate = """
      |User: {{user.name}}
      |Email: {{user.email}}
      |Role: {{user.profile.role}}
      |Department: {{user.profile.department}}
    """.stripMargin.trim

    val nestedContext = Stache.obj(
      "user" -> Stache.obj(
        "name"  -> Stache.str("Bob"),
        "email" -> Stache.str("bob@example.com"),
        "profile" -> Stache.obj(
          "role"       -> Stache.str("Developer"),
          "department" -> Stache.str("Engineering")
        )
      )
    )

    val nestedResult = Mustachio.render(nestedTemplate, nestedContext)
    println(s"Result:\n$nestedResult\n")

    // Missing values
    println("=== Missing Values ===")
    val missingTemplate = "Name: {{name}}, Age: {{age}}, City: {{city}}"

    val missingContext = Stache.obj(
      "name" -> Stache.str("Charlie")
      // age and city are missing
    )

    val missingResult = Mustachio.render(missingTemplate, missingContext)
    println(s"Template: $missingTemplate")
    println(s"Result:   $missingResult")
    println("(Missing values render as empty strings)\n")

    println("=== Example Complete ===")
  }

}
