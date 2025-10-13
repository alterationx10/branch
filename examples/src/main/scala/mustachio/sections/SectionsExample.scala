package examples.mustachio.sections

import dev.alteration.branch.mustachio.{Mustachio, Stache}

/** An example showing how to use sections and arrays in mustache templates.
  *
  * This example demonstrates:
  *   - Iterating over arrays with {{#section}}...{{/section}}
  *   - Conditional rendering based on truthiness
  *   - Inverted sections with {{^section}}...{{/section}}
  *   - Nested sections
  *   - Using {{.}} to reference the current item
  *
  * To run this example: sbt "examples/runMain
  * examples.mustachio.sections.SectionsExample"
  */
object SectionsExample {

  def main(args: Array[String]): Unit = {

    println("=== Mustachio Sections and Arrays Example ===\n")

    // Array iteration
    println("=== Iterating Over Arrays ===")
    val arrayTemplate = """
      |Shopping List:
      |{{#items}}
      |  - {{.}}
      |{{/items}}
    """.stripMargin.trim

    val arrayContext = Stache.obj(
      "items" -> Stache.Arr(
        List(
          Stache.str("Milk"),
          Stache.str("Eggs"),
          Stache.str("Bread"),
          Stache.str("Coffee")
        )
      )
    )

    val arrayResult = Mustachio.render(arrayTemplate, arrayContext)
    println(s"$arrayResult\n")

    // Array of objects
    println("=== Array of Objects ===")
    val objectArrayTemplate = """
      |Team Members:
      |{{#members}}
      |  - {{name}} ({{role}})
      |{{/members}}
    """.stripMargin.trim

    val objectArrayContext = Stache.obj(
      "members" -> Stache.Arr(
        List(
          Stache.obj("name" -> Stache.str("Alice"), "role" -> Stache.str("Lead")),
          Stache
            .obj("name" -> Stache.str("Bob"), "role" -> Stache.str("Developer")),
          Stache
            .obj("name" -> Stache.str("Charlie"), "role" -> Stache.str("Designer"))
        )
      )
    )

    val objectArrayResult = Mustachio.render(objectArrayTemplate, objectArrayContext)
    println(s"$objectArrayResult\n")

    // Conditional sections
    println("=== Conditional Sections ===")
    val conditionalTemplate = """
      |{{#loggedIn}}
      |Welcome back, {{username}}!
      |{{/loggedIn}}
      |{{^loggedIn}}
      |Please log in to continue.
      |{{/loggedIn}}
    """.stripMargin.trim

    // When loggedIn is an object (truthy)
    val loggedInContext = Stache.obj(
      "loggedIn" -> Stache.obj("username" -> Stache.str("Alice"))
    )

    val loggedInResult = Mustachio.render(conditionalTemplate, loggedInContext)
    println("Logged in:")
    println(s"$loggedInResult\n")

    // When loggedIn is missing (falsy)
    val loggedOutContext = Stache.obj()

    val loggedOutResult = Mustachio.render(conditionalTemplate, loggedOutContext)
    println("Logged out:")
    println(s"$loggedOutResult\n")

    // Nested sections
    println("=== Nested Sections ===")
    val nestedTemplate = """
      |{{#departments}}
      |Department: {{name}}
      |  {{#employees}}
      |  - {{name}} ({{title}})
      |  {{/employees}}
      |{{/departments}}
    """.stripMargin.trim

    val nestedContext = Stache.obj(
      "departments" -> Stache.Arr(
        List(
          Stache.obj(
            "name" -> Stache.str("Engineering"),
            "employees" -> Stache.Arr(
              List(
                Stache.obj(
                  "name"  -> Stache.str("Alice"),
                  "title" -> Stache.str("Senior Engineer")
                ),
                Stache.obj(
                  "name"  -> Stache.str("Bob"),
                  "title" -> Stache.str("Engineer")
                )
              )
            )
          ),
          Stache.obj(
            "name" -> Stache.str("Design"),
            "employees" -> Stache.Arr(
              List(
                Stache.obj(
                  "name"  -> Stache.str("Charlie"),
                  "title" -> Stache.str("Lead Designer")
                )
              )
            )
          )
        )
      )
    )

    val nestedResult = Mustachio.render(nestedTemplate, nestedContext)
    println(s"$nestedResult\n")

    // Empty array handling
    println("=== Empty Array Handling ===")
    val emptyArrayTemplate = """
      |{{#items}}
      |  - {{.}}
      |{{/items}}
      |{{^items}}
      |No items found.
      |{{/items}}
    """.stripMargin.trim

    val emptyArrayContext = Stache.obj(
      "items" -> Stache.Arr(List.empty)
    )

    val emptyArrayResult = Mustachio.render(emptyArrayTemplate, emptyArrayContext)
    println(s"$emptyArrayResult\n")

    println("=== Example Complete ===")
  }

}
