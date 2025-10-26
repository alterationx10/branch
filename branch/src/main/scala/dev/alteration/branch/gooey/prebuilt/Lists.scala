package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{
  Component,
  Grid,
  HStack,
  Spacer,
  VStack
}
import dev.alteration.branch.gooey.components.{Image, Text}
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{Alignment, Colors, Fonts}

/** Pre-built list and grid components. */
object Lists {

  /** Creates a list item with title, optional subtitle, and optional icon.
    *
    * @param title
    *   The item title
    * @param subtitle
    *   Optional subtitle text
    * @param iconPath
    *   Optional path to icon image
    * @return
    *   A styled list item component
    */
  def listItem(
      title: String,
      subtitle: Option[String] = None,
      iconPath: Option[String] = None
  ): Component = {
    val textContent = subtitle match {
      case Some(sub) =>
        VStack(
          spacing = 4,
          alignment = Alignment.leading,
          Text(title).font(Fonts.body),
          Text(sub).font(Fonts.caption).color(Colors.gray(0.6f))
        )
      case None      =>
        Text(title).font(Fonts.body)
    }

    val content = iconPath match {
      case Some(path) =>
        HStack(
          spacing = 12,
          alignment = Alignment.center,
          Image(path).size(40, 40),
          textContent,
          Spacer()
        )
      case None       =>
        HStack(
          spacing = 12,
          alignment = Alignment.center,
          textContent,
          Spacer()
        )
    }

    content
      .padding(12)
      .background(Colors.white)
  }

  /** Creates a grid gallery of components.
    *
    * @param items
    *   Sequence of components to display in grid
    * @param columns
    *   Number of columns (default: 3)
    * @return
    *   A grid layout with styled items
    */
  def gridGallery(items: Seq[Component], columns: Int = 3): Component =
    Grid(
      columns = columns,
      spacing = 16,
      items.map { item =>
        item
          .background(Colors.white)
          .cornerRadius(8)
          .shadow(offsetY = 2, blur = 8, opacity = 0.15f)
      }*
    )

}
