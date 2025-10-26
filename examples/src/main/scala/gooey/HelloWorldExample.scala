package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.VStack
import dev.alteration.branch.gooey.components.{Text, Button}
import dev.alteration.branch.gooey.styling.{Fonts, Colors, Alignment}
import dev.alteration.branch.gooey.modifiers.*

import java.awt.Component

/** Simple Hello World example demonstrating basic DSL usage. */
object HelloWorldExample extends GooeyApp {

  override val appTitle: String = "Hello World"

  override def mainContent: Component = {
    VStack(
      spacing = 20,
      alignment = Alignment.center,
      children = Seq(
        Text("Hello, Gooey!")
          .font(Fonts.largeTitle)
          .color(Colors.blue),
        Text("A modern Swing DSL for Scala 3")
          .font(Fonts.body)
          .color(Colors.gray(0.6f)),
        Button("Say Hello", () => println("Hello from Gooey!"))
          .padding(horizontal = 24, vertical = 12)
          .background(Colors.blue)
          .cornerRadius(8)
      )*
    )
      .padding(40)
      .render()
      .asJComponent
  }

}
