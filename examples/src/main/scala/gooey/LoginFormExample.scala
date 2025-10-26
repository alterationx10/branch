package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.prebuilt.Forms

import java.awt.Component

/** Login form example demonstrating the dramatic improvement in code clarity.
  *
  * Traditional Swing GridBagLayout version would be 83+ lines. This DSL version
  * is just 9 lines of form definition!
  */
object LoginFormExample extends GooeyApp {

  override val appTitle: String = "Login Form Example"

  override def mainContent: Component = {
    var username = ""
    var password = ""

    Forms
      .form(
        title = "Login",
        fields = Seq(
          Forms.formField(
            "Username",
            Forms.textField(
              placeholder = "Enter username",
              onChange = Some(u => username = u)
            )
          ),
          Forms.formField(
            "Password",
            Forms.passwordField(
              placeholder = "Enter password",
              onChange = Some(p => password = p)
            )
          )
        ),
        submitLabel = "Login"
      ) {
        // Handle login
        println(s"Authenticating user: $username")
        // authenticate(username, password)
      }
      .render()
      .asJComponent
  }

}
