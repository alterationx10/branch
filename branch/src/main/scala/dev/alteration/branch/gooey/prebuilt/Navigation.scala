package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.{Component, HStack, Spacer, VStack}
import dev.alteration.branch.gooey.components.Text
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{withAlpha, Alignment, Colors, Fonts}

/** Pre-built navigation components. */
object Navigation {

  /** Creates a navigation bar (navbar) with title and action buttons.
    *
    * @param title
    *   The navbar title
    * @param actions
    *   Action components to display on the right
    * @return
    *   A styled navbar component
    */
  def navbar(title: String, actions: Component*): Component =
    HStack(
      spacing = 16,
      alignment = Alignment.center,
      (Seq(
        Text(title).font(Fonts.title).color(Colors.white),
        Spacer()
      ) ++ actions)*
    )
      .padding(horizontal = 16, vertical = 12)
      .background(Colors.blue)
      .shadow(offsetY = 2, blur = 4, opacity = 0.15f)

  /** Creates a sidebar navigation menu.
    *
    * @param items
    *   Sequence of (label, action) tuples
    * @param selected
    *   Index of the currently selected item (default: 0)
    * @return
    *   A sidebar navigation component
    */
  def sidebar(items: Seq[(String, () => Unit)], selected: Int = 0): Component =
    VStack(
      spacing = 4,
      alignment = Alignment.leading,
      items.zipWithIndex.map { case ((label, action), idx) =>
        sidebarItem(label, isSelected = idx == selected)(action)
      }*
    )
      .padding(8)
      .background(Colors.gray(0.95f))

  /** Creates a sidebar item (internal helper).
    *
    * @param label
    *   The item label
    * @param isSelected
    *   Whether this item is currently selected
    * @param action
    *   Action to perform when clicked
    * @return
    *   A sidebar item component
    */
  private def sidebarItem(label: String, isSelected: Boolean)(
      action: () => Unit
  ): Component =
    Text(label)
      .padding(horizontal = 16, vertical = 12)
      .background(
        if (isSelected) Colors.blue.withAlpha(0.1f) else Colors.transparent
      )
      .cornerRadius(6)
      .onClick(action())

}
