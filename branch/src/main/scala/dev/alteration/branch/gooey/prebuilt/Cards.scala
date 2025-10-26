package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{Component, VStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{Alignment, Colors, Fonts}

/** Pre-built card components following Bootstrap-style conventions. */
object Cards {

  /** Creates a basic card with shadow and rounded corners.
    *
    * @param content
    *   The content to display inside the card
    * @param padding
    *   Padding around the content in pixels (default: 16)
    * @return
    *   A styled card component
    */
  def card(content: Component, padding: Int = 16): Component =
    content
      .padding(padding)
      .background(Colors.white)
      .cornerRadius(8)
      .shadow(offsetY = 2, blur = 8, opacity = 0.15f)

  /** Creates a card with a header section.
    *
    * @param header
    *   The header text
    * @param content
    *   The main content
    * @return
    *   A card with header and content sections
    */
  def cardWithHeader(header: String, content: Component): Component =
    VStack(
      spacing = 0,
      alignment = Alignment.Leading,
      Text(header)
        .font(Fonts.headline)
        .padding(16)
        .background(Colors.gray(0.95f)),
      content.padding(16)
    )
      .background(Colors.white)
      .cornerRadius(8)
      .shadow(offsetY = 2, blur = 8, opacity = 0.15f)

  /** Creates an elevated card that can respond to hover states.
    *
    * Note: Dynamic hover shadow changes require state management, which is
    * limited in this basic implementation. This provides the base elevated
    * card.
    *
    * @param content
    *   The content to display inside the card
    * @return
    *   An elevated card component
    */
  def elevatedCard(content: Component): Component =
    card(content).shadow(offsetY = 4, blur = 12, opacity = 0.2f)

}
