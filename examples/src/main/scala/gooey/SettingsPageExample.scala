package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.templates.Templates
import dev.alteration.branch.gooey.prebuilt.Forms
import dev.alteration.branch.gooey.styling.Fonts

import java.awt.Component

/** Settings page example demonstrating structured sections. */
object SettingsPageExample extends GooeyApp {

  override val appTitle: String = "Settings"

  override def mainContent: Component = {
    Templates
      .settingsPage(
        Seq(
          "Account" -> Seq(
            Forms.formField("Email", Forms.textField(value = "user@example.com")),
            Forms.formField(
              "Password",
              Forms.passwordField(placeholder = "Change password")
            )
          ),
          "Appearance" -> Seq(
            Forms.formField("Theme", Text("Light").font(Fonts.body)),
            Forms.formField("Font Size", Text("Medium").font(Fonts.body))
          ),
          "Notifications" -> Seq(
            Forms.formField(
              "Email Notifications",
              Text("Enabled").font(Fonts.body)
            ),
            Forms.formField("Push Notifications", Text("Disabled").font(Fonts.body))
          )
        )
      )
      .render()
      .asJComponent
  }

}
