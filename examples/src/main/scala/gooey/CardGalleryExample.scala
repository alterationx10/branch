package gooey

import dev.alteration.branch.gooey.GooeyApp
import dev.alteration.branch.gooey.layout.{VStack, Grid, Spacer}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.prebuilt.{Cards, Progress}
import dev.alteration.branch.gooey.styling.{Fonts, Alignment}
import dev.alteration.branch.gooey.modifiers.*

import java.awt.Component

/** Card gallery demonstrating various card styles and layouts. */
object CardGalleryExample extends GooeyApp {

  override val appTitle: String = "Card Gallery"

  override def mainContent: Component = {
    VStack(
      spacing = 24,
      alignment = Alignment.leading,
      children = Seq(
        Text("Card Styles")
          .font(Fonts.largeTitle),
        // Basic cards in a grid
        Grid(
          columns = 3,
          spacing = 16,
          children = Seq(
            Cards.card(
              VStack(
                spacing = 8,
                alignment = Alignment.center,
                children = Seq(
                  Text("Basic Card").font(Fonts.title),
                  Text("Simple content").font(Fonts.body),
                  Spacer().frame(width = 100, height = 20)
                )*
              ),
              padding = 20
            ),
            Cards.elevatedCard(
              VStack(
                spacing = 8,
                alignment = Alignment.center,
                children = Seq(
                  Text("Elevated Card").font(Fonts.title),
                  Text("With more shadow").font(Fonts.body),
                  Spacer().frame(width = 100, height = 20)
                )*
              )
            ),
            Cards.cardWithHeader(
              "Header Card",
              VStack(
                spacing = 8,
                children = Seq(
                  Text("Content below header").font(Fonts.body),
                  Spacer().frame(width = 100, height = 20)
                )*
              )
            )
          )*
        ),
        // Information cards
        Text("Information Cards")
          .font(Fonts.headline),
        Grid(
          columns = 2,
          spacing = 16,
          children = Seq(
            Cards.card(
              VStack(
                spacing = 12,
                alignment = Alignment.leading,
                children = Seq(
                  Text("Project Status").font(Fonts.headline),
                  Text("Completion: 65%").font(Fonts.caption),
                  Progress.progressBar(value = 0.65, width = 250)
                )*
              ),
              padding = 20
            ),
            Cards.card(
              VStack(
                spacing = 12,
                alignment = Alignment.leading,
                children = Seq(
                  Text("Team Metrics").font(Fonts.headline),
                  Text("Active Users: 42").font(Fonts.body),
                  Text("Tasks Completed: 127").font(Fonts.body)
                )*
              ),
              padding = 20
            )
          )*
        )
      )*
    )
      .padding(24)
      .render()
      .asJComponent
  }

}
