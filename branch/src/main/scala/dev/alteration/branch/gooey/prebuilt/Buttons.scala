package dev.alteration.branch.gooey.prebuilt

import dev.alteration.branch.gooey.layout.Component
import dev.alteration.branch.gooey.components.{Image, Text}
import dev.alteration.branch.gooey.modifiers.*
import dev.alteration.branch.gooey.styling.{bold, Colors, Fonts}

/** Button styles following common design system conventions. */
enum ButtonStyle {
  case Primary   // Filled, accent color
  case Secondary // Outlined
  case Tertiary  // Text only
  case Danger    // Red/destructive
  case Success   // Green
}

/** Pre-built button components with consistent styling. */
object Buttons {

  /** Creates a styled button with the specified style and action.
    *
    * @param text
    *   The button text
    * @param style
    *   The button style (default: Primary)
    * @param action
    *   The action to perform when clicked
    * @return
    *   A styled button component
    */
  def button(text: String, style: ButtonStyle = ButtonStyle.Primary)(
      action: => Unit
  ): Component = {
    val (bg, fg, borderColor) = style match {
      case ButtonStyle.Primary   =>
        (Colors.blue, Colors.white, Colors.blue)
      case ButtonStyle.Secondary =>
        (Colors.white, Colors.blue, Colors.blue)
      case ButtonStyle.Tertiary  =>
        (Colors.transparent, Colors.blue, Colors.transparent)
      case ButtonStyle.Danger    =>
        (Colors.red, Colors.white, Colors.red)
      case ButtonStyle.Success   =>
        (Colors.green, Colors.white, Colors.green)
    }

    Text(text)
      .font(Fonts.body.bold)
      .color(fg)
      .padding(horizontal = 24, vertical = 12)
      .background(bg)
      .border(borderColor, width = if (style == ButtonStyle.Tertiary) 0 else 2)
      .cornerRadius(6)
      .onClick(action)
  }

  /** Creates an icon button with tooltip.
    *
    * Icon buttons are compact, typically used for toolbar actions.
    *
    * @param iconPath
    *   Path to the icon image
    * @param size
    *   Size of the icon in pixels (default: 24)
    * @param tooltip
    *   Tooltip text (not yet implemented in basic version)
    * @param action
    *   The action to perform when clicked
    * @return
    *   An icon button component
    */
  def iconButton(iconPath: String, size: Int = 24, tooltip: String = "")(
      action: => Unit
  ): Component =
    Image(iconPath)
      .size(size, size)
      .padding(8)
      .background(Colors.transparent)
      .cornerRadius(4)
      .onClick(action)

}
