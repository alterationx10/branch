package dev.alteration.branch.gooey.components

import dev.alteration.branch.gooey.layout.{Component, RenderedComponent}
import dev.alteration.branch.gooey.styling.Fonts

import javax.swing.JLabel
import java.awt.{Color, Font}

/** Text display component.
  *
  * Displays a single line or multi-line text with optional styling.
  *
  * @param content
  *   The text content to display
  * @param font
  *   The font to use (default: body font)
  * @param color
  *   The text color (default: black)
  */
case class Text(
    content: String,
    font: Font = Fonts.default,
    color: Color = Color.BLACK
) extends Component {

  override def render(): RenderedComponent = {
    // Support basic HTML for multi-line text
    val htmlContent =
      if (content.contains("\n")) {
        val lines = content.split("\n")
        "<html>" + lines.mkString("<br>") + "</html>"
      } else {
        content
      }

    val label = new JLabel(htmlContent)
    label.setFont(font)
    label.setForeground(color)
    label.setOpaque(false)

    RenderedComponent(label)
  }

  /** Creates a new Text with the specified font.
    *
    * @param newFont
    *   The font to use
    */
  def font(newFont: Font): Text = copy(font = newFont)

  /** Creates a new Text with the specified color.
    *
    * @param newColor
    *   The text color
    */
  def color(newColor: Color): Text = copy(color = newColor)

}
