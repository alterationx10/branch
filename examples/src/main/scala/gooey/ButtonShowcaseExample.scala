package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.{VStack, HStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.prebuilt.{Buttons, ButtonStyle, Cards}
import dev.alteration.branch.gooey.styling.{Fonts, Alignment}
import dev.alteration.branch.gooey.modifiers.*

import java.awt.Component

/** Button showcase demonstrating different button styles. */
object ButtonShowcaseExample extends GooeyApp {

  override val appTitle: String = "Button Showcase"

  override def mainContent: Component = {
    VStack(
      spacing = 24,
      alignment = Alignment.leading,
      children = Seq(
        Text("Button Styles")
          .font(Fonts.largeTitle),
        Cards.card(
          VStack(
            spacing = 16,
            alignment = Alignment.leading,
            children = Seq(
              Text("Primary Actions").font(Fonts.headline),
              HStack(
                spacing = 12,
                children = Seq(
                  Buttons.button("Primary", ButtonStyle.Primary) {
                    println("Primary clicked")
                  },
                  Buttons.button("Secondary", ButtonStyle.Secondary) {
                    println("Secondary clicked")
                  },
                  Buttons.button("Tertiary", ButtonStyle.Tertiary) {
                    println("Tertiary clicked")
                  }
                )*
              )
            )*
          ),
          padding = 20
        ),
        Cards.card(
          VStack(
            spacing = 16,
            alignment = Alignment.leading,
            children = Seq(
              Text("Status Actions").font(Fonts.headline),
              HStack(
                spacing = 12,
                children = Seq(
                  Buttons.button("Success", ButtonStyle.Success) {
                    println("Success clicked")
                  },
                  Buttons.button("Danger", ButtonStyle.Danger) {
                    println("Danger clicked")
                  }
                )*
              )
            )*
          ),
          padding = 20
        ),
        Text("Click any button to see console output")
          .font(Fonts.caption)
      )*
    )
      .padding(24)
      .render()
      .asJComponent
  }

}
