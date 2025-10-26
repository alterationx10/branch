package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.{VStack, Grid}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.prebuilt.Cards
import dev.alteration.branch.gooey.styling.{Fonts, Colors, Alignment, bold}
import dev.alteration.branch.gooey.modifiers.*

import java.awt.Component

/** Application launcher example from the planning document.
  *
  * This demonstrates the clean, declarative syntax for building a modern-looking
  * application launcher grid.
  */
object AppLauncherExample extends GooeyApp {

  override val appTitle: String = "Application Launcher"

  case class Application(name: String, icon: String)

  val applications = Seq(
    Application("Calculator", "🧮"),
    Application("Calendar", "📅"),
    Application("Mail", "📧"),
    Application("Browser", "🌐"),
    Application("Music", "🎵"),
    Application("Photos", "📷")
  )

  def launchApp(app: Application): Unit = {
    println(s"Launching ${app.name}...")
  }

  override def mainContent: Component = {
    VStack(
      spacing = 16,
      alignment = Alignment.leading,
      children = Seq(
        Text("Launcher").font(Fonts.headline),
        Grid(
          columns = 3,
          spacing = 16,
          children = applications.map { app =>
            Cards
              .card(
                VStack(
                  alignment = Alignment.center,
                  children = Seq(
                    Text(app.icon).font(Fonts.largeTitle),
                    Text(app.name).font(Fonts.body.bold)
                  )*
                ),
                padding = 32
              )
              .onClick(launchApp(app))
          }*
        ),
        Text("Click an icon to launch the program")
          .font(Fonts.caption)
          .color(Colors.gray(0.6f))
      )*
    )
      .padding(16)
      .render()
      .asJComponent
  }

}
