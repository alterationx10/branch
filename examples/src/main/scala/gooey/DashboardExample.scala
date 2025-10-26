package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.{VStack, HStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.templates.Templates
import dev.alteration.branch.gooey.prebuilt.Progress
import dev.alteration.branch.gooey.styling.{Fonts, Alignment}

import java.awt.Component

/** Dashboard example showing widget-based layout. */
object DashboardExample extends GooeyApp {

  override val appTitle: String = "Dashboard Example"

  override def mainContent: Component = {
    Templates
      .dashboard(
        Seq(
          "System Status" -> VStack(
            spacing = 16,
            children = Seq(
              HStack(
                spacing = 16,
                children = Seq(
                  Text("CPU Usage:"),
                  Progress.progressBar(value = 0.65, width = 200)
                )*
              ),
              HStack(
                spacing = 16,
                children = Seq(
                  Text("Memory:"),
                  Progress.progressBar(value = 0.42, width = 200)
                )*
              ),
              HStack(
                spacing = 16,
                children = Seq(
                  Text("Disk:"),
                  Progress.progressBar(value = 0.78, width = 200)
                )*
              )
            )*
          ),
          "Recent Activity" -> VStack(
            spacing = 8,
            alignment = Alignment.leading,
            children = Seq(
              Text("User logged in").font(Fonts.caption),
              Text("Database backup completed").font(Fonts.caption),
              Text("New update available").font(Fonts.caption)
            )*
          ),
          "Quick Actions" -> VStack(
            spacing = 8,
            children = Seq(
              Text("Settings"),
              Text("Reports"),
              Text("Help")
            )*
          )
        )
      )
      .render()
      .asJComponent
  }

}
