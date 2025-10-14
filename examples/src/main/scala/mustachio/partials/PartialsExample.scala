package examples.mustachio.partials

import dev.alteration.branch.mustachio.{Mustachio, Stache}

/** An example showing how to use partials for template composition.
  *
  * This example demonstrates:
  *   - Using {{>partial}} to include reusable templates
  *   - Defining partials as a Stache.Obj with template strings
  *   - Partials inheriting the current context
  *   - Building complex templates from smaller components
  *
  * To run this example: sbt "examples/runMain
  * examples.mustachio.partials.PartialsExample"
  */
object PartialsExample {

  def main(args: Array[String]): Unit = {

    println("=== Mustachio Partials Example ===\n")

    // Simple partial inclusion
    println("=== Simple Partials ===")
    val mainTemplate = """
      |<html>
      |<head>
      |  {{>header}}
      |</head>
      |<body>
      |  <h1>{{title}}</h1>
      |  {{>footer}}
      |</body>
      |</html>
    """.stripMargin.trim

    val partials = Stache.obj(
      "header" -> Stache.str("<title>{{title}}</title>"),
      "footer" -> Stache.str("<footer>© 2025 {{company}}</footer>")
    )

    val context = Stache.obj(
      "title"   -> Stache.str("Welcome"),
      "company" -> Stache.str("Acme Corp")
    )

    val result = Mustachio.render(mainTemplate, context, Some(partials))
    println(s"$result\n")

    // Reusable components
    println("=== Reusable Components ===")
    val pageTemplate = """
      |{{#users}}
      |{{>userCard}}
      |{{/users}}
    """.stripMargin.trim

    val cardPartials = Stache.obj(
      "userCard" -> Stache.str("""
        |<div class="card">
        |  <h3>{{name}}</h3>
        |  <p>Email: {{email}}</p>
        |  <p>Role: {{role}}</p>
        |</div>
      """.stripMargin.trim)
    )

    val usersContext = Stache.obj(
      "users" -> Stache.Arr(
        List(
          Stache.obj(
            "name"  -> Stache.str("Alice"),
            "email" -> Stache.str("alice@example.com"),
            "role"  -> Stache.str("Admin")
          ),
          Stache.obj(
            "name"  -> Stache.str("Bob"),
            "email" -> Stache.str("bob@example.com"),
            "role"  -> Stache.str("User")
          ),
          Stache.obj(
            "name"  -> Stache.str("Charlie"),
            "email" -> Stache.str("charlie@example.com"),
            "role"  -> Stache.str("User")
          )
        )
      )
    )

    val cardResult = Mustachio.render(pageTemplate, usersContext, Some(cardPartials))
    println(s"$cardResult\n")

    // Nested partials
    println("=== Nested Partials ===")
    val layoutTemplate = """
      |<!DOCTYPE html>
      |<html>
      |{{>head}}
      |<body>
      |  {{>navigation}}
      |  <main>
      |    {{content}}
      |  </main>
      |  {{>footer}}
      |</body>
      |</html>
    """.stripMargin.trim

    val nestedPartials = Stache.obj(
      "head" -> Stache.str("""
        |<head>
        |  <meta charset="UTF-8">
        |  <title>{{pageTitle}}</title>
        |  {{>styles}}
        |</head>
      """.stripMargin.trim),
      "styles" -> Stache.str("""<link rel="stylesheet" href="/styles.css">"""),
      "navigation" -> Stache.str("""
        |<nav>
        |  <a href="/">Home</a> | <a href="/about">About</a>
        |</nav>
      """.stripMargin.trim),
      "footer" -> Stache.str("""
        |<footer>
        |  <p>Contact: {{contactEmail}}</p>
        |</footer>
      """.stripMargin.trim)
    )

    val layoutContext = Stache.obj(
      "pageTitle"    -> Stache.str("My Website"),
      "content"      -> Stache.str("Welcome to the home page!"),
      "contactEmail" -> Stache.str("info@example.com")
    )

    val layoutResult =
      Mustachio.render(layoutTemplate, layoutContext, Some(nestedPartials))
    println(s"$layoutResult\n")

    println("=== Example Complete ===")
  }

}
